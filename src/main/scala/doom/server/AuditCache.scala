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

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object AuditCache {
  private val _cache = mutable.Map[AuditSearch, String]() 

  def find(config: AuditSearch): List[WorkOrder] = {
    AuditDB.find(config)
  }

  def findJSON(config: AuditSearch): String = {
    if (!_cache.contains(config)) {
      _cache.update(config, toJSON(find(config)))
    }
    _cache(config)
  }

  def log(workOrder: WorkOrder) {
    AuditDB.log(workOrder)
    _cache.clear()
  }

  private def toJSON(list: List[WorkOrder]): String = {
    val buf = new StringBuilder()
    buf.append("[ ")
    for (order <- list) {
      if (! buf.endsWith("[ ")) {
        buf.append(", ")
      }
      buf.append(order.json)
    }
    buf.append(" ]")
    buf.toString
  }
}
