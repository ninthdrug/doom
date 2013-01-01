/*
 * Copyright 2012 Trung Dinh
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
import java.util.Hashtable
import javax.jms._
import javax.naming.{Context, InitialContext}
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.xml._
import ninthdrug.logging.Logger
import ninthdrug.util.Base64

object WeblogicJMS {
  private val log = Logger("doom.WeblogicJMS")

  def dumpQueue(
    servername: String,
    queuename: String,
    filename: String
  ): (Seq[String], Option[Exception]) = {

    val server = ConfigCache.getServer(servername)
    val domain = ConfigCache.getDomain(server.domainname)
    val url = "t3://" + server.address + ":" + server.port
    val password = CredentialCache.getPassword("weblogic", domain.name, "weblogic")
    var ctx: InitialContext = null
    var connection: QueueConnection = null
    var session: QueueSession = null
    var browser: QueueBrowser = null
    val ids = ListBuffer[String]()

    try {
      val connectionFactoryName = "weblogic.jms.ConnectionFactory"
      val env = new Hashtable[String, String]
      env.put(Context.INITIAL_CONTEXT_FACTORY,
            "weblogic.jndi.WLInitialContextFactory")
      env.put(Context.PROVIDER_URL, url)
      env.put(Context.SECURITY_PRINCIPAL, "weblogic")
      env.put(Context.SECURITY_CREDENTIALS, password)
      ctx = new InitialContext(env)
      val connectionFactory = (ctx.lookup(connectionFactoryName)).asInstanceOf[QueueConnectionFactory]
      connection = connectionFactory.createQueueConnection()
      session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)
      connection.start()

      val queue = (ctx.lookup(queuename)).asInstanceOf[Queue]
      browser = session.createBrowser(queue)
      val msgs = browser.getEnumeration()
      val writer = new BufferedWriter(new FileWriter(filename))
      writer.write("""<?xml version="1.0" encoding="UTF-8"?>""")
      writer.write("\n<messages>")
      while (msgs.hasMoreElements()) {
        val msg = msgs.nextElement.asInstanceOf[Message]
        val msgid = msg.getJMSMessageID
        msg match {
          case _:ObjectMessage =>
            log.info("Ignoring object message: " + msgid + " on " + queuename)
          case _:StreamMessage =>
            log.info("Ignoring stream message: " + msgid + " on " + queuename)
          case _:TextMessage | _:BytesMessage | _:MapMessage =>
            ids += msg.getJMSMessageID
            writer.write("\n    ")
            writer.write(JMS.toXML(msg).toString)
          case _ =>
            log.info("Ignoring unknown message: " + msgid + " on " + queuename)
        }
      }
      writer.write("\n</messages>\n")
      writer.close()
      (ids.toIndexedSeq, None)
    } catch {
      case e: Exception =>
        (Seq(), Some(e))
    } finally {
      if (ctx != null) ctx.close
      connection.close
    }
  }

  def backupQueue(
    servername: String,
    queuename: String,
    filename: String
  ): (String, Option[Exception]) = {
    dumpQueue(servername, queuename, filename) match {
      case (_, Some(e)) =>
        ("ERROR: saving messages in queue " + queuename, Some(e))
      case (ids, None) =>
        deleteMessages(servername, queuename, ids) match {
          case (msg, Some(e)) =>
            (msg, Some(e))
          case (msg, None) =>
            (msg, None)
        }
    }
  }

  def deleteMessages(
    servername: String,
    queuename: String,
    ids: Seq[String]
  ): (String, Option[Exception]) = {
    val server = ConfigCache.getServer(servername)
    val domain = ConfigCache.getDomain(server.domainname)
    val url = "t3://" + server.address + ":" + server.port
    val password = CredentialCache.getPassword("weblogic", domain.name, "weblogic")
    val batch = 5
    var ctx: InitialContext = null
    var connection: QueueConnection = null
    var session: QueueSession = null
    var receiver: QueueReceiver = null
    val numMessages = ids.size
    log.debug("numMessages: " + numMessages)
    try {
      val connectionFactoryName = "weblogic.jms.ConnectionFactory"
      val env = new Hashtable[String, String]
      env.put(Context.INITIAL_CONTEXT_FACTORY,
        "weblogic.jndi.WLInitialContextFactory")
      env.put(Context.PROVIDER_URL, url)
      env.put(Context.SECURITY_PRINCIPAL, "weblogic")
      env.put(Context.SECURITY_CREDENTIALS, password)
      ctx = new InitialContext(env)
      val connectionFactory = (ctx.lookup(connectionFactoryName)).asInstanceOf[QueueConnectionFactory]
      connection = connectionFactory.createQueueConnection()
      session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE)
      val queue = (ctx.lookup(queuename)).asInstanceOf[Queue]
      connection.start()
      for (i <- 0 until numMessages by batch) {
        val last = math.min(i + batch, numMessages)
        val someIds = (i until last) map (n => ids(n))
        val selector = (someIds map (id => "JMSMessageID='" + id + "'")).mkString(" OR ")
        receiver = session.createReceiver(queue, selector)
        for (j <- i until last) {
          receiver.receiveNoWait
        }
        receiver.close
      }
      ("OK: " + numMessages + " in queue " + queuename, None)
    } catch {
      case e: Exception =>
        ("ERROR: deleting messages from queue " + queuename, Some(e))
    } finally {
      connection.close
    }
  }
}
