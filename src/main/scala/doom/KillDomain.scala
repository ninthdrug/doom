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

import java.io._
import ninthdrug.command._
import scala.util.matching._

/**
 * Command to kill all servers in a domain.
 */
class KillDomain(domain: Domain) extends Command {

  def result(): Result = {
    Chain(
      Chain(
        domain.managedservers map {
          server => Lazarus(KillManagedServer(server), server.machinename)
        }
      ),
      Chain(
        domain.machinenames map {
          machinename => Lazarus(KillNodeManager(domain), machinename)
        }
      ),
      KillAdminServer(domain)
    ).result
  }
}

object KillDomain {
  def apply(domain: Domain): Command = {
    new KillDomain(domain)
  }
}
