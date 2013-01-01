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

case class Datasource(
  name: String,
  domainname: String,
  vendor: String,
  driver: String,
  dbname: String,
  dbhost: String,
  dbport: String,
  username: String,
  password: String,
  globaltransaction: String,
  connection_min: Int = 10,
  connection_max: Int = 10,
  properties: List[Tuple2[String,String]] = List()
) {
  val jndiname = name
}
