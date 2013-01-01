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
import ninthdrug.edit._
import scala.xml._
import scala.xml.transform._

/**
 * Command to configure logging for a domain.
 */
case class ConfigPreClassPathOffline(domain: Domain) extends Command {

  def result(): Result = {
    try {
      if (new File(preClassPathFileName).exists) {
        val configFileName = domain.domain_home + "/config/config.xml"
        val config = XML.loadFile(configFileName)
        val tweaked_config = formatConfig(transform(config))
        Command.write(configFileName, tweaked_config)
      }
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
        case e: Elem if e.label == "server" =>
          val servername = (e \ "name").text.trim
          domain.managedservers.find(_.name == servername) match {
            case Some(server) =>
              val preclasspath =
                <java-compiler-pre-class-path>{getPreClassPath(server)}</java-compiler-pre-class-path>
              e.child.indexWhere(
                _.label == "java-compiler-pre-class-path"
              ) match {
                case -1 =>
                  val index = e.child.indexWhere(_.label == "server-start")
                  e.copy(child = e.child.patch(index, preclasspath, 0))
                case n =>
                  e.copy(child = e.child.patch(n, preclasspath, 1))
              }
            case None => e
          }
        case n => n
      }
    }
    val transformer = new RuleTransformer(rule)
    transformer.transform(config).head
  }

  /**
   * Retrieve classpath from configuration file.
   */
  def getPreClassPath(server: Server): String = {
    val props = Map(
      "domain.name" -> domain.name,
      "domain.home" -> domain.domain_home,
      "java.home" -> domain.java_home,
      "server.name" -> server.name
    )
    val lines = Command.readlines(preClassPathFileName)
    interpolate(lines.map(_.trim).filter(_ != "").mkString(":"), props)
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

  private def preClassPathFileName = {
    Config.get("doom.conf_dir") + "/weblogic/" +
      domain.domaintype + "/" + domain.version + "/default.preclasspath"
  }
}
