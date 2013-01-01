/*
 * Copyright 2008-2011 Trung Dinh
 *
 *  This file is part of Doom.
 *
 *  Doom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Doom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Doom.  If not, see <http://www.gnu.org/licenses/>.
 */
package doom.server
import doom.ConfigCache
import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable.ListBuffer
import ninthdrug.logging.Logger

case object Dispatch

object ControlManager extends Actor {
  private val log = Logger("doom.server.ControlManager")
  private val maxWorkers = 24
  private val workers = new Array[ControlWorker](maxWorkers)
  private val idlers =  new ListBuffer[Int]
  private var orders = new ListBuffer[WorkOrder]
 
  def act() {
    log.info("started")
    for (i <- 0 until maxWorkers) {
      workers(i) = new ControlWorker(i)
      workers(i).start
      idlers += i
    }
    while (true) {
      receive {
        case request: ControlRequest =>
          schedule(request)
        case FinishedOrder(workerid, orderid, orderstate) =>
          finish(workerid, orderid, orderstate)
        case Dispatch =>
          dispatch()
      }
    }
  }

  private def schedule(request: ControlRequest) {
    val order = WorkOrder(
      request.user,
      request.command,
      request.servername,
      new java.sql.Timestamp(System.currentTimeMillis()),
      null,
      "PENDING"
    )
    orders.append(order)
    // We use WorkOrder(order) to create a copy of the original order
    LoggerManager ! new LoggerRequest(WorkOrder(order))
    ControlManager ! Dispatch
  }

  private def finish(workerid: Int, orderid: Int, orderstate: String) {
    log.debug("FinishedOrder(" + workerid + ", " + orderid + ", " + orderstate + ")")
    val now = new java.sql.Timestamp(System.currentTimeMillis)
    idlers += workerid
    orders.find(_.orderid == orderid) match {
      case Some(finishedOrder) =>
        finishedOrder.status = orderstate
        finishedOrder.endDate = now
        LoggerManager ! new LoggerRequest(WorkOrder(finishedOrder))
        for (
          order <- orders.filter(
            o => o.orderid != orderid &&
            o.status == "WORKING" &&
            o.servername == finishedOrder.servername
          )
        ) {
          if (order.command == finishedOrder.command) {
            order.status = finishedOrder.status
          } else {
            order.status = "ERROR"
          }
          order.endDate = now
          LoggerManager ! new LoggerRequest(WorkOrder(order))
        }
        ConfigCache.servers.find(server =>
          server.name == finishedOrder.servername &&
          server.servertype == "nodemanager"
        ) match {
          case Some(nm) =>
            for (
              order <- orders.filter(
                o => o.orderid != orderid &&
                o.status == "WORKING"
              )
            ) {
              ConfigCache.servers.find(s =>
                s.name == order.servername &&
                s.servertype == "nodemanager" &&
                s.address == nm.address
              ) match {
                case Some(nm2) =>
                  if (order.command == finishedOrder.command) {
                    order.status = finishedOrder.status
                  } else {
                    order.status = "ERROR"
                  }
                  order.endDate = now
                  LoggerManager ! new LoggerRequest(WorkOrder(order))
                case None =>
              }
            }
          case None =>
        }
      case None =>
    }
    ControlManager ! Dispatch
  }

  private def dispatch() {
    var dispatchAgain = false
    for (order <- orders.filter(_.status == "READY")) {
      if (!idlers.isEmpty) {
        order.status = "WORKING"
        val worker = workers(idlers.remove(0))
        worker ! WorkOrder(order)
        LoggerManager ! new LoggerRequest(WorkOrder(order))
      }
    }

    for (order <- orders.filter(_.status == "PENDING")) {
      val server = ConfigCache.getServer(order.servername)
      if (server.servertype == "managed") {
        dispatchAgain |= scheduleManagedServerOrder(order)
      } else {
        dispatchAgain |= scheduleNonManagedServerOrder(order)
      }
    }
    if (dispatchAgain) {
      ControlManager ! Dispatch
    }  
  }

  private def scheduleManagedServerOrder(workorder: WorkOrder): Boolean = {
    var dispatchAgain = false

    if (workorder.command == "kill") {
      workorder.status = "READY"
      LoggerManager ! new LoggerRequest(WorkOrder(workorder))
      dispatchAgain = true
    } else {
      val servername = workorder.servername
      val server = ConfigCache.getServer(workorder.servername)
      val domain = ConfigCache.getDomain(server.domainname)
      val adminservername = domain.adminserver.name
      val nodemanagername = 
        ConfigCache.findNodeManager(server.machinename) match {
          case Some(nodemanager) => nodemanager.name
          case None => servername
        }
      orders.find {
        order => (
          order != workorder &&
          (order.status == "WORKING" || order.status == "READY") &&
          (
            order.servername == servername || 
            order.servername == adminservername ||
            order.servername == nodemanagername
          )
        )
      } match {
        case Some(order) => 
        case None => {
          val adminserverhealth = ServerHealthCache.getHealth(adminservername)
          if (adminserverhealth == "DOWN") {
            val order = WorkOrder(
              workorder.userid,
              "start",
              adminservername,
              new java.sql.Timestamp(System.currentTimeMillis()),
              null,
              "READY"
            )
            orders.append(order)
            LoggerManager ! new LoggerRequest(WorkOrder(order))
          } else {
            workorder.status = "READY"
            LoggerManager ! new LoggerRequest(WorkOrder(workorder))
          }
          dispatchAgain = true
        }
      }
    }
    dispatchAgain
  }

  private def scheduleNonManagedServerOrder(workorder: WorkOrder): Boolean = {
    if (workorder.command == "kill") {
      workorder.status = "READY"
      LoggerManager ! new LoggerRequest(WorkOrder(workorder))
    } else {
      val servername = workorder.servername
      orders.find {
        order => (
          order != workorder &&
          (order.status == "WORKING" || order.status == "READY") &&
          order.servername == servername
        )
      } match {
        case Some(order) => 
        case None =>
          workorder.status = "READY"
          LoggerManager ! new LoggerRequest(WorkOrder(workorder))
      }
    }
    workorder.status == "READY"
  }
}
