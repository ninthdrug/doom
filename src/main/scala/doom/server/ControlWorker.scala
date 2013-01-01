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

import doom._
import scala.actors.Actor
import scala.actors.Actor._
import ninthdrug.logging.Logger

class ControlWorker(id: Int) extends Actor {
  private val log = Logger("doom.server.ControlWorker")

  def act() {
    loop {
      react {
        case order: WorkOrder => {
          val result = control(order)
          log.debug("control returns " + result) 
          var orderstate = if (!result) {
            "ERROR" 
          } else {
            "FINISHED"
          }        
          ControlManager ! FinishedOrder(id, order.orderid, orderstate)
        }
      }
    }
  }  

  def control(order: WorkOrder): Result = {
    val server = ConfigCache.getServer(order.servername)
    val domain = ConfigCache.getDomain(server.domainname)
    val command = server.servertype match {
      case "admin" => {
        order.command match {
          case "kill" => KillAdminServer(domain)
          case "start" => StartAdminServer(domain)
          case "stop" => StopAdminServer(domain)
          case "restart" => RestartAdminServer(domain)
          case _ => Return(Result(1, "Unknown command: " + order.command))
        }
      }
      case "loadbalancer" => {
        order.command match {
          case "kill" => KillLoadBalancer(domain)
          case "start" => StartLoadBalancer(domain)
          case "stop" => StopLoadBalancer(domain)
          case "restart" => RestartLoadBalancer(domain)
          case _ => Return(Result(1, "Unknown command: " + order.command))
        }
      }
      case "managed" => {
        val user = "weblogic"
        val password = CredentialCache.getPassword(
          "weblogic",
          domain.name,
          user
        )
        order.command match {
          case "kill" => KillManagedServer(server)
          case "start" => StartManagedServer(domain, server, user, password)
          case "stop" => StopManagedServer(domain, server, user, password)
          case "restart" => RestartManagedServer(domain, server, user, password)
          case _ => Return(Result(1, "Unknown command: " + order.command))
        }
      }
      case "nodemanager" => {
        val host = server.address
        order.command match {
          case "kill" => KillNodeManager(domain, host)
          case "start" => StartNodeManager(domain, host)
          case "stop" => StopNodeManager(domain, host)
          case "restart" => RestartNodeManager(domain, host)
          case _ => Return(Result(1, "Unknown command: " + order.command))
        }
      }
      case _ => Return(Result(1, "Unknown servertype: " + server.servertype))
    }
    command.result
  }
}
