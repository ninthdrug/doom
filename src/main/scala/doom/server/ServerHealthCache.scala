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
import ninthdrug.util.Json.quote
import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable
import ninthdrug.logging.Logger

object ServerHealthCache extends Actor {
  val log = Logger("doom.server.ServerHealthCache")
  val size = ConfigCache.servers.size
  val serverNames = new Array[String](size)
  val serverHealths = new Array[String](size)
  val counts = new Array[Int](size)
  val times = new Array[Long](size)
  for (i <- 0 until size) {
    serverHealths(i) = "UNKNOWN"
    counts(i) = 0
    times(i) = 0
  }

  private val name2index = mutable.Map[String, Int]()

  for (i <- 0 until size) {
    val server = ConfigCache.servers(i)
    serverNames(i) = server.name
    name2index.update(server.name, i)
  }

  def getHealth(servername: String): String = {
    serverHealths(name2index(servername))
  }

  def json() : String = {
    val buf = new StringBuilder()
    buf.append("{ ")
    for (i <- 0 until size) {
      if (i > 0) buf.append(", ")
      buf.append(quote(serverNames(i)))
      buf.append(" : ")
      buf.append(quote(serverHealths(i)))
    }
    buf.append(" }")
    buf.toString
  }

  def act () {
    while(true) {
      receive {
        case u : ServerHealthUpdate => {
          update(u)
        }
      }
    }
  }

  private def update(u : ServerHealthUpdate) {
    if (name2index.contains(u.servername)) {
      val i = name2index(u.servername)
      if (times(i) == u.time) {
        if (counts(i) < Integer.MAX_VALUE) {
          counts(i) = counts(i) + 1
        }
        if (counts(i) > 2) {
          serverHealths(i) = "UNKNOWN"
        } else {
          serverHealths(i) = u.health
        }
      } else {
        counts(i) = 0
        times(i) = u.time
        serverHealths(i) = u.health
      }
    } else {
      log.error("ServerHealthCache: unknown server: " + u.servername)
    }
  }
}
