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
import doom.Ssh
import ninthdrug.config.Config

class PollBlockedServerHealth extends Runnable {
  private val doom_home = Config.get("doom.home")

  def run() {
    while(true) {
      pollhealth()
      Thread.sleep(1000L)
    }
  }

  def pollhealth() {
    val blockedServers = ConfigCache.servers.filter(_.blocked)
    for (server <- blockedServers) {
      val machine = ConfigCache.getMachine(server.machinename)
      val host = machine.name
      val user = machine.user
      val healthfile = doom_home + "/hosts/" + host + "/health"
      val cmd = Ssh("cat " + healthfile, host, user)
      val res = cmd.result()
      if (!res) {
        val time = System.currentTimeMillis
        val update = ServerHealthUpdate(server.name, "UNKNOWN", time)
        ServerHealthCache ! update
      } else {
        val lines = res.output.split('\n')
        for (line <- lines) {
          val columns = line.split(':')
          if (columns.length > 4) {
            val servername = columns(1)
            val health = columns(4)
            val time = columns(0).toLong
            val update = ServerHealthUpdate(servername, health, time)
            ServerHealthCache ! update
          }
        }
      }
    }
  }
}
