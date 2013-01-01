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
case class ConfigServerStartOffline(domain: Domain) extends Command {

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
   * Configure server start.
   */
  def transform(config: Elem): Node = {
    val rule = new RewriteRule {
      override def transform(node: Node): NodeSeq = node match {
        case e: Elem if e.label == "server" =>
          val servername = (e \ "name").text.trim
          domain.managedservers.find(_.name == servername) match {
            case Some(server) =>
              val serverstart =
                <server-start>
                  <java-vendor>{domain.java_vendor}</java-vendor>
                  <java-home>{domain.java_home}</java-home>
                  <class-path>{getClassPath(server)}</class-path>
                  <bea-home>{domain.mw_home}</bea-home>
                  <root-directory>{domain.domain_home}</root-directory>
                  <security-policy-file>{
                    domain.wl_home + "/server/lib/weblogic.policy"
                  }</security-policy-file>
                  <arguments>{getStartArgs(server)}</arguments>
                </server-start>
              val argsIndex = e.child.indexWhere(_.label == "server-start")
              if (argsIndex != -1) {
                e.copy(child = e.child.patch(argsIndex, serverstart, 1))
              } else {
                val index = e.child.indexWhere(
                  _.label == "jta-migratable-target"
                )
                e.copy(child = e.child.patch(index, serverstart, 0))
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
  def getClassPath(server: Server): String = {
    val props = Map(
      "domain.name" -> domain.name,
      "domain.home" -> domain.domain_home,
      "mw.home" -> domain.mw_home,
      "java.home" -> domain.java_home,
      "server.name" -> server.name,
      "alsb.home" -> domain.alsb_home,
      "osb.home" -> domain.osb_home,
      "wls.home" -> domain.wl_home
    )
    val classpathFile = Config.get("doom.conf_dir") + "/weblogic/" +
      domain.domaintype + "/" + domain.version + "/default.classpath"
    val lines = Command.readlines(classpathFile)
    interpolate(lines.map(_.trim).filter(_ != "").mkString(":"), props)
  }

  /**
   * Construct startargs.
   */
  def getStartArgs(server: Server): String = {
    val props = Map(
      "domain.name" -> domain.name,
      "domain.home" -> domain.domain_home,
      "mw.home" -> domain.mw_home,
      "java.home" -> domain.java_home,
      "server.name" -> server.name,
      "alsb.home" -> domain.alsb_home,
      "osb.home" -> domain.osb_home,
      "wls.home" -> domain.wl_home
    )
    val filename = Config.get("doom.conf_dir") + "/weblogic/" +
      domain.domaintype + "/" + domain.version + "/default.startargs"
    val lines = Command.readlines(filename)
    val default_args = interpolate(
      lines.map(_.trim).filter(_ != "").mkString(" "),
      props
    )
    val jmx_args = 
      "-Dcom.sun.management.jmxremote.port=" + server.jmxport + " " +
      "-Dcom.sun.management.jmxremote.ssl=false " +
      "-Dcom.sun.management.jmxremote.authenticate=true " +
      "-Djavax.management.builder.initial=weblogic.management.jmx.mbeanserver.WLSMBeanServerBuilder"
    val mem_args = "-Xms1024m -Xmx1024m -Xss256k -XX:MaxPermSize=512m"
    mem_args + " " + jmx_args + " " + default_args
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
