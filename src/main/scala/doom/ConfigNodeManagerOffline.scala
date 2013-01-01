/*
 * Copyright 2008-2012 Trung Dinh
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
import scala.xml._
import scala.xml.transform._

/**
 * Command to configure logging for a domain.
 */
case class ConfigNodeManagerOffline(domain: Domain) extends Command 
  with FormatXML {

  def result(): Result = {
    try {
      val configFileName = domain.domain_home + "/config/config.xml"
      val config = XML.loadFile(configFileName)
      val tweaked_config = formatConfig(transform(config))
      Command.write(configFileName, tweaked_config)
      Result()
    } catch {
      case e: Exception => Result(e)
    }
  }

  /**
   * Configure server start.
   */
  def transform(config: Elem): Node = {
    val rule = new RewriteRule {
      override def transform(node: Node): NodeSeq = node match {
        case e: Elem if e.label == "node-manager" =>
          val nmtype = <nm-type>Plain</nm-type>
          e.child.indexWhere( _.label == "nm-type" ) match {
            case -1 =>
              val index = e.child.indexWhere(_.label == "listen-address")
              e.copy(child = e.child.patch(index, nmtype, 0))
            case n =>
              e.copy(child = e.child.patch(n, nmtype, 1))
          }
        case _ => node
      }
    }
    val transformer = new RuleTransformer(rule)
    transformer.transform(config).head
  }
}
