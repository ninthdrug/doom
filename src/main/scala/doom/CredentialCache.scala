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

import java.sql._
import scala.collection.mutable.ListBuffer
import ninthdrug.config.EncryptionKey
import ninthdrug.sql._
import ninthdrug.util.Base64
import ninthdrug.util.Crypto
import ninthdrug.util.Util

object CredentialCache {
  private var db: Database = null

  private var _credentials = List[Credential]()

  load()

  def load() {
    val dburl = Config.get("doom.dburl")
    val dbuser = Config.get("doom.dbuser")
    val dbpass = Config.get("doom.dbpassword")
    db = Database(dburl, dbuser, dbpass)

    _credentials = {
      db.list[Credential]("select * from credentials") {
        (rs: ResultSet) => Credential(
          rs.getString("id"),
          rs.getString("realmtype"),
          rs.getString("realm"),
          rs.getString("principal"),
          decrypt(rs.getString("password"))
        )
      }
    }
  }
 
  def credentials = _credentials

  /**
   * Return credential for specified id or throw an exception.
   */
  def getCredential(id: String): Credential = {
    _credentials.find(_.id == id) match {
      case None => throw new java.util.NoSuchElementException(
        "key not found: " + id
      )
      case Some(credential) => credential
    }
  }


  def hasCredential(id: String): Boolean = {
    _credentials.exists(_.id == id)
  }

  /**
   * Return password or throw CredentialNotFoundException.
   */
  def getPassword(
    realmtype: String,
    realm: String,
    principal: String
  ): String = {
    _credentials.find(c => 
      c.realmtype == realmtype &&
      c.realm == realm &&
      c.principal == principal
    ) match {
      case Some(credential) => credential.password
      case None => throw new CredentialNotFoundException(
        "Credential not found for " + 
        realmtype + " " + realm + " " + principal
      )
    }
  }

  /**
   * Return doom password or throw CredentialNotFoundException.
   */
  def getDoomPassword(userid: String): String =
    getPassword("doom", "doom", userid)

  /**
   * Find a credential matching realmtype, realm, and principal.
   */
  def findCredential(
    realmtype: String,
    realm: String,
    principal: String
  ): Option[Credential] = {
    _credentials.find(c => 
      c.realmtype == realmtype &&
      c.realm == realm &&
      c.principal == principal
    )
  }

  /**
   * Add a credential.
   */
  def addCredential(credential: Credential) {
    db.execute(
      "INSERT INTO credentials (realmtype, realm, principal, password) VALUES (?, ?, ?, ?)",
      credential.realmtype,
      credential.realm,
      credential.principal,
      encrypt(credential.password)
    )
    load()
  }

  /**
   * Update credential.
   */
  def updateCredential(credential: Credential) {
    db.execute(
      "update credentials set password=? where " +
        "realmtype=? and realm=? and principal=?",
      encrypt(credential.password),
      credential.realmtype,
      credential.realm,
      credential.principal
    )
    load()
  }

  /**
   * Delete a credential.
   */
  def deleteCredential(id: String) {
    db.delete("DELETE FROM credentials where id=?", id)
    load()
  }

  /**
   * Set Doom user password.
   */
  def setDoomPassword(userid: String, password: String) {
    updateCredential(Credential("doom", "doom", userid, password))
  }

  /**
   * Create Doom User.
   */
  def createDoomUser(
    userid: String,
    password: String,
    username: String,
    email: String,
    groups: List[String]
  ) {
    db.execute(
      "insert into users (userid, username, email, enabled) values (?,?,?,?)",
      userid,
      username,
      email,
      true
    )
    for (group <- groups) {
      addGroupMember(group, userid)
    }
    val exists = db.exists(
      "select * from credentials where realmtype='doom' and principal=?",
      userid
    )
    if (exists) {
      setDoomPassword(userid, password)
    } else {
      addCredential(Credential("doom", "doom", userid, password))
    }
  }

  /**
   * Add a member to a group.
   */
  def addGroupMember(group: String, userid: String) {
    val exists = db.exists(
      "select * from groupmembers where groupname=? and userid=?",
      group,
      userid
    )
    if (!exists) {
      db.execute(
        "insert into groupmembers (groupname, userid) values (?,?)",
        group,
        userid
      )
    }
  }

  private def encrypt(cleartext : String) : String = {
    Base64.encode(Crypto.encrypt(cleartext.getBytes, EncryptionKey.key))
  }

  private def decrypt(encrypted : String) : String = {
    new String(Crypto.decrypt(Base64.decode(encrypted), EncryptionKey.key))
  }
}
