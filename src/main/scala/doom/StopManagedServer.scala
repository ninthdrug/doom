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

import ninthdrug.command._

class StopManagedServer(
  domain: Domain,
  server: Server,
  user: String,
  password: String
) extends Command {

  def result(): Result = {
    try {
      val adminserver = domain.adminserver
      val jmx = WeblogicJMX(
        adminserver.address,
        adminserver.port,
        user,
        password
      )
      jmx.stopManagedServer(server.name)
      jmx.close()
      Result("stopped " + server.name)
    } catch {
      case e: Exception => Result("Error stopping " + server.name, e)
    }
  }

  override def toString() = "StopManagedServer(" + server.name + ")"
}

object StopManagedServer {
  def apply(
    domain: Domain,
    server: Server,
    user: String = "weblogic",
    password: String = ""
  ): Command = {
    val passwd = if (password == null || password == "") {
      CredentialCache.getPassword("weblogic", domain.name, user)
    } else {
      password
    }
    new StopManagedServer(domain, server, user, passwd)
  }
}
