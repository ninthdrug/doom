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
import ninthdrug.logging.Logger

/**
 * Command to create a weblogic domain.
 */
case class RecreateDomain(
  domain: Domain,
  password: String,
  datasources: List[Datasource] = List[Datasource]()
) extends Command {
  private val log = Logger("doom.RecreateDomain")

  def result(): Result = {
    val dir = new File(domain.domain_home)
    if (!dir.exists()) {
      Result(1, "Domain does not exists.  Use CreateDomain to create new dowmain.")
    } else {
      val domainhome = domain.domain_home
      val backup_domainhome = domainhome + "_bak"
      val adminserver = domain.adminserver

      val machinenames = domain.machinenames filter {
        m => m != doom.HOSTNAME && m != doom.HOST && m != "localhost"
      }

      val result = Chain(
        Sh("mv " + domainhome + " " + backup_domainhome),
        Chain(machinenames map {
          m => Ssh("mv " + domainhome + " " + backup_domainhome, m)
        }),
        CreateDomainOffline(domain, password, datasources),
        Sh("rsync -av " + backup_domainhome + "/security  " + domainhome),
        Sh(
          "rsync -av " + backup_domainhome + "/servers/" +
          adminserver.name + "/security/boot.properties " +
          domainhome + "/servers/" + adminserver.name + "/security/"
        )
      ).result
      if (!result) {
        log.error("Error creating " + domain.name + " offline.")
        result
      } else {
        val config = XML.loadFile(domainhome + "/config/config.xml")
        val oldConfig = XML.loadFile(backup_domainhome + "/config/config.xml")
        val credential = (config \ "security-configuration" \ "credential-encrypted").text
        val nm_password = (config \ "security-configuration" \ "node-manager-password-encrypted").text
        val ldap_credential = (config \ "embedded-ldap" \ "credential-encrypted").text

        val old_credential = (oldConfig \ "security-configuration" \ "credential-encrypted").text
        val old_nm_password = (oldConfig \ "security-configuration" \ "node-manager-password-encrypted").text
        val old_ldap_credential = (oldConfig \ "embedded-ldap" \ "credential-encrypted").text
        EditFile(
          domainhome + "/config/config.xml",
          Replace(credential, old_credential),
          Replace(ldap_credential, old_ldap_credential),
          Replace(nm_password, old_nm_password)
        ).result
        val jdbcFileName = domainhome + "/config/jdbc/wlsbjmsrpDataSource-jdbc.xml"
        val jdbcFile = new File(jdbcFileName)
        if (jdbcFile.exists()) {
          val ds = XML.loadFile(jdbcFileName)
          val ds_password = (ds \ "jdbc-driver-params" \ "password-encrypted").text
          val db_password = datasources(0).password
          val command = domain.java_home + "/bin/java -cp " +
            domain.wl_home + "/server/lib/weblogic.jar" +
            " -Dweblogic.RootDirectory=" + domain.domain_home +
            " -Dweblogic.management.allowPasswordEcho=true" +
            " weblogic.security.Encrypt " + "'" + db_password + "'"
          val encrypt_result = Sh(command).result
          if (encrypt_result.toBoolean) {
            val encrypted_db_password = encrypt_result.output.trim
            EditFile(
              jdbcFileName,
              Replace(ds_password, encrypted_db_password)
            ).result
          }
        }
        Chain(
          InitDomainOffline(domain),
          StartAdminServer(domain),
          KillAdminServer(domain),
          Chain(machinenames map {
            m => Put(domain.domain_home, m, null, domain.domains_dir)
          }),
          Chain(machinenames map {
            m => new Lazarus(CreateNodeManager(domain, m), m, null)
          }),
          StartAdminServer(domain),
          Chain(machinenames map {
            m => new Lazarus(EnrollNodeManager(domain, m, "weblogic", password), m, null)
          }),
          KillAdminServer(domain),
          Echo("recreated " + domain.name)
        ).result
      }
    }
  }
}
