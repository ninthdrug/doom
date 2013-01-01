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
import ninthdrug._
import ninthdrug.command._

/**
 * Command to kill the nodemanager.
 */
class KillNodeManager(domain: Domain) extends Command {

  def result(): Result = {
    val hostname = fullhostname
    try {
      val pids = psgrep(" weblogic.NodeManager ")
      pids foreach {
        pid => kill(pid)
      }

      if (pids.isEmpty) {
        Result("nodemanager does not appear to be running on " + hostname)
      } else {
        Result("killed nodemanager on " + hostname)
      }
    } catch {
      case e: Exception => Result("ERROR: killing nodemanager on " + hostname, e)
    }
  }
}

object KillNodeManager {
  def apply(domain: Domain, host: String = "localhost"): Command = {
    DoomAPI.remote(new KillNodeManager(domain), host)
  }
}
