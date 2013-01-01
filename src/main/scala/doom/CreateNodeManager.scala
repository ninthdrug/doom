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
import ninthdrug.edit._
import scala.util.matching._

case class CreateNodeManager(domain: Domain, machinename: String) extends Command {

  def result(): Result = {
    val nodemanager_home = domain.nodemanager_home
    val nm_props = """DomainsFile=/data/domains/nodemanager/nodemanager.domains
AuthenticationEnabled=true
CrashRecoveryEnabled=false
DomainsFileEnabled=true
JavaHome=
ListenAddress=
ListenBacklog=50
ListenPort=5556
LogAppend=true
LogCount=1
LogFile=/data/domains/nodemanager/nodemanager.log
LogFormatter=weblogic.nodemanager.server.LogFormatter
LogLevel=INFO
LogLimit=0
LogToStderr=true
NativeVersionEnabled=false
NodeManagerHome=/data/domains/nodemanager
QuitEnabled=true
SecureListener=false
StartScriptEnabled=false
StartScriptName=startWebLogic.sh
StateCheckInterval=500
StopScriptEnabled=false
"""

    try {
      val outfile = nodemanager_home + "/nohup.out"
      val alsb_home = if (domain.alsb_home == "") {
        "/opt/oracle/11g/osb"
      } else {
        domain.alsb_home
      }
      Chain(
        Exec("mkdir -p " + nodemanager_home),
        Write(nodemanager_home + "/nodemanager.properties", nm_props),
        Exec("touch " + nodemanager_home + "/nodemanager.domains"),
        EditFile(
          nodemanager_home + "/nodemanager.domains",
          AddProperty(domain.name, domain.domain_home)
        ),
        Exec("cp " + domain.wl_home + "/server/bin/startNodeManager.sh " +
             nodemanager_home),
        EditFile(
          nodemanager_home + "/startNodeManager.sh",
          ChangeProperty("NODEMGR_HOME", nodemanager_home),
          InsertBefore(
            """^NODEMGR_HOME=.*""".r, 
            """
ALSB_HOME=""" + alsb_home + """
export ALSB_HOME

JAVA_OPTIONS="${JAVA_OPTIONS} -Dcom.sun.management.jmxremote.port=3000 -Dcom.sun.management.jmxremote.authenticate=true -Dcom.sun.management.jmxremote.ssl=false"
"""
          ),
          Replace(
            "weblogic.NodeManager -v",
            "weblogic.NodeManager -v >" + outfile + " 2>&1 &"
          ),
          Replace("""umask .*""".r, "umask 022")
        ),
        Exec("chmod 0755 " + nodemanager_home + "/startNodeManager.sh"),
        EditFile(
          nodemanager_home + "/nodemanager.properties",
          ChangeProperty("JavaHome", domain.java_home),
          ChangeProperty("NodeManagerHome", nodemanager_home),
          ChangeProperty("ListenAddress", machinename)
        )
      ).result
    } catch {
      case e: Exception => Result(e)
    }
  }
}
