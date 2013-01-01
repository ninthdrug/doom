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

import javax.management.JMException
import javax.management.MBeanException
import javax.management.MBeanServerConnection
import javax.management.ObjectName
import javax.management.Query
import javax.management.remote.JMXConnector
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL
import javax.naming.Context
import ninthdrug.jmx.JMX
import scala.collection.mutable.ListBuffer
import weblogic.health.HealthState
import weblogic.management.runtime.ExecuteThread

class WeblogicJMX(
  host: String,
  port: Int,
  user: String,
  password: String
) {
  /**
   * Weblogic Edit connection.
   */
  lazy val edit = connect(WeblogicJMX.EDIT)

  /**
   * Weblogic Domain Runtime connection.
   */
  lazy val domain = connect(WeblogicJMX.DOMAIN)

  /**
   * Weblogic Server Runtime connection.
   */
  lazy val server = connect(WeblogicJMX.RUNTIME)

  /**
   * ServerRuntimeMBean.
   */
  lazy val serverRuntime = server.getName(
    WeblogicJMX.RUNTIME_SERVICE, "ServerRuntime"
  )

  /**
   * DomainRuntimeMBean.
   */
  lazy val domainRuntime = domain.getName(
    WeblogicJMX.DOMAIN_SERVICE, "DomainRuntime"
  )

  /**
   * ConfigManagerMBean.
   */
  lazy val configManager =
    edit.getName(WeblogicJMX.EDIT_SERVICE, "ConfigurationManager")

  /**
   * Default security realm authenticator.
   */
  lazy val auth = getDefaultAuthenticator()

  private def connect(service: String): JMX = {
    val serviceURL = new JMXServiceURL(
      "service:jmx:iiop://" + host + ":" + port + "/jndi/" + service
    )

    val map = Map(
      Context.SECURITY_PRINCIPAL -> user,
      Context.SECURITY_CREDENTIALS -> password,
      JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES -> "weblogic.management.remote"
    )
    new JMX(serviceURL, map)
  }

  /**
   * Close the jmx connection to the server.
   */
  def close() {
    edit.close()
    domain.close()
    server.close()
  }

  /**
   * Start editing.  Returns DomainMBean.
   */
  def startEdit(): ObjectName = {
    val domainConfigRoot = edit.invoke[ObjectName](
      configManager,
      "startEdit",
      new java.lang.Integer(0),
      new java.lang.Integer(60000)
    )
    if (domainConfigRoot == null) {
      throw new Exception("Someone else is editing this domain.")
    }
    domainConfigRoot
  }

  /**
   * Stop editing and abandon changes.
   */
  def stopEdit() {
    edit.invoke(configManager, "stopEdit")
  }

  /**
   * Cancel editing.  Force open editing session to be closed.
   */
  def cancelEdit() {
    edit.invoke(configManager, "cancelEdit")
  }

  /**
   * Save edits.
   */
  def save() {
    edit.invoke(configManager, "save")
  }

  /**
   * Activate pending edits.
   */
  def activate(timeout: Long = 600000): ObjectName = {
    edit.invoke(configManager, "activate", timeout)
  }

  /**
   * Returns Array of ServerRuntimes.
   */
  def getServerRuntimes(): Array[ObjectName] = {
    domain.getNameArray(WeblogicJMX.DOMAIN_SERVICE, "ServerRuntimes")
  }

  /**
   * Find ServerRuntime for servername.
   */
  def findServerRuntime(servername: String): Option[ObjectName] = {
    getServerRuntimes.find(
      s => domain.getString(s, "Name") == servername
    )
  }

  /**
   * Retrieve ServerLifeCycleRuntimeMBean.
   */
  def getServerLifeCycleRuntime(servername: String): Option[ObjectName] = {
    val serverLifeCycleRuntimes = domain.getNameArray(
      domainRuntime, "ServerLifeCycleRuntimes"
    )
    serverLifeCycleRuntimes.find {
      s => domain.getString(s, "Name") == servername
    }
  }

  /**
   * Start managed server.
   */
  def startManagedServer(servername: String, timeout: Int = 1800) {
    getServerLifeCycleRuntime(servername) match {
      case None =>
        throw new IllegalArgumentException("Bad servername argument to startManagedServer: " + servername)
      case Some(serverLifeCycle) =>
        domain.invoke[Any](serverLifeCycle, "start")

        for (n <- 0 to timeout) {
          Thread.sleep(1000L)
          findServerRuntime(servername) match {
            case Some(serverRuntime) =>
              val state = domain.getString(serverRuntime, "State")
              if (state == "RUNNING") return
            case None =>
          }
        }

        throw new ControlException("Timeout starting " + servername)
    }
  }

  /**
   * Stop managed server.
   */
  def stopManagedServer(servername: String, timeout: Int = 1800) {
    getServerLifeCycleRuntime(servername) match {
      case None =>
        throw new IllegalArgumentException("Bad servername argument to stopManagedServer: " + servername)
      case Some(serverLifeCycle) =>
        val task = domain.invoke[ObjectName](
          serverLifeCycle,
          "shutdown",
          timeout,
          true
        )

        for (n <- 0 to 2 * timeout) {
          Thread.sleep(1000L)
          val status = domain.getString(task, "Status")
          if (status == "TASK COMPLETED") {
            return
          } else if (status == "FAILED") {
            findServerRuntime(servername) match {
              case Some(serverRuntime) =>
                val state = domain.getString(serverRuntime, "State")
                if (state == "SHUTDOWN") return
              case None =>
            }
            throw new ControlException("Failure stopping " + servername)
          }
        }

        throw new ControlException("Timeout stopping " + servername)
    }
  }
  
  /**
   * Retrieve DefaultAuthenticator Mbean.
   */
  def getDefaultAuthenticator(): ObjectName = {
    val config = 
      domain.getName(WeblogicJMX.DOMAIN_SERVICE, "DomainConfiguration")
    val security = domain.getName(config, "SecurityConfiguration")
    val defaultRealm = domain.getName(security, "DefaultRealm")
    val providers = domain.getNameArray(defaultRealm, "AuthenticationProviders")
    for (provider <- providers) {
      val name = domain.getString(provider, "Name")
      if (name.endsWith("DefaultAuthenticator")) {
        return provider
      }
    }
    throw new Exception("Cannot retrieve DefaultAuthenticator")
  }

  /**
   * Create a user.
   */
  def createUser(
    user: String,
    password: String,
    description: String,
    groups: List[String]
  ) {
    if (userExists(user)) {
      domain.invoke[Any](auth, "resetUserPassword", user, password)
      domain.invoke[Any](auth, "setUserDescription", user, description)
    } else {
      domain.invoke[Any](auth, "createUser", user, password, description)
    }
    val cursor = domain.invoke[String](auth, "listMemberGroups", user)
    while (domain.invoke[Boolean](auth, "haveCurrent", cursor)) {
      val group = domain.invoke[String](auth, "getCurrentName", cursor)
      if (group != null) {
        domain.invoke[Any](auth, "removeMemberFromGroup", group, user)
      }
      domain.invoke[Any](auth, "advance", cursor)
    }
    for (group <- groups) {
      domain.invoke[Any](auth, "addMemberToGroup", group, user)
    }
  }
  
  /**
   * Remove a user.
   */
  def removeUser(user: String) {
    domain.invoke[Any](auth, "removeUser", user)
  }
  
  /**
   * Check if user exists.
   */
  def userExists(user: String): Boolean = {
    domain.invoke[Boolean](auth, "userExists", user)
  }

  /**
   * Check if user belongs to a group.
   */
  def isMember(user: String, group: String): Boolean = {
    domain.invoke[Boolean](auth, "isMember", group, user)
  }

  /**
   * Add a member to group.
   */
  def addMemberToGroup(user: String, group: String) {
    domain.invoke[Any](auth, "addMemberToGroup", group, user)
  }
  
  /**
   * Return health state of server.
   */
  def getHealth(): String = {
    try {
      val state = server.getString(serverRuntime, "State")
      val health = if (state != "RUNNING") {
        state
      } else {
        val ho = server.get[HealthState](serverRuntime, "HealthState")
        HealthState.mapToString(ho.getState)
      }
      WeblogicJMX.HEALTHSTATE(health)
    } catch {
      case e: Exception => {
        "DOWN"
      }
    }
  }

  /**
   * Return list of JMS queue states for a particular JMS server.
   */
  def getJmsQueueStates(servername: String): List[JmsQueueState] = {
    val buf = ListBuffer[JmsQueueState]()
    val scope = new ObjectName("com.bea:Type=JMSDestinationRuntime,*")
    val queues = server.queryNames(scope, null)
    for (queue <- queues) {
      val name = server.getString(queue, "Name")
      val jndi = name.split("!").toList.last.split("@").toList.last
      val messageCount = server.getLong(queue, "MessagesCurrentCount")
      val pendingCount = server.getLong(queue, "MessagesPendingCount")
      buf += JmsQueueState(name, jndi, servername, messageCount, pendingCount)
    }
    buf.toList.sortBy(_.name).reverse
  }

  /**
   * Return the message count for a queue.
   */
  def getQueueMessageCount(queue: String): Long = {
    val scope = new ObjectName("com.bea:Type=JMSDestinationRuntime,*")
    val query = Query.finalSubString(Query.attr("Name"), Query.value(queue))
    val names = server.queryNames(scope, query)
    var count = 0L
    var Pat = (""".*(^|!|@)""" + queue + """$""").r
    var found = false
    for (dest <- names) {
      val name = server.getString(dest, "Name")
      name match {
        case Pat(_) =>
          if (found) throw new AmbiguousQueueException(queue)
          count = server.getLong(dest, "MessagesCurrentCount")
          found = true
        case _ =>
      }
    }
    if (!found) {
      throw new UnknownQueueException(queue)
    } 
    count
  }

  /**
   * Return the consumer count for a queue.
   */
  def getQueueConsumerCount(queue: String): Long = {
    val scope = new ObjectName("com.bea:Type=JMSDestinationRuntime,*")
    val query = Query.finalSubString(Query.attr("Name"), Query.value(queue))
    val names = server.queryNames(scope, query)
    var count = 0L
    var Pat = (""".*(^|!|@)""" + queue + """$""").r
    var found = false
    for (dest <- names) {
      val name = server.getString(dest, "Name")
      name match {
        case Pat(_) =>
          if (found) throw new AmbiguousQueueException(queue)
          count = server.getLong(dest, "ConsumersCurrentCount")
          found = true
        case _ =>
      }
    }
    if (!found) {
      throw new UnknownQueueException(queue)
    } 
    count
  }

  /**
   * Return a tuple of the total, hogger, and stuck thread counts.
   */
  def getThreadCount(): (Int, Int, Int) = {
    val threadPool = server.getName(serverRuntime, "ThreadPoolRuntime")
    val threads = server.get[Array[ExecuteThread]](threadPool, "ExecuteThreads")
    val total = threads.length
    val hogger = threads.count(_.isHogger)
    val stuck = threads.count(_.isStuck)
    (total, hogger, stuck)
  }
}
  
