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
import javax.management.JMException
import javax.management.MBeanException
import javax.management.MBeanServerConnection
import javax.management.ObjectName
import javax.management.openmbean.CompositeData
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL
import scala.collection.mutable
import ninthdrug.logging.Logger

class PollNodeManagers extends Runnable {
  private val log = Logger("doom.server.PollNodeManagers")
  var servers = List[Server]()
  val connectorMap = mutable.Map[String, JMXConnector]()

  def run() {
    try {
      init()
      while (true) {
        val t0 = System.currentTimeMillis()
        for (server <- servers) {
          updateServerState(server)
        }
        val t1 = System.currentTimeMillis()
        val t = t1 - t0
        val delay : Long = if (t < 900L) 1000L - t else 100L
        Thread.sleep(delay)
      }
    } catch {
      case e : Exception => {
        cleanup()
      }
    }
  }

  def init() {
    servers = ConfigCache.servers.filter {
      s => s.jmxport != 0 && s.servertype == "nodemanager"
    }
    for (server <- servers) {
      connectorMap.put(server.name, null)
    }
  }

  private def cleanup() {
    for (server <- servers) {
      try {
        val connector = connectorMap(server.name)
        if (connector != null) {
          connector.close()
        }
      } catch {
        case e : Exception => {
        }
      }
    }
  }

  private def updateServerState(server : Server) {
    var state = "DOWN"
    var connector = connectorMap(server.name)
    if (connector == null) {
      connector = connect(server)
      connectorMap.update(server.name, connector)
    }
    if (connector != null) {
      state = getState(connector)
      if (state == "DOWN") {
        connectorMap.update(server.name, null)
        try {
          connector.close()
        } catch {
          case e : Exception => {}
        }
      }
    }
    val t = System.currentTimeMillis()
    ServerHealthCache ! ServerHealthUpdate(server.name, state, t)
  }

  private def connect(server : Server) : JMXConnector = {
    var connector : JMXConnector = null
    val host = server.address
    val port = server.jmxport
    if (host == null || port == 0) return null
    val domain = ConfigCache.getDomain(server.domainname)
    val env = domain.env
    val map = new java.util.HashMap[String,Object]()
    CredentialCache.findCredential("jmx", env, "doom") match {
      case Some(credential) =>
        val creds = Array("doom", credential.password)
        map.put("jmx.remote.credentials", creds)
      case None =>
    }
    val serviceURL = new JMXServiceURL(
      "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi")
    try {
      connector = JMXConnectorFactory.newJMXConnector(serviceURL, map)
      connector.connect()
    } catch {
      case e : Exception => {
        log.error("CANNOT CONNECT TO SERVER " + server.name)
        if (connector != null) {
          try {
            connector.close()
          } catch {
            case e: Exception => {}
          }
          connector = null;
        }
      }
    }
    return connector
  }

  private def getState(connector : JMXConnector) : String = {
    var state = "DOWN"
    try {
      val connection = connector.getMBeanServerConnection()
      val mem = new ObjectName("java.lang:type=Memory")
      val heap = connection.getAttribute(mem, "HeapMemoryUsage").asInstanceOf[CompositeData]
      state = "OK"
    } catch {
      case e : Exception => {
        state = "DOWN"
      }
    }
    return state
  }
}
