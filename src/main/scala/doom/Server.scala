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
import ninthdrug.util.Json.quote

/**
 * A Weblogic server.
 */
case class Server(
  name: String,
  servertype: String = "managed",
  domainname: String = "",
  clustername: String = "",
  machinename: String = "localhost",
  address: String = "localhost",
  port: Int = 7001,
  jmxport: Int = 1500,
  blocked: Boolean = false
) {
  /**
   * Return true if server type is admin or managed.
   */
  def isAdminOrManaged = servertype == "admin" || servertype == "managed"

  /**
   * Return true if server type is managed.
   */
  def isManaged = servertype == "managed"

  /**
   * Return true if server type is admin.
   */
  def isAdmin = servertype == "admin"

  def memargs = "-Xms1024m -Xmx1024m -Xss256k -XX:PermSize=384m"

  def jmxargs = 
    "-Dcom.sun.management.jmxremote.port=" + jmxport +
    " -Dcom.sun.management.jmxremote.ssl=false" +
    " -Dcom.sun.management.jmxremote.authenticate=true" +
    " -Djavax.management.builder.initial=weblogic.management.jmx.mbeanserver.WLSMBeanServerBuilder"

  def startargs = memargs + " " + jmxargs

  def json() = {
    "{ " +
    quote("servername") + " : " + quote(name) + ", " +
    quote("servertype") + " : " + quote(servertype) + ", " +
    quote("domainname") + " : " + quote(domainname) + ", " +
    quote("clustername") + " : " + quote(clustername) + ", " +
    quote("machinename") + " : " + quote(machinename) + ", " +
    quote("address") + " : " + quote(address) + ", " +
    quote("port") + " : " + port + ", " +
    quote("jmxport") + " : " + jmxport + ", " +
    quote("blocked") + " : " + blocked +
    " }"
  }
}
