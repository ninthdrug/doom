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

import ninthdrug.util.Util.quote

case class Permission(
  val id: String,
  val groupname: String,
  val project: String,
  val env: String,
  val action: String
) {
  def json() = {
    "{ " +
    quote("groupname") + " : " + quote(groupname) + ", " +
    quote("project") + " : " + quote(project) + ", " +
    quote("env") + " : " + quote(env) + ", " +
    quote("action") + " : " + quote(action) +
    " }"
  }
}

object Permission {
  val actions = List(
    "view",
    "control"
  )

  def check(
    userid: String,
    project: String,
    env: String,
    action: String
  ) : Boolean = {
    val mygroups = ConfigCache.getGroupsForUser(userid)
    for (p <- ConfigCache.permissions) {
      if ((p.groupname == "" || mygroups.contains(p.groupname)) &&
        (p.project == "" || p.project == project) &&
        (p.env == "" || p.env == env) &&
        (p.action == "" || p.action == action)) {
        return true
      }
    }
    return false
  }

  def canControlServer(userid : String, servername : String) : Boolean = {
    val server = ConfigCache.getServer(servername)
    val domain = ConfigCache.getDomain(server.domainname)
    return check(userid, domain.project, domain.env, "control")
  }

  def canControlDomain(userid : String, domainname : String) : Boolean = {
    val domain = ConfigCache.getDomain(domainname)
    return check(userid, domain.project, domain.env, "control")
  }

  def apply(
    groupname : String,
    project : String,
    env: String,
    action: String
  ) : Permission = {
    new Permission(null, groupname, project, env, action)
  }
}
