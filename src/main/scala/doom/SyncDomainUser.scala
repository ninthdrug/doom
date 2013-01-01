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

import ninthdrug.logging.Logger

class SyncDomainUser(
  domain: String,
  host: String,
  port: Int,
  admin: String,
  password: String,
  user: WeblogicUser
) extends Command {
  private val log = Logger("doom.SyncDomainUser")
       
  def result() : Result = {
    log.info("syncing user " + user.name + " for " + domain + " on " + host + ":" + port)
    var jmx: WeblogicJMX = null
    try {
      jmx = WeblogicJMX(host, port, admin, password)
      if (user.enabled) {
        jmx.createUser(user.name, user.password, user.description, user.groups)
      } else {
        if (jmx.userExists(user.name)) {
          jmx.removeUser(user.name)
        }
      }
      Result()
    } catch {
      case e : Exception => {
        Result("unable to sync user " + user.name + " on " + host, e)
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

object SyncDomainUser {

  val Administrators = "Administrators"
  val Monitors = "Monitors"
  val Operators = "Operators"
  val AppTesters = "AppTesters"
  val IntegrationDeployers = "IntegrationDeployers"

  def apply(domainname: String, userid: String): SyncDomainUser = {
    val domain = ConfigCache.getDomain(domainname)
    val server = domain.adminserver
    val password = CredentialCache.getPassword(
      "weblogic",
      domain.name,
      "weblogic"
    )
      
    new SyncDomainUser(
      domainname,
      server.address,
      server.port.toInt,
      "weblogic",
      password,
      getUser(domainname, userid)
    )
  }

  def getUser(domainname: String, userid: String): WeblogicUser = {
    import scala.collection.mutable.ListBuffer

    val domain = ConfigCache.getDomain(domainname)
    val project = domain.project
    val env = domain.env
    val user = ConfigCache.getUser(userid)
    val groups = ListBuffer[String]()
    if (user.enabled) {
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
    val enabled = user.enabled && groups.nonEmpty
    val password = if (enabled) {
      CredentialCache.getPassword("doom", "doom", userid)
    } else {
      ""
    }
    WeblogicUser(userid, password, user.username, groups.toList, enabled)
  }
}
