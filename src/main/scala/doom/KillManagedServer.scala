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
 * Command to kill a managed server.
 */
class KillManagedServer(server: Server) extends Command {

  def result(): Result = {
    try {
      val pids = psgrep("weblogic.Name=" + server.name)
      pids foreach {
        pid => kill(pid)
      }

      if (pids.isEmpty) {
        Result(server.name + " does not appear to be running")
      } else {
        Result("killed  " + server.name)
      }
    } catch {
      case e: Exception => Result("ERROR: killing " + server.name, e)
    }
  }

  override def toString() = "KillManagedServer(" + server.name + ")"
}

object KillManagedServer {
  def apply(server: Server): Command = {
    DoomAPI.remote(new KillManagedServer(server), server.address)
  }
}
