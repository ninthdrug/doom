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
import doom.ConfigCache

class WorkOrder(
  val orderid: Int,
  val userid: String,
  val command: String,
  val servername: String,
  val startDate: java.sql.Timestamp,
  var endDate: java.sql.Timestamp,
  var status: String
) {
  lazy val json =
    "{" +
    "\"username\" : \"" + ConfigCache.getUser(userid).username + "\", " +
    "\"servername\" : \"" + servername + "\", " +
    "\"startdate\" : \"" + startDate + "\", " +
    "\"enddate\" : \"" + endDate + "\", " +
    "\"command\" : \"" + command + "\", " +
    "\"status\" : \"" + status + "\" " +
    "}"
}

object WorkOrder {
  def apply(
    orderid: Int,
    userid: String,
    command: String,
    servername: String,
    startDate: java.sql.Timestamp,
    endDate: java.sql.Timestamp,
    status: String
  ) = {
    new WorkOrder(
      orderid,
      userid,
      command,
      servername,
      startDate,
      endDate,
      status
    )
  }

  def apply(
    userid: String,
    command: String,
    servername: String,
    startDate: java.sql.Timestamp,
    endDate: java.sql.Timestamp,
    status: String
  ) = {
    new WorkOrder(
      AuditDB.nextId,
      userid,
      command,
      servername,
      startDate,
      endDate,
      status
    )
  }

  def apply(order: WorkOrder) = {
    new WorkOrder(
      order.orderid,
      order.userid,
      order.command,
      order.servername,
      order.startDate,
      order.endDate,
      order.status
    )
  }
}
