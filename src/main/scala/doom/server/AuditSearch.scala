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
package doom.server
import ninthdrug.sql.Database

case class AuditSearch(
  user_filter: String,
  server_filter: String,
  limit: Int,
  offset: Int
) {
  val sql = {
    val where = if (user_filter != "") {
      " where userid = " + Database.quote(user_filter)
    } else if (server_filter != "") {
      " where servername LIKE " + Database.quote("%" + server_filter + "%")
    } else {
      ""
    }
    "SELECT * FROM audit" + where + " ORDER BY date DESC " +
    "LIMIT " + limit + " OFFSET " + offset
  }
}
