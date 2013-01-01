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
import java.sql.Timestamp
import ninthdrug.util.Json.quote
import ninthdrug.util.Util.formatTimestamp

case class Credential(
  val id: String,
  val credgroup: String,
  val realmtype: String,
  val realm: String,
  val principal: String,
  val password: String,
  val env: String,
  val permgroup: String,
  val url: String,
  val notes: String,
  val verified: Boolean,
  val date_verified: Timestamp
) {
  def json() = {
    "{ " +
    quote("id") + ":" + quote(id) + ", " +
    quote("credgroup") + ":" + quote(credgroup) + ", " +
    quote("realmtype") + ":" + quote(realmtype) + ", " +
    quote("realm") + ":" + quote(realm) + ", " +
    quote("principal") + ":" + quote(principal) + ", " +
    quote("password") + ":" + quote(password) + ", " +
    quote("env") + ":" + quote(env) + ", " +
    quote("permgroup") + ":" + quote(permgroup) + ", " +
    quote("url") + ":" + quote(url) + ", " +
    quote("notes") + ":" + quote(notes) + ", " +
    quote("verified") + ":" + verified + ", " +
    quote("date_verified") + ":" + quote(formatTimestamp(date_verified)) +
    " }"
  }
}

object Credential {

  def apply(
    credgroup: String,
    realmtype: String,
    realm: String,
    principal: String,
    password: String,
    env: String = "",
    permgroup: String = "",
    url: String = "",
    notes: String = "",
    verified: Boolean = false,
    date_verified: Timestamp = null
  ): Credential = {
    new Credential(
        null,
        credgroup,
        realmtype,
        realm,
        principal,
        password,
        env,
        permgroup,
        url,
        notes,
        verified,
        date_verified
    )
  }

  def apply(
    realmtype: String,
    realm: String,
    principal: String,
    password: String
  ): Credential = {
    new Credential(
        null,
        null,
        realmtype,
        realm,
        principal,
        password,
        null,
        null,
        null,
        null,
        false,
        null
    )
  }
}
