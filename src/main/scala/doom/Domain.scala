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

case class Domain(
  name: String,
  domaintype: String,
  domaingroup: String = "",
  project: String = "",
  env: String = "",
  java_vendor: String = "Sun",
  java_home: String = "/opt/oracle/jdk160",
  mw_home: String = "/opt/oracle/11g",
  wl_home: String = "/opt/oracle/11g/wlserver_10.3",
  wl_version: String = "10.3.5.0",
  osb_home: String = "/opt/oracle/11g/osb",
  osb_version: String = "11.1.1.5.0",
  alsb_home: String = "",
  alsb_version: String = "",
  servers: List[Server] = List[Server](),
  clusters: List[Cluster] = List[Cluster](),
  loadbalancers: List[Server] = List[Server]()
) {
  val domains_dir = Config.get("doom.weblogic.domains_dir")
  val share_dir = Config.get("doom.weblogic.share_dir")
  val domain_home = domains_dir + "/" + name
  val nodemanager_home = domains_dir + "/nodemanager"
  lazy val adminserver = servers.find(s => s.servertype == "admin") match {
    case Some(server) => server
    case None => throw new ConfigException("Domain without adminserver: " + name)
  }
  lazy val managedservers = 
    servers.filter(_.servertype == "managed") ++
    (clusters flatMap (c => c.servers))
  lazy val machinenames = (managedservers map { s => s.machinename}).distinct

  def version = domaintype match {
    case "alsb" => alsb_version
    case "osb" => osb_version
    case _ => wl_version
  }

  def json() = {
    "{ " +
    quote("name") + " : " + quote(name) + ", " +
    quote("domaintype") + " : " + quote(domaintype) + ", " +
    quote("domain_home") + " : " + quote(domain_home) + ", " +
    quote("java_home") + " : " + quote(java_home) + ", " +
    quote("java_vendor") + " : " + quote(java_vendor) + ", " +
    quote("bea_home") + " : " + quote(mw_home) + ", " +
    quote("wl_version") + " : " + quote(wl_version) + ", " +
    quote("wl_home") + " : " + quote(wl_home) + ", " +
    quote("osb_home") + " : " + quote(osb_home) + ", " +
    quote("alsb_home") + " : " + quote(alsb_home) + ", " +
    quote("env") + " : " + quote(env) + ", " +
    quote("group") + " : " + quote(domaingroup) + ", " +
    quote("project") + " : " + quote(project) +
    " }"
  }
}
