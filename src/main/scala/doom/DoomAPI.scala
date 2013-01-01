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
package doom

import doom.server.ServerHealthCache
import ninthdrug.command._

object DoomAPI {
  def getUser(userid: String) = ConfigCache.getUser(userid)
  def getGroupsForUser(userid : String) : List[String] = {
    ConfigCache.getGroupsForUser(userid)
  }
  def users = ConfigCache.users
  def machines = ConfigCache.machines
  def domains = ConfigCache.domains
  def servers = ConfigCache.servers
  def getMachine(name: String) = ConfigCache.getMachine(name)
  def getDomain(name: String) = ConfigCache.getDomain(name)
  def getAdminServer(domainname: String) = getDomain(domainname).adminserver
  def getServer(name: String) = ConfigCache.getServer(name)
  def hasMachine(name: String) = ConfigCache.hasMachine(name)
  def hasDomain(name: String) = ConfigCache.hasDomain(name)
  def hasServer(name: String) = ConfigCache.hasServer(name)
  def getServerHealthCacheAsJson = ServerHealthCache.json

  def remote(command: Command, host: String): Command = {
    if (
      host == "localhost" ||
      host == "localhost.localdomain" ||
      host == doom.HOST ||
      host == doom.HOSTNAME
    ) {
      command
    } else {
      ConfigCache.findMachine(host) match {
        case None =>
          throw new UnknownHostException(host)
        case Some(machine) =>
          new Lazarus(command, machine.address, machine.user)
      }
    }
  }
}
