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

import java.io._
import ninthdrug.command._
import scala.util.matching._

/**
 * Command to kill the admin server for a domain.
 */
class StopAdminServer(domain: Domain) extends Command {

  def result(): Result = {
    try {
      val domain_home = domain.domain_home
      val name = domain.adminserver.name
      val pids = psgrep("weblogic.Name=" + name)

      if (!pids.isEmpty) {
        exec(domain_home + "/bin/stopWebLogic.sh")
      }

      if (pids.isEmpty) {
        Result(name + " does not appear to be running")
      } else {
        Result("stopped  " + name)
      }
    } catch {
      case e: Exception => Result(e)
    }
  }

  override def toString() = "StopAdminServer(" + domain.name + ")"
}

object StopAdminServer {
  def apply(domain: Domain): Command = {
    val host = domain.adminserver.address
    DoomAPI.remote(new StopAdminServer(domain), host)
  }
}
