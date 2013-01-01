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

case class EnrollNodeManager(
  domain: Domain,
  machinename: String,
  user: String,
  password: String
) extends Command {

  def result(): Result = {
    try {
      val adminserver = domain.adminserver
      val wlst = domain.wl_home + "/common/bin/wlst.sh"
      val scriptfile = File.createTempFile("wlst", ".py")
      scriptfile.deleteOnExit
      val scriptname = scriptfile.getAbsolutePath
      val url = "t3://" + adminserver.address + ":" + adminserver.port
      val script =
        "connect('" + user + "', '" + password + "', '" + url + "')\n" +
        "nmEnroll('" + domain.domain_home + "', '" + domain.nodemanager_home + "')\n" +
        "disconnect()\n"
      Command.write(scriptname, script)
      Sh(wlst + " " + scriptname).result
    } catch {
      case e: Exception => Result(e)
    }
  }
}
