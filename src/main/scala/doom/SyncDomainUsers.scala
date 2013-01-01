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

import scala.collection.mutable.ListBuffer
import ninthdrug.logging.Logger

class SyncDomainUsers(
  domain: String,
  host: String,
  port: Int,
  user: String,
  password: String,
  users: List[WeblogicUser]
) extends Command {
  private val log = Logger("doom.SyncDomainUsers")
       
  def result() : Result = {
    log.info("syncing users for " + domain + " on " + host + ":" + port)
    val errors = ListBuffer[String]()
    var jmx: WeblogicJMX = null
    try {
      jmx = WeblogicJMX(host, port, user, password)
      for (user <- users) {
        try {
          if (user.enabled) {
            jmx.createUser(
              user.name,
              user.password,
              user.description,
              user.groups
            )
          } else {
            if (jmx.userExists(user.name)) {
              jmx.removeUser(user.name)
            }
          }
        } catch {
          case e: Exception => {
            errors.append(
              "Error syncing " + user.name + " on " + host + ": " + e.getMessage
            )
          }
        }
      }
      if (errors.length == 0) {
        Result()
      } else {
        Result(errors.length, errors.mkString("\n"))
      }
    } catch {
      case e : Exception => {
        Result("unable to sync users on " + host, e)
      }
    } finally {
      if (jmx != null) {
        try {
          jmx.close()
        } catch {
          case e : Exception =>
        }
      }
    }
  }
}

object SyncDomainUsers {

  val Administrators = "Administrators"
  val Monitors = "Monitors"
  val Operators = "Operators"
  val AppTesters = "AppTesters"
  val IntegrationDeployers = "IntegrationDeployers"

  def apply(domainname : String) : SyncDomainUsers = {
    val domain = ConfigCache.getDomain(domainname)
    val server = domain.adminserver
    val password = CredentialCache.getPassword(
      "weblogic",
      domain.name,
      "weblogic"
    )
      
    new SyncDomainUsers(
      domainname,
      server.address,
      server.port.toInt,
      "weblogic",
      password,
      getUsers(domainname)
    )
  }

  def getUsers(domainname : String) : List[WeblogicUser] = {
    import scala.collection.mutable.ListBuffer

    val domain = ConfigCache.getDomain(domainname)
    val project = domain.project
    val env = domain.env
    val users = ListBuffer[WeblogicUser]()
    for (u <- ConfigCache.allusers) {
      val groups = ListBuffer[String]()
      val userid = u.userid
      if (u.enabled) {
        if (Permission.check(userid, project, env, "bea_admin")) {
          groups += Administrators
        } else {
          if (Permission.check(userid, project, env, "bea_operator")) {
            groups += Operators
          }
          if (Permission.check(userid, project, env, "bea_monitor")) {
            groups += Monitors
          }
          if (Permission.check(userid, project, env, "bea_apptester")) {
            groups += AppTesters
          }
          if (domain.domaintype == "osb" || domain.domaintype == "alsb") {
            if (Permission.check(userid, project, env, "bea_integrationdeployer")) {
              groups += IntegrationDeployers
            }
          }
        }
      }
      val enabled = u.enabled && groups.nonEmpty
      val password = if (enabled) {
        CredentialCache.getPassword("doom", "doom", u.userid)
      } else {
        ""
      }
      users.append(
        WeblogicUser(u.userid, password, u.username, groups.toList, enabled)
      )
    }
    users.toList
  }
}
