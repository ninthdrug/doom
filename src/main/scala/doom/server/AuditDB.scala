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

import java.sql._
import ninthdrug.config.Config
import ninthdrug.sql._

object AuditDB {
  private val dbuser = Config.get("doom.dbuser")
  private val dbpass = Config.get("doom.dbpassword")
  private val dburl = Config.get("doom.dburl")
  private val db = Database(dburl, dbuser, dbpass)
   
  def log(order: WorkOrder) {
    val update_sql = "UPDATE audit SET status=?, enddate=? where id=?"
    var insert_sql = "INSERT INTO audit(id, date, userid, servername, action, status, enddate) VALUES (?,?,?,?,?,?,?)"
    if (db.exists("select id from audit where id=?", order.orderid)) {
      db.update(update_sql, order.status, order.endDate, order.orderid)
    } else {
      db.execute(
        insert_sql,
        order.orderid,
        order.startDate,
        order.userid,
        order.servername,
        order.command,
        order.status,  
        order.endDate
        )
    }
  }

  def find(search: AuditSearch): List[WorkOrder] = {
    db.list[WorkOrder](search.sql) {
      (rs: ResultSet) => WorkOrder(
        rs.getInt("id"),
        rs.getString("userid"),
        rs.getString("action"),
        rs.getString("servername"),
        rs.getTimestamp("date"),
        rs.getTimestamp("enddate"),
        rs.getString("status")
      )
    }
  }

  def nextId(): Int = {
    db.nextInt("audit_id_seq")
  }
}
