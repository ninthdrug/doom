package doom.agent

import doom.WeblogicJMX
import java.io._
import java.net._
import javax.management.ObjectName
import ninthdrug.command._
import ninthdrug.config.Config
import ninthdrug.jmx.JMX
import weblogic.health.HealthState

class PollHealth extends Runnable {

  def run() {
    val doom_home = Config.get("doom.home")
    val hostname = InetAddress.getLocalHost.getHostName.split('.')(0)
    val outputdir = new File(doom_home + "/hosts/" + hostname)
    val outputfile = new File(outputdir, "health")
    if (!outputdir.exists()) {
      Sh("mkdir -p " + outputdir.getPath).run
    }
    val password = getpassword()
    
    while (true) {
      val file = File.createTempFile("doom", ".health", outputdir)
      val writer = new BufferedWriter(new FileWriter(file))
      for (s <- Config.all("doom.agent.pollhealth")) {
        val fields = s.split(':')
        val server = fields(0)
        val host = fields(1)
        val port = fields(2).toInt
        val time = System.currentTimeMillis
        val health = pollhealth(host, port, password)
        val line = time + ":" + server + ":" + host + ":" + port + ":" + health + "\n"
        writer.write(line)
      }
      writer.close()
      file.renameTo(outputfile)
      Thread.sleep(1000L)
    }
  }

  def pollhealth(host: String, port: Int, password: String): String = {
    val jmx = JMX(host, port, "monitor", password)
    try {
      val runtime = new ObjectName(
        "com.bea:Name=RuntimeService,Type=weblogic.management.mbeanservers.runtime.RuntimeServiceMBean"
      )
      val srt = jmx.getName(runtime, "ServerRuntime")
      val state = jmx.getString(srt, "State")
      val health = if (state != "RUNNING") {
        state
      } else {
        val ho = jmx.get[HealthState](srt, "HealthState")
        HealthState.mapToString(ho.getState)
      }
      WeblogicJMX.HEALTHSTATE(health)
    } catch {
      case e: Exception => {
        e.printStackTrace
        "DOWN"
      }
    } finally {
      try {
        jmx.close()
      } catch {
        case e: Exception =>
      }
    }
  }

  private def getpassword(): String = {
    Config.get("doom.agent.weblogic.monitor.password")
  }
}
