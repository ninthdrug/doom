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

import ninthdrug.command._

class CheckQueueMessageCount(
  val queue: String,
  val host: String,
  val port: Int,
  val user: String,
  val password: String
) extends Command {

  def result(): Result = {
    var jmx: WeblogicJMX = null
    try {
      jmx = WeblogicJMX(host, port, user, password)
      val count = jmx.getQueueMessageCount(queue)
      Result(count.toString)
    } catch {
      case e: UnknownQueueException =>
        Result("UnknownQueue: " + queue, e)
      case e: AmbiguousQueueException =>
        Result("AmbiguousQueue: " + queue, e)
      case e: Exception =>
        Result("UnknownErrorWithQueue: " + queue, e)
    } finally {
      try {
        jmx.close()
      } catch {
        case _ =>
      }
    }
  }
}

object CheckQueueMessageCount {
  def apply(queue: String, host: String, port: Int, user: String, password: String) =
    new CheckQueueMessageCount(queue, host, port, user, password)
}
