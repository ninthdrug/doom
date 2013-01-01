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
import java.io._
import java.net._

class PollLoadBalancers extends Runnable {
  var servers: List[Server] = null

  def run() {
    try {
      servers = ConfigCache.servers.filter(_.servertype == "loadbalancer")
      while (true) {
        val t0 = System.currentTimeMillis()
        for (server <- servers) {
          updateServerHealth(server)
        }
        val t1 = System.currentTimeMillis()
        val t = t1 - t0
        val delay : Long = if (t < 900L) 1000L - t else 100L
        Thread.sleep(delay)
      }
    } catch {
      case e : Exception => {
      }
    }
  }

  private def updateServerHealth(server : Server) {
    var health = "UNKNOWN"
    var socket : Socket = null
    try {
      socket = new Socket(server.address, server.port.toInt)
      health = "OK"
    } catch {
      case ioe : IOException => {
        health = "DOWN"
      }
    } finally {
      if (socket != null) {
        try {
          socket.close()
        } catch {
          case e : Exception =>
        }
      }
    }
    val t = System.currentTimeMillis()
    ServerHealthCache ! ServerHealthUpdate(server.name, health, t)
  }
}
