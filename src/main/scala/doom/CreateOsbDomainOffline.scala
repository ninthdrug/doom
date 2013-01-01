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
import ninthdrug.expect.Expect
import scala.util.matching._
import ninthdrug.logging.Logger

case class CreateOsbDomainOffline(
  domain: Domain,
  password: String,
  datasources: List[Datasource] = List[Datasource]()
) extends Command {
  private val log = Logger("doom.CreateOsbDomainOffline")

  def result(): Result = {
    try {

      val cmd = domain.mw_home + "/oracle_common/common/bin/config.sh -mode=console"
      val child = Expect.spawn(cmd)

      child.expect("->1|Create a new WebLogic domain")
      child.expect("[Next]> ")
      child.sendline("Next")

      child.expect("->1|Choose Weblogic Platform components")
      child.expect("[Next]> ")
      child.sendline("Next")

      child.expect("Application Template Selection:")
      child.expect("[Next]> ")
      val re = new Regex("""Oracle Service Bus - 11\.1\.1\.[56] \[osb\] \[5\]""")
      re.findFirstMatchIn(child.before) match {
        case None =>
          log.error("Cannot find option for Oracle Service Bus.")
          sys.exit(-1)
        case Some(m) =>
          child.sendline("5")
      }

      child.expect("[Next]> ")
      child.sendline("Next")

      child.expect("Edit Domain Information:")
      child.expect("""Enter value for "Name" """)
      child.expect("[Next]> ")
      child.sendline(domain.name)

      child.expect("[Next]> ")
      child.sendline("Next")

      child.expect("Select the target domain directory for this domain:")
      child.expect("[Next]> ")
      child.sendline(domain.domains_dir)

      child.expect("Configure Administrator User Name and Password:")
      child.expect("[Next]> ")
      child.sendline("2")

      child.expect("User password:")
      child.expect("[Accept]> ")
      child.sendline(password)

      child.expect("[Next]> ")
      child.sendline("3")

      child.expect("Confirm user password:")
      child.sendline(password)

      child.expect("[Next]> ")
      child.sendline("Next")

      child.expect("->1|Development Mode")
      child.expect("[Next]> ")
      child.sendline("Next")

      child.expect("->1|Sun SDK 1.6.0_")
      child.expect("[Next]> ")
      child.sendline("Next")

      child.expect("Configure JDBC Data Sources:")
      child.expect("1| wlsbjmsrpDataSource")
      child.expect("1 - Modify")
      child.expect("[Next]> ")
      datasources.find(
        ds =>
          ds.name == "wlsbjmsrpDataSource" &&
          ds.domainname == domain.name
      ) match {
        case None =>
        case Some(datasource) =>
          child.sendline("1")

          child.expect("Configure JDBC Data Sources:")
          child.expect("1| wlsbjmsrpDataSource")
          child.expect("[Next]> ")
          child.sendline("1")

          child.expect("Configure JDBC Data Sources:")
          child.expect("""2 - Modify "JDBC driver params vendor""")
          child.expect("[Accept]> ")
          child.sendline("2")

          child.expect("Configure JDBC Data Sources:")
          child.expect("""*Enter index number to modify "Value""")
          child.expect("[Accept]> ")
          var choice = select_database_vendor(datasource.vendor, child.before)
          child.sendline(choice)

          child.expect("Configure JDBC Data Sources:")
          child.expect("""3 - Modify "JDBC driver params driver""")
          child.expect("[Accept]> ")
          child.sendline("3")

          child.expect("Configure JDBC Data Sources:")
          child.expect("""*Enter index number to modify "Value""")
          child.expect("[Accept]> ")
          choice = select_database_driver(datasource.driver, child.before)
          child.sendline(choice)

          child.expect("Configure JDBC Data Sources:")
          child.expect("""4 - Modify "JDBC driver params dbms name""")
          child.expect("[Accept]> ")
          child.sendline("4")

          child.expect("""Enter value for "JDBC driver params dbms name""")
          child.expect("[Accept]> ")
          child.sendline(datasource.dbname)

          child.expect("Configure JDBC Data Sources:")
          child.expect("""5 - Modify "JDBC driver params dbms host""")
          child.expect("[Accept]> ")
          child.sendline("5")

          child.expect("""Enter value for "JDBC driver params dbms host""")
          child.expect("[Accept]> ")
          child.sendline(datasource.dbhost)

          if (datasource.dbport != null) {
            child.expect("Configure JDBC Data Sources:")
            child.expect("""6 - Modify "JDBC driver params dbms port""")
            child.expect("[Accept]> ")
            child.sendline("6")

            child.expect("""Enter value for "JDBC driver params dbms port""")
            child.expect("[Accept]> ")
            child.sendline(datasource.dbport.toString)
          }

          child.expect("Configure JDBC Data Sources:")
          child.expect("""7 - Modify "JDBC driver params user name""")
          child.expect("[Accept]> ")
          child.sendline("7")

          child.expect("""Enter value for "JDBC driver params user name""")
          child.expect("[Accept]> ")
          child.sendline(datasource.username)

          child.expect("Configure JDBC Data Sources:")
          child.expect("""8 - Modify "JDBC driver params user password""")
          child.expect("[Accept]> ")
          child.sendline("8")

          child.expect("Enter new *JDBC driver params user password encrypted:")
          child.expect("[Accept]> ")
          child.sendline(datasource.password)

          child.expect("Configure JDBC Data Sources:")
          child.expect("""9 - Modify "JDBC driver params confirm user password""")
          child.expect("[Accept]> ")
          child.sendline("9")

          child.expect("Enter new *JDBC driver params confirm user password")
          child.expect("[Accept]> ")
          child.sendline(datasource.password)

          child.expect("Configure JDBC Data Sources:")
          child.expect("[Accept]> ")
          child.sendline("Accept")

          child.expect("Configure JDBC Data Sources:")
          child.expect("[Next]> ")
      }
      child.sendline("Next")

      child.expect("Run Database Scripts:")
      child.expect("->2|Skip JDBC Configuration")
      child.expect("[Next]> ")
      child.sendline("Next")

      child.expect("Select Optional Configuration:")
      child.expect("1|Administration Server [ ]")
      child.expect("[Next]> ")
      child.sendline("1")

      child.expect("Select Optional Configuration:")
      child.expect("3|Managed Servers, Clusters and Machines [ ]")
      child.expect("[Next]> ")
      child.sendline("3")

      child.expect("Select Optional Configuration:")
      child.expect("[Next]> ")
      child.sendline("Next")

      child.expect("Configure the Administration Server:")
      child.expect("""1 - Modify "Name""")
      child.expect("[Next]> ")
      child.sendline("1")

      child.expect("[Next]> ")
      child.sendline(domain.adminserver.name)

      child.expect("""2 - Modify "Listen address""")
      child.expect("[Next]> ")
      child.sendline("2")

      child.expect("""Enter value for "Listen address" """)
      child.expect("[Next]> ")
      child.sendline(domain.adminserver.address)

      child.expect("""3 - Modify "Listen port""")
      child.expect("[Next]> ")
      child.sendline("3")

      child.expect("""Enter value for "Listen port" """)
      child.expect("[Next]> ")
      child.sendline(domain.adminserver.port.toString)

      child.expect("Configure the Administration Server:")
      child.expect("[Next]> ")
      child.sendline("Next")

      child.expect("Configure Managed Servers:")
      child.expect("Add or delete configuration information for Managed Servers.")
      if (domain.managedservers.isEmpty) {
        child.expect("1|osb_server1")
        child.expect("3 - Delete")
        child.expect("[Next]> ")
        child.sendline("3")

        child.expect("1|osb_server1")
        child.expect("Enter row number to delete")
        child.expect("[Next]> ")
        child.sendline("1")
      } else {
        val server = domain.managedservers.head
        child.expect("2 - Modify")
        child.expect("[Next]> ")
        child.sendline("2")
      
        child.expect("1|osb_server1")
        child.expect("Enter row number to modify")
        child.expect("[Next]> ")
        child.sendline("1")

        child.expect("""1 - Modify "Name""")
        child.expect("[Next]> ")
        child.sendline("1")

        child.expect("""Modify "Name""")
        child.expect("[Next]> ")
        child.sendline(server.name)
      
        child.expect("""2 - Modify "Listen address""")
        child.expect("[Next]> ")
        child.sendline("2")

        child.expect("""Modify "Listen address" OR """)
        child.expect("[Next]> ")
        child.sendline(server.address)

        child.expect("""3 - Modify "Listen port""")
        child.expect("[Next]> ")
        child.sendline("3")

        child.expect("""Modify "Listen port" OR """)
        child.expect("[Next]> ")
        child.sendline(server.port.toString)

        child.expect("5 - Done")
        child.expect("[Next]> ")
        child.sendline("5")

        for (server <- domain.managedservers.tail) {
          child.expect("Configure Managed Servers:")
          child.expect("Add or delete configuration information for Managed Servers.")
          child.expect("1 - Add")
          child.expect("[Next]> ")
          child.sendline("1")

          child.expect("Enter name for a new")
          child.expect("[Next]> ")
          child.sendline(server.name)

          child.expect("""2 - Modify "Listen address""")
          child.expect("[Next]> ")
          child.sendline("2")

          child.expect("""Modify "Listen address" OR """)
          child.expect("[Next]> ")
          child.sendline(server.address)

          child.expect("""3 - Modify "Listen port""")
          child.expect("[Next]> ")
          child.sendline("3")

          child.expect("""Modify "Listen port" """)
          child.expect("[Next]> ")
          child.sendline(server.port.toString)

          child.expect("5 - Done")
          child.expect("[Next]> ")
          child.sendline("5")
        }
      }

      child.expect("Configure Managed Servers:")
      child.expect("[Next]> ")
      child.sendline("Next")

      for (cluster <- domain.clusters) {
        child.expect("Configure Clusters:")
        child.expect("[Next]> ")
        if (child.before.contains("Enter name for a new Cluster")) {
          child.sendline(cluster.name)
        } else if (child.before.contains("1 - Add Cluster")) {
          child.sendline("1")
          child.expect("Enter name for a new Cluster")
          child.expect("[Next]> ")
          child.sendline(cluster.name)
        }
        child.expect("Configure Clusters:")
        child.expect("4 - Done")
        child.expect("[Next]> ")
        child.sendline("4")
      }
      child.expect("Configure Clusters:")
      child.expect("[Next]> ")
      child.sendline("Next")

      for (cluster <- domain.clusters) {
        val re = new Regex(cluster.name + """ \[(\d+)\]""")
        child.expect("Assign Servers to Clusters:")
        child.expect("[Next]> ")
        re.findFirstMatchIn(child.before) match {
          case None =>
            log.error("Cannot find match for cluster " + cluster.name)
            sys.exit(-1)
          case Some(m) =>
            child.sendline(m.group(1))
            child.expect("Select WebLogic Servers and assign them to a cluster.")
            child.expect("1 - Select")
            child.expect("[Accept]> ")
            child.sendline("1")

            child.expect("Select WebLogic Servers and assign them to a cluster.")
            child.expect("[Accept]> ")

            val selection = select_cluster_servers(cluster, child.before)
            child.sendline(selection)
            child.expect("Use above value or select another option:")
            child.expect("[Accept]> ")
            child.sendline("Accept")
        }
      }

      child.expect("Assign Servers to Clusters:")
      child.expect("[Next]> ")
      child.sendline("Next")

      for (machinename <- domain.machinenames) {
        child.expect("Configure Machines:")
        child.expect("[Next]> ")
        if (child.before.contains("Enter name for a new Machine")) {
          child.sendline(machinename)
        } else if (child.before.contains("1 - Add Machine")) {
          child.sendline("1")
          child.expect("Enter name for a new Machine")
          child.expect("[Next]> ")
          child.sendline(machinename)
        }
        child.expect("""2 - Modify "Node manager listen address""")
        child.expect("[Next]> ")
        domain.managedservers.find(s => s.machinename == machinename) match {
          case Some(server) =>
            child.sendline("2")

            child.expect("[Next]> ")
            child.sendline(server.address)
          case None =>
        }
        child.expect("4 - Done")
        child.expect("[Next]> ")
        child.sendline("4")
      }

      child.expect("Configure Machines:")
      child.expect("[Next]> ")
      child.sendline("Next")

      child.expect("Configure Unix Machines:")
      child.expect("[Next]> ")
      child.sendline("Next")

      for (machinename <- domain.machinenames) {
        child.expect("Assign Servers to Machines:")
        child.expect("[Next]> ")
        val re = new Regex(machinename + """ \[([0-9.]+)\]""")
        re.findFirstMatchIn(child.before) match {
          case None =>
            log.error("Cannot assign servers to machine " + machinename)
            sys.exit(-1)
          case Some(m) =>
            child.sendline(m.group(1))

            child.expect("Use above value or select another option:")
            child.expect("1 - Select")
            child.expect("[Accept]> ")
            child.sendline("1")

            child.expect("Select WebLogic Servers and assign them to a machine.")
            child.expect("[Accept]> ")
            val selection = select_servers_for_machine(
              domain.managedservers,
              machinename,
              child.before
            )
            child.sendline(selection)

            child.expect("[Accept]> ")
            child.sendline("Accept")
        }
      }

      child.expect("Assign Servers to Machines:")
      child.expect("[Next]> ")
      child.sendline("Next")

      child.expect("Creating Domain...")
      child.expect("Domain Created Successfully!")

      Result()
    } catch {
      case e: Exception => Result(e)
    }
  }

  private def select_database_vendor(vendor: String, text: String): String = {
    val re = new Regex("""(\d+)\|""" + vendor)
    re.findFirstMatchIn(text) match {
      case None =>
        throw new CommandException("ERROR: Cannot find database vendor " + vendor)
      case Some(m) =>
        m.group(1)
    }
  }

  private def select_database_driver(driver: String, text: String): String = {
    val re = new Regex("""(\d+)\|.*Oracle's *Driver \(Thin XA\) for Instance connections""")
    re.findFirstMatchIn(text) match {
      case None =>
        throw new CommandException("ERROR: Cannot find option for dbms driver")
      case Some(m) =>
        m.group(1)
    }
  }

  private def select_cluster_servers(cluster: Cluster, text: String): String = {
    val buf = new StringBuilder()
    for (server <- cluster.servers) {
      val re = new Regex("""(\d+)\|""" + server.name)
      re.findFirstMatchIn(text) match {
        case None =>
          throw new CommandException("ERROR: Cannot assign " + server.name)
        case Some(m) =>
          if (buf.length > 0) buf.append(",")
          buf.append(m.group(1))
      }
    }
    buf.toString
  }

  private def select_servers_for_machine(
    servers: List[Server],
    machinename: String,
    text: String
  ): String = {
    val buf = new StringBuilder()
    for (server <- servers; if server.machinename == machinename) {
      val re = new Regex("""(\d+)\|""" + server.name)
      re.findFirstMatchIn(text) match {
        case None =>
          throw new CommandException("ERROR: Cannot assign " + server.name)
        case Some(m) =>
          if (buf.length > 0) buf.append(",")
          buf.append(m.group(1))
      }
    }
    buf.toString
  }
}
