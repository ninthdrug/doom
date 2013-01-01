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
import java.util.Hashtable
import javax.jms._
import javax.naming.{Context, InitialContext}
import ninthdrug.command._
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._
import scala.xml.Elem
import scala.xml.XML

/**
 * Command to backup messages in a JMS queue.
 */
case class BackupQueue(
  servername: String,
  queuename: String,
  path: String
) extends Command {

  private val server = ConfigCache.getServer(servername)
  private val domain = ConfigCache.getDomain(server.domainname)
  private val url = "t3://" + server.address + ":" + server.port
  private val password = CredentialCache.getPassword(
    "weblogic",
    domain.name,
    "weblogic"
  )

  def result(): Result = {
    var ctx: InitialContext = null
    var connection: QueueConnection = null
    var session: QueueSession = null
    var receiver: QueueReceiver = null
    var result = Result("OK")

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
      receiver = session.createReceiver(queue)
      connection.start()
      val buf = new ListBuffer[Message]
      var done = false

      while (!done) {
        val message = receiver.receiveNoWait().asInstanceOf[Message]
        if (message == null) {
          done = true
        } else {
          buf += message
        }
      }
      if (buf.size == 0) {
        result = Result(-1, "ERROR")
      } else {
        val messages = buf.toList

        val xml =
<content>
{ for (message <- messages) yield
    message2elem(message)
}
</content>
        XML.save(path, xml, "UTF-8", true, null)
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        result = Result(-1, "ERROR")
    } finally {
      if (receiver != null) {
        receiver.close()
      }
      if (session != null) {
        session.close()
      }
      if (connection != null) {
        connection.close()
      }
      if (ctx != null) {
        ctx.close()
      }
    }
    result
  }

  private def readBytes(message: BytesMessage): Array[Byte] = {
    val bytes = new Array[Byte](message.getBodyLength.toInt)
    message.readBytes(bytes)
    bytes
  }

  private def message2elem(message: Message): Elem = {
    val props = message.getPropertyNames
    <entry type="1">
        <textMessage
            JMSDeliveryMode={message.getJMSDeliveryMode.toString}
            JMSExpiration={message.getJMSExpiration.toString}
            JMSMessageID={message.getJMSMessageID.toString}
            JMSPriority={message.getJMSPriority.toString}
            JMSRedelivered={message.getJMSRedelivered.toString}
            JMSTimestamp={message.getJMSTimestamp.toString} >
        { for (prop <- props) yield
            <headerProperty name={ prop.toString } type="java.lang.String"      value={ message.getStringProperty(prop.toString) }/>
        }
            <text>{
              message match {
                case textMessage: TextMessage =>
                  textMessage.getText
                case bytesMessage: BytesMessage =>
                  new String(readBytes(bytesMessage))
                case _ =>
                  throw new RuntimeException("Unsupported Message Type")
              }
           }</text>
        </textMessage>
    </entry>
  }
}
