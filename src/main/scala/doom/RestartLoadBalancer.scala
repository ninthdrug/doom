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
import ninthdrug._
import ninthdrug.command._

class RestartLoadBalancer() extends Command {

  def result(): Result = {
    Chain(
      new StopLoadBalancer(),
      new Sleep(2000L),
      new StartLoadBalancer()
    ).result
  }
}

object RestartLoadBalancer {
  def apply(): Command = {
    new RestartLoadBalancer()
  }

  def apply(server: Server): Command = {
    DoomAPI.remote(new RestartLoadBalancer(), server.address)
  }

  def apply(domain: Domain): Command = {
    Chain(
      domain.loadbalancers.map(lb =>
        DoomAPI.remote(new RestartLoadBalancer(), lb.address)
      )
    )
  }
}
