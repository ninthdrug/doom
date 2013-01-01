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

import scala.util.Random

object Auth {
  private val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "abcdefghijklmnopqrstuvwxyz" +
            "0123456789!@#$%*"
  private val SPECIAL_CHARS = "0123456789!@#$%*"
  private val rand = new Random()

  /**
   * Returns a randomly generated password.
   *
   */
  def generatePassword(): String = {
    val buf = new StringBuilder()
    for (i <- 0 to 8) {
      buf.append(CHARS.charAt(rand.nextInt(CHARS.length)))
    }
    buf.setCharAt(
      rand.nextInt(8),
      SPECIAL_CHARS.charAt(rand.nextInt(SPECIAL_CHARS.length))
    )
    return buf.toString
  }

  /**
   * Returns true if the userid exists, is enabled and the password matches.
   */
  def authenticate(userid: String, password: String): Boolean = {
    try {
      val user = ConfigCache.getUser(userid)
      user.enabled && password == CredentialCache.getDoomPassword(userid)
    } catch {
      case e: Exception =>
        false
    }
  }

  /**
   * Returns true if the password is at least 8 characters long and contains
   * at least one character which is not a letter.
   */
  def isGoodPassword(password: String): Boolean = {
    password.length >= 8 && password.exists(c => !Character.isLetter(c))
  }
}
