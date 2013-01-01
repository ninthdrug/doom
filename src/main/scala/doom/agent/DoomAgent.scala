package doom.agent

import doom._
import java.io._
import java.net._
import scala.actors.Actor
import scala.actors.Actor._

case object Done

class DoomAgent() {
  val hostname = InetAddress.getLocalHost.getHostName.split('.')(0) 
  println("starting doom agent on " + hostname) 
  private val port = Config.getInt("doom.agent.port")
  private val permittedhosts: Set[String] = {
    Set("localhost", hostname) ++ Config.all("doom.agent.permittedhost")
  }
  println("port: " + port)
  println("permitted hosts: " + permittedhosts)
  @volatile private var connections = List[Connection]()

  def start() {
    for (name <- Config.all("doom.agent.runnable")) {
      val clazz = Class.forName(name)
      val runnable = clazz.newInstance().asInstanceOf[Runnable]
      val thread = new Thread(runnable)
      thread.start()
    }
    val serverSocket = new ServerSocket(port)
    while(true) {
      val socket = serverSocket.accept()
      val hostname = socket.getInetAddress.getHostName
      println("connection from " + hostname)
      if (permittedhosts.contains(hostname)) {
        val connection = new Connection(socket)
        connections = connection :: connections
        connection.start()
      } else {
        println("refuse connection from " + hostname)
        socket.close()
      }
    }
  }

  class Connection(val socket : Socket) extends Actor {
    val out = new OutChannel(this, socket.getOutputStream)
    val in = new InChannel(this, socket.getInputStream)
    def act() {
      out.start()
      in.start()

      var done = false
      while (!done) {
        receive {
          case Done => done = true
          case s : String => {
            out ! s
          }
          case cmd : Command => {
            var result = Result()
            try {
              result = cmd.result()
            } catch {
              case e : Exception => {
                result = Result(e)
              }
            }
            out ! result
          }
        }
      }
      socket.close()
      connections = connections.filterNot(_ == this)
      exit()
    }
  }

  class InChannel(
    val connection: Connection,
    val stream: InputStream
  ) extends Actor {
    def act() {
      var done = false
      var reader : ObjectInputStream = null
      try {
        reader = new ObjectInputStream(stream)
        while(!done) {
          val obj = reader.readObject()
          println("got: " + obj)
          obj match {
            case null => done = true
            case obj => {
              connection ! obj
            }
          }
        }
      } catch {
        case e : Exception =>
          // e.printStackTrace
      } finally {
        try {
          if (reader != null) {
            reader.close()
          }
        } catch {
          case e : Exception =>
        }
        connection ! Done
        exit()
      }
    }
  }

  class OutChannel(val connection : Connection, val stream : OutputStream) extends Actor {

    def act() {
      var writer : ObjectOutputStream = null
      try {
        writer = new ObjectOutputStream(stream)
        while(true) {
          receive {
            case obj => {
              writer.writeObject(obj)
              writer.flush()
            }
          }
        }
      } catch {
        case e : Exception => {
        }
      } finally {
        try {
          if (writer != null) {
            writer.close()
          }
        } catch {
          case e : Exception =>
        }
        connection ! Done
        exit()
      }
    }
  }
}

object DoomAgent {
  def main(args : Array[String]) {
    val agent = new DoomAgent()
    agent.start()
  }
}
