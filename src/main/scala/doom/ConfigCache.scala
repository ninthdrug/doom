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

import java.sql._
import scala.collection.mutable.ListBuffer
import ninthdrug.sql._
import ninthdrug.util.Json._
import ninthdrug.logging.Logger

object ConfigCache {
  private val log = Logger("doom.ConfigCache")
  private var db: Database = null

  private var _machines = List[Machine]()
  private var _servers = List[Server]()
  private var _clusters = List[Cluster]()
  private var _domains = List[Domain]()
  private var _allusers = List[User]()
  private var _users = List[User]()
  private var _groups = List[Group]()
  private var _groupmembers = List[Pair[String,String]]()
  private var _permissions = List[Permission]()

  private var _machineMap = Map[String, Machine]()
  private var _serverMap = Map[String, Server]()
  private var _clusterMap = Map[String, Cluster]()
  private var _domainMap = Map[String, Domain]()
  private var _userMap = Map[String, User]()
  private var _groupMap = Map[String, Group]()

  private var _json = ""

  load()

  def load() {
    val dburl = Config.get("doom.dburl")
    val dbuser = Config.get("doom.dbuser")
    val dbpass = Config.get("doom.dbpassword")
    db = Database(dburl, dbuser, dbpass)

    _machines = {
      db.list[Machine]("select * from machines order by machinename") {
        (rs: ResultSet) => Machine(
          rs.getString("machinename"),
          rs.getString("address"),
          rs.getString("os_user")
        )
      }
    }
    _machineMap = Map() ++ (_machines.map(machine => (machine.name, machine)))

    _servers = {
      db.list[Server]("select * from servers order by servername") {
        (rs: ResultSet) => Server(
          name = rs.getString("servername"),
          servertype = rs.getString("servertype"),
          domainname = rs.getString("domainname"),
          clustername = rs.getString("clustername"),
          machinename = rs.getString("machinename"),
          address = rs.getString("address"),
          port = rs.getInt("port"),
          jmxport = rs.getInt("jmxport"),
          blocked = rs.getBoolean("blocked")
        )
      }
    }
    _serverMap = Map() ++ (_servers.map(server => (server.name, server)))

    _clusters = {
      db.list[Cluster](
        "select distinct clustername, domainname from servers where clustername <> ''" +
        " order by clustername"
      ) {
        (rs: ResultSet) => {
          val clustername = rs.getString("clustername")
          Cluster(
            name = clustername,
            domainname = rs.getString("domainname"),
            servers = _servers.filter(_.clustername == clustername)
          )
        }
      }
    }
    _clusterMap = Map() ++ (_clusters.map(cluster => (cluster.name, cluster)))

    _domains = {
      db.list[Domain]("select * from domains order by domainname") {
        (rs: ResultSet) => {
          val domainname = rs.getString("domainname")
          val myclusters = _clusters.filter(_.domainname == domainname)
          val myservers = _servers.filter(s =>
            s.domainname == domainname && s.clustername == ""
          )
          Domain(
            name = domainname,
            domaintype = rs.getString("domaintype"),
            domaingroup = rs.getString("domaingroup"),
            project = rs.getString("project"),
            env = rs.getString("env"),
            java_vendor = rs.getString("java_vendor"),
            java_home = rs.getString("java_home"),
            mw_home = rs.getString("bea_home"),
            wl_home = rs.getString("wl_home"),
            wl_version = rs.getString("wl_version"),
            osb_home = rs.getString("osb_home"),
            osb_version = rs.getString("osb_version"),
            alsb_home = rs.getString("alsb_home"),
            alsb_version = rs.getString("alsb_version"),
            servers = _servers.filter(s =>
              s.domainname == domainname &&
              s.clustername == "" &&
              (s.servertype == "managed" || s.servertype == "admin")
            ),
            clusters = _clusters.filter( _.domainname == domainname),
            loadbalancers = _servers.filter(s =>
              s.domainname == domainname &&
              s.servertype == "loadbalancer"
            )
          )
        }
      }
    }
    _domainMap = Map() ++ (_domains.map(domain => (domain.name, domain)))

    _groupmembers = {
      db.list[Pair[String,String]]("select * from groupmembers") {
        (rs: ResultSet) => Pair(
          rs.getString("groupname"),
          rs.getString("userid")
        )
      }
    }

    _allusers = {
      db.list[User]("select * from users order by userid") {
        (rs: ResultSet) => User(
          rs.getString("userid"),
          rs.getString("username"),
          rs.getString("email"),
          rs.getBoolean("enabled")
        )
      }
    }
    _userMap = Map() ++ (_allusers.map(user => (user.userid, user)))
    _users = _allusers.filter(_.enabled)

    _groups = {
      db.list[Group]("select groupname from groups order by groupname") {
        (rs: ResultSet) => Group(
          rs.getString("groupname")
        )
      }
    }
    _groupMap = Map() ++ (_groups.map(group => (group.groupname, group)))

    _permissions = {
      db.list[Permission]("select * from permissions") {
        (rs: ResultSet) => Permission(
          rs.getString("id"),
          rs.getString("groupname"),
          rs.getString("project"),
          rs.getString("env"),
          rs.getString("action")
        )
      }
    }

    _json = init_json()
  }
 
  def machines = _machines
  def servers = _servers
  def clusters = _clusters
  def domains = _domains
  def allusers = _allusers
  def users = _users
  def groups = _groups
  def permissions = _permissions
  def adminservers = _servers.filter(_.servertype == "admin")
  def managedservers = _servers.filter(_.servertype == "managed")