object WeblogicJMX {

  def apply(
    host: String,
    port: Int,
    user: String,
    password: String
  ) = new WeblogicJMX(host, port, user, password)

  val EDIT = "weblogic.management.mbeanservers.edit"
  val DOMAIN = "weblogic.management.mbeanservers.domainruntime"
  val RUNTIME = "weblogic.management.mbeanservers.runtime"

  val DOMAIN_SERVICE = new ObjectName("com.bea:Name=DomainRuntimeService,Type=weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean")

  val RUNTIME_SERVICE = new ObjectName("com.bea:Name=RuntimeService,Type=weblogic.management.mbeanservers.runtime.RuntimeServiceMBean")

  val EDIT_SERVICE = new ObjectName("com.bea:Name=EditService,Type=weblogic.management.mbeanservers.edit.EditServiceMBean")

  val HEALTHSTATE = Map(
    "ADMIN" -> "ADMIN",
    "FAILED_MIGRATABLE" -> "FAILED",
    "FAILED_NOT_RESTARTABLE" -> "FAILED",
    "FAILED_RESTARTING" -> "STARTING",
    "FORCE_SHUTTING_DOWN" -> "DYING",
    "HEALTH_CRITICAL" -> "CRITICAL",
    "HEALTH_FAILED" -> "FAILED",
    "HEALTH_OK" -> "OK",
    "HEALTH_OVERLOADED" -> "OVERLOADED",
    "HEALTH_WARN" -> "WARN",
    "RESUMING" -> "RESUMING",
    "RUNNING" -> "RUNNING",
    "SHUTDOWN" -> "DOWN",
    "SHUTTING_DOWN" -> "STOPPING",
    "STANDBY" -> "STANDBY",
    "STARTING" -> "STARTING",
    "SUSPENDING" -> "SUSPENDING",
    "UNKNOWN" -> "UNKNOWN"
  ).withDefault(x => x)
}
