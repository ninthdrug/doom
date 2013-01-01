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
import scala.util.matching._

class StartNodeManager(domain: Domain) extends Command {

  def result(): Result = {
    val host = fullhostname
    try {
      val nodemanager_home = domain.nodemanager_home
      val scriptfile = nodemanager_home + "/startNodeManager.sh"
      val outfile = nodemanager_home + "/nohup.out"

      if (psgrep("weblogic.NodeManager").length > 0) {
        return Result("nodemanager already running on " + host)
      }

      exec(scriptfile)

      for (n <- 0 to 900) {
        Thread.sleep(1000L)
        val out = read(outfile)
        if (out.contains("socket listener started")) {
          return Result("started nodemanager on " + host)
        }
        if (out.contains("<SEVERE> <Fatal error in node manager server>")) {
          return Result(1, "ERROR starting nodemanager on " + host)
        }
      }
      Result(1, "ERROR: timeout starting nodemanger on " + host)
    } catch {
      case e: Exception => Result("ERROR starting nodemanger on " + host, e)
    }
  }
}

object StartNodeManager {
  def apply(domain: Domain, host: String = "localhost"): Command = {
    DoomAPI.remote(new StartNodeManager(domain), host)
  }
}