  def getMachine(name: String) = _machineMap(name)

  def findMachine(host: String): Option[Machine] = {
    _machines.find(m => m.name == host || m.address == host)
  }

  def findNodeManager(machinename: String): Option[Server] = {
    _servers.find(
      s => s.machinename == machinename && s.servertype == "nodemanager"
    )
  }

  def getDomain(name: String) = _domainMap(name)

  def getCluster(name: String) = _clusterMap(name)

  def getServer(name: String) = _serverMap(name)

  def getUser(id: String) = _userMap(id)

  def getGroup(name: String) = _groupMap(name)

  def getGroupsForUser(userid: String): List[String] = {
    _groupmembers.filter(_._2 == userid).map(_._1)
  }

  def getMembersForGroup(groupname: String): List[String] = {
    _groupmembers.filter(_._1 == groupname).map(_._2)
  }

  def hasUser(name : String) : Boolean = _userMap.contains(name)

  def hasMachine(name : String) : Boolean = _machineMap.contains(name)

  def hasDomain(name : String) : Boolean = _domainMap.contains(name)

  def hasCluster(name : String) : Boolean = _clusterMap.contains(name)

  def hasServer(name : String) : Boolean = _serverMap.contains(name)

  private def init_json(): String = {
    val buf = new StringBuilder()
    buf.append("{ ")
    buf.append(quote("permissions")).append(" : ")
    buf.append(_permissions.json)
    buf.append(", ")
    buf.append(quote("machines")).append(" : ")
    buf.append(_machines.json)
    buf.append(", ")
    buf.append(quote("domains")).append(" : ")
    buf.append(_domains.json)
    buf.append(", ")
    buf.append(quote("servers")).append(" : ")
    buf.append(_servers.json)
    buf.append(", ")
    buf.append(quote("users")).append(" : ")
    buf.append(_users.json)
    buf.append(" }")
    buf.toString
  }

  def json() = _json

  /**
   * Add a server to the doom database.
   */
  def addServer(server: Server, reload: Boolean = true) {
    db.execute(
      "insert into servers (servername, servertype, domainname, clustername, machinename, address, port, jmxport, blocked) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
      server.name,
      server.servertype,
      server.domainname,
      server.clustername,
      server.machinename,
      server.address,
      server.port,
      server.jmxport,
      server.blocked
    )

    if (reload) load()
 
  }

  /**
   * Add a domain to the doom database.
   */
  def addDomain(domain: Domain, reload: Boolean = true) {
    db.execute(
      "insert into domains (domainname, domaintype, domaingroup, project, env, java_vendor, java_home, bea_home, wl_home, wl_version, osb_home, osb_version, alsb_home, alsb_version) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
      domain.name,
      domain.domaintype,
      domain.domaingroup,
      domain.project,
      domain.env,
      domain.java_vendor,
      domain.java_home,
      domain.mw_home,
      domain.wl_home,
      domain.wl_version,
      domain.osb_home,
      domain.osb_version,
      domain.alsb_home,
      domain.alsb_version
    )

    for (server <- domain.servers) {
      addServer(server.copy(domainname = domain.name), false)
    }

    for (server <- domain.loadbalancers) {
      addServer(server.copy(domainname = domain.name), false)
    }

    for (cluster <- domain.clusters) {
      for (server <- cluster.servers) {
        addServer(
          server.copy(
            domainname = domain.name
          ).copy(
            clustername = cluster.name
          ),
          false
        )
      }
    }

    for (server <- domain.managedservers) {
      addServer(
        Server(
          name = makeNodeManagerName(server),
          servertype = "nodemanager",
          domainname = domain.name,
          machinename = server.machinename,
          address = server.address,
          port = Config.getInt("doom.nodemanager.port", 5556),
          jmxport = Config.getInt("doom.nodemanager.jmxport", 3000)
        ),
        false
      )
    }

    if (reload) load()
  }

  /**
   * Add a machine to the doom database.
   */
  def addMachine(machine: Machine, reload: Boolean = true) {
    db.execute(
      "insert into machines (machinename, address, os_user) values (?, ?, ?)",
      machine.name,
      machine.address,
      machine.user
    )

    if (reload) load()
  }

  /**
   * Remove a domain from the doom database.
   */
  def removeDomain(domain: Domain, reload: Boolean = true) {
    db.execute("delete from servers where domainname = ?", domain.name)
    db.execute("delete from domains where domainname = ?", domain.name)

    if (reload) load()
  }

  /**
   * Remove a server from the doom database.
   */
  def removeServer(server: Server, reload: Boolean = true) {
    log.debug("removeServer " + server.domainname + " " + server.name)
    db.execute(
      "delete from servers where domainname = ? and servername = ?",
      server.domainname,
      server.name
    )

    if (reload) load()
  }

  /**
   * Remove a machine from the doom database.
   */
  def removeMachine(machine: Machine, reload: Boolean = true) {
    db.execute(
      "delete from machines where machinename = ?",
      machine.name
    )

    if (reload) load()
  }

  /**
   * Remove a list of machines from the doom database.
   */
  def removeMachines(machines: List[String], reload: Boolean = true) {
    db.execute(
      "delete from machines where machinename in ?",
      machines
    )

    if (reload) load()
  }

  def makeNodeManagerName(server: Server): String = {
    val Name = """(.*)_server_([0-9]+)$""".r
    server.name match {
      case Name(prefix, num) => prefix + "_nm_" + num
      case _ => server.name + "_nm"
    }
  }
}
