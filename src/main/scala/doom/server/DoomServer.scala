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

import java.net.InetAddress
import ninthdrug.http.WebServer
import ninthdrug.http.ServerConfig
import ninthdrug.http.SiteConfig
import ninthdrug.config.Config
import ninthdrug.logging.Logger

object DoomServer {
  private val log = Logger("doom.server.DoomServer")
  def main(args: Array[String]) = {
    LoggerManager.start()
    ControlManager.start()
    ServerHealthCache.start()

    val threads = for {
      r <- Config.all("doom.server.runnable").toList
      val runnable = (Class.forName(r).newInstance).asInstanceOf[Runnable]
      val thread = new Thread(runnable)
    } yield thread
    threads foreach (t => t.start)

    val host = Config.get("doom.server.host", "")
    val port = Config.getInt("doom.server.port", 9999)
    val web_dir = Config.get("doom.server.web_dir")
    log.info("host: " + host)
    log.info("port: " + port)
    log.info("web_dir: " + web_dir)
    val config = 
      ServerConfig(
        Config.get("ninthdrug.http.scriptcache"),
        List(
          SiteConfig("doom", host, port, "/doom", web_dir)
        )
      )
    val server = new WebServer(config)
    server.run()
  }
}
