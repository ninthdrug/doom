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
import scala.xml._
import scala.xml.transform._
import ninthdrug.command._

/**
 * Command to configure logging for a domain.
 */
case class ConfigLoggingOffline(domain: Domain) extends Command {

  def result(): Result = {
    try {
      val configFileName = domain.domain_home + "/config/config.xml"
      val config = XML.loadFile(configFileName)
      val tweaked_config = formatConfig(transform(config))

      Write(configFileName, tweaked_config).result
    } catch {
      case e: Exception => Result(e)
    }
  }

  /**
   * Configure loggin.
   */
  def transform(config: Elem): Node = {
    val rule = new RewriteRule {
      override def transform(node: Node): NodeSeq = node match {
        case e: Elem if e.label == "server" =>
          val servername = (e \ "name").text.trim
          val logfilename = domain.share_dir + "/" + domain.name +
            "/logs/" + servername + ".log"
          val log =
            <log>
              <file-name>{logfilename}</file-name>
              <rotation-type>bySize</rotation-type>
              <number-of-files-limited>true</number-of-files-limited>
              <file-count>20</file-count>
              <file-min-size>50000</file-min-size>
              <rotate-log-on-startup>true</rotate-log-on-startup>
            </log>
          val logIndex = e.child.indexWhere(_.label == "log")
          if (logIndex != -1) {
            e.copy(child = e.child.patch(logIndex, log, 1))
          } else {
            val index = e.child.indexWhere(c => c.label == "name")
            e.copy(child = e.child.patch(index+1, log, 0))
          }
        case n => n
      }
    }
    val transformer = new RuleTransformer(rule)
    transformer.transform(config).head
  }

  /**
   * Format config.xml.
   */
  def formatConfig(config: Node): String = {
    val pp = new PrettyPrinter(1000, 2) {
      override protected def traverse(node: Node, pscope: NamespaceBinding, ind: Int) = node match {
        case e: Elem if e.child.size == 0 => makeBox(ind, leafTag(e))
        case _ => super.traverse(node, pscope, ind)
      }
    }

    val buf = new StringBuilder()
    buf.append("""<?xml version="1.0" encoding="UTF-8"?>""")
    buf.append("\n")
    buf.append(pp.format(config))
    buf.append("\n")
    buf.toString
  }
}
