package tmaslanka.chat

import java.io.File
import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.StrictLogging
import io.restassured.RestAssured.get
import tmaslanka.chat.server.RestApiTestTemplate

import scala.annotation.tailrec
import scala.sys.process._
import scala.util.{Failure, Success, Try}

class IntegrationTest extends RestApiTestTemplate with StrictLogging {


  val roundRobinPorts: Iterator[Int] = {
    var i = 0
    Iterator.continually {
      i += 1
      8080 + (i % 3)
    }
  }

  //lets do some round robin for fun
  override def restClientPort: Int = {
    val port = roundRobinPorts.next()
    logger.info(s"current client $port")
    port
  }


  var serverProcesses: Vector[Process] = Vector.empty

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    serverProcesses :+= startServer(2551, 8082)
    serverProcesses :+= startServer(2552, 8081)
    serverProcesses :+= startServer(2553, 8080)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()

    // uncomment this to be able to play a bit with cluster
    // or with database
    // TimeUnit.HOURS.sleep(1)

    serverProcesses.foreach { serverProcess =>
      logger.debug("stopping server")
      serverProcess.destroy()
    }
  }

  def startServer(akkaPort: Int, httpPort: Int): Process = {
    logger.debug("starting server")

    logger.info(s"current dir: ${new File(".").getAbsolutePath}")

    val process = s"java -Dakka.remote.netty.tcp.port=$akkaPort -Dchat.server.port=$httpPort -jar ./target/scala-2.12/chat-assembly.jar" #>
      new File(s"target/server-http-$httpPort.log") run()

    if (!process.isAlive()) {
      throw new IllegalStateException("server process not started")
    }

    logger.info("Wait for process to start")
    waitConnected(httpPort)
    logger.info("Connected")
    process
  }



  def waitConnected(port:Int): Int = {
    @tailrec
    def loop(statusCode: Try[Int] = Failure(new RuntimeException()),
             count: Int = 0): Int = {
      @inline
      def tryAgain = {
        TimeUnit.MILLISECONDS.sleep(100)
        Try(get(s"http://localhost:$port/health-check").andReturn().statusCode())
      }

      statusCode match {
        case Failure(exception) if count == 100 =>
          throw new RuntimeException("Can not connect", exception)
        case Failure(_) =>
          loop(tryAgain, count + 1)
        case Success(value) if value != 200 =>
          loop(tryAgain, count + 1)
        case Success(value) => value
      }
    }

    loop()
  }
}
