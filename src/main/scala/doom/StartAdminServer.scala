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

class StartAdminServer(domain: Domain) extends Command {

  def result(): Result = {
    try {
      val domain_home = domain.domain_home
      if (psgrep("-Dweblogic.Name=" + domain.name).length > 0) {
        return Result(domain.adminserver.name + " already running")
      }

      val outfile = domain_home + "/nohup.out"
      val scriptfile = domain_home + "/startAdminServer.sh"
      if (!(new File(scriptfile).canExecute)) {
        val jmxport = domain.adminserver.jmxport
        val text = "#!/bin/bash\n" +
          "export USER_MEM_ARGS=" +
          "\"" + domain.adminserver.memargs + "\"\n" +
          "export JAVA_OPTIONS=\"" +
          "-Dcom.sun.management.jmxremote.port=" + jmxport +
          " -Dcom.sun.management.jmxremote.ssl=false" +
          " -Dcom.sun.management.jmxremote.authenticate=true" +
          " -Djavax.management.builder.initial=weblogic.management.jmx.mbeanserver.WLSMBeanServerBuilder" +
          "\"\n" +
          domain_home + "/bin/startWebLogic.sh >" + outfile + " 2>&1 &\n"
        write(scriptfile, text)
        exec("chmod 0755 " + scriptfile)
      }

      exec(scriptfile)

      for (n <- 0 to 900) {
        Thread.sleep(1000L)
        val out = read(outfile)
        if (out.contains("Server state changed to RUNNING")) {
          return Result("started " + domain.adminserver.name)
        }
        if (out.contains("Server state changed to FAILED")) {
          return Result(
            1,
            "ERROR: starting admin server " + domain.adminserver.name
          )
        }
      }
      Result(1, "ERROR: timeout starting " + domain.adminserver.name)
    } catch {
      case e: Exception => Result(e)
    }
  }

  override def toString() = "StartAdminServer(" + domain.name + ")"
}

object StartAdminServer {
  def apply(domain: Domain): Command = {
    val host = domain.adminserver.machinename
    DoomAPI.remote(new StartAdminServer(domain), host)
  }
}
