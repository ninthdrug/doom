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
import javax.jms._
import scala.collection.JavaConversions._
import scala.xml._
import ninthdrug.util.Base64

object JMS {

  private def getType(value: Any): String = {
    value match {
      case null => "null"
      case _: java.lang.Boolean   => "bool"
      case _: java.lang.Byte      => "byte"
      case _: java.lang.Character => "char"
      case _: java.lang.Short     => "short"
      case _: java.lang.Integer   => "int"
      case _: java.lang.Long      => "long"
      case _: java.lang.Float     => "float"
      case _: java.lang.Double    => "double"
      case _: java.lang.String    => "string"
      case _: Array[Byte]         => "bytes"
      case _                      => "object"
    }
  }

  private def getMessageType(message: Message): String = message match {
    case msg: TextMessage   => "TextMessage"
    case msg: BytesMessage  => "BytesMessage"
    case msg: MapMessage    => "MapMessage"
    case msg: ObjectMessage => "ObjectMessage"
    case _ => throw new JMSException("Unsupported JMS Message type.")
  }

  private def makeProperties(msg: Message): Seq[Node] = {
    val props = msg.getPropertyNames.toList
    props flatMap { p =>
      val name = p.toString
      Seq(
        Text("\n                "),
        <property name={name} type={getType(msg.getObjectProperty(name))}>{msg.getStringProperty(name)}</property>
      )
    }
  }

  private def makeBody(message: Message): Seq[Node] = {
     message match {
       case msg: TextMessage =>
         msg.getText match {
           case null => Seq(Text(""))
           case text => Seq(Text(text))
         }
       case msg: BytesMessage =>
         val size = msg.getBodyLength.toInt
         val bytes = new Array[Byte](size)
         msg.readBytes(bytes)
         Seq(Text(Base64.encode(bytes)))
       case msg: MapMessage =>
         val keys = msg.getMapNames.toList
         keys flatMap { key =>
           val name = key.toString
           Seq(
             Text("\n                "),
             <entry name={name} type={getType(msg.getObject(name))}>{msg.getString(name)}</entry>
           )
         }
       case msg: ObjectMessage =>
         val baos = new ByteArrayOutputStream()
         val out = new ObjectOutputStream(baos)
         out.writeObject(msg.getObject)
         val bytes = baos.toByteArray
         out.close()
         Seq(Text(Base64.encode(bytes)))
       case _ =>
         throw new JMSException("Unsupported message type.")
     }
  }

  def toXML(message: Message): Elem = {
    val props = message.getPropertyNames
    <message type={getMessageType(message)}>
        <header>
            <JMSMessageID>{message.getJMSMessageID.toString}</JMSMessageID>
            <JMSDeliveryMode>{message.getJMSDeliveryMode.toString}</JMSDeliveryMode>
            <JMSExpiration>{message.getJMSExpiration.toString}</JMSExpiration>
            <JMSPriority>{message.getJMSPriority.toString}</JMSPriority>
            <JMSRedelivered>{message.getJMSRedelivered.toString}</JMSRedelivered>
            <JMSTimestamp>{message.getJMSTimestamp.toString}</JMSTimestamp>
            <properties>{makeProperties(message)}
            </properties>
        </header>
        <body>{makeBody(message)}</body>
    </message>
  }

  private def fillMessageHeader(message: Message, elem: Elem) {
    val header = elem \ "header"
    for (messageId <- (header \ "JMSMessageID")) {
      message.setJMSMessageID(messageId.text)
    }
    for (deliveryMode <- (header \ "JMSDeliveryMode")) {
      message.setJMSDeliveryMode(deliveryMode.text.trim.toInt)
    }
    for (expiration <- (header \ "JMSExpiration")) {
      message.setJMSExpiration(expiration.text.trim.toLong)
    }
    for (priority <- (header \ "JMSPriority")) {
      message.setJMSPriority(priority.text.trim.toInt)
    }
    for (redelivered <- (header \ "JMSRedelivered")) {
      message.setJMSRedelivered(redelivered.text.trim.toBoolean)
    }
    for (timestamp <- (header \ "JMSTimestamp")) {
      message.setJMSTimestamp(timestamp.text.trim.toLong)
    }
    for (jmstype <- (header \ "JMSType")) {
      message.setJMSType(jmstype.text.trim)
    }
    for (property <- (header \ "properties" \ "property")) {
      val name = (property \ "@name").text.trim
      val value = property.text
      (property \ "@type").text.trim match {
        case "bool" =>
          message.setBooleanProperty(name, value.toBoolean)
        case "byte" =>
          message.setByteProperty(name, value.toByte)
        case "char" =>
          message.setObjectProperty(name, value.charAt(0))
        case "short" =>
          message.setShortProperty(name, value.toShort)
        case "int" =>
          message.setIntProperty(name, value.toInt)
        case "long" =>
          message.setLongProperty(name, value.toLong)
        case "float" =>
          message.setFloatProperty(name, value.toFloat)
        case "double" =>
          message.setDoubleProperty(name, value.toDouble)
        case "string" =>
          message.setStringProperty(name, value)
        case "bytes" =>
          message.setObjectProperty(name, Base64.decode(value.trim))
        case t =>
          throw new JMSException("Unsupported MapMessage entry type: " + t)
      }
    }
  }

  def fillTextMessage(msg: TextMessage, elem: Elem) {
    fillMessageHeader(msg, elem)
    msg.setText((elem \ "body").text)
  }

  def fillBytesMessage(msg: BytesMessage, elem: Elem) {
    fillMessageHeader(msg, elem)
    msg.writeBytes(Base64.decode((elem \ "body").text.trim))
  }

  def fillMapMessage(msg: MapMessage, elem: Elem) {
    fillMessageHeader(msg, elem)
    for (entry <- (elem \ "entry")) {
      val name = (entry \ "@name").text
      val value = (entry.text)
      (entry \ "@type").text.trim match {
        case "bool" =>
          msg.setBoolean(name, value.toBoolean)
        case "byte" =>
          msg.setByte(name, value.toByte)
        case "char" =>
          msg.setChar(name, value.charAt(0))
        case "short" =>
          msg.setShort(name, value.toShort)
        case "int" =>
          msg.setInt(name, value.toInt)
        case "long" =>
          msg.setLong(name, value.toLong)
        case "float" =>
          msg.setFloat(name, value.toFloat)
        case "double" =>
          msg.setDouble(name, value.toDouble)
        case "string" =>
          msg.setString(name, value)
        case "bytes" =>
          msg.setBytes(name, Base64.decode(value.trim))
        case t =>
          throw new JMSException("Unsupported MapMessage entry type: " + t)
      }
    }
  }

  def fillObjectMessage(msg: ObjectMessage, elem: Elem) {
    fillMessageHeader(msg, elem)
    val bytes = Base64.decode((elem \ "body").text.trim)
    val bais = new ByteArrayInputStream(bytes)
    val ois = new ObjectInputStream(bais)
    val obj = ois.readObject
    ois.close
    bais.close
    obj match {
      case ser: Serializable =>
        msg.setObject(ser)
      case _ =>
        throw new JMSException("Serialization error")
    }
  }
}
