package tmaslanka.chat.server

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.typesafe.scalalogging.StrictLogging
import tmaslanka.chat.Settings

import scala.concurrent.{Await, ExecutionContext, Future}

class Server(settings: Settings,
             routes: Routes)
            (implicit system: ActorSystem,
             mat: Materializer,
             ex: ExecutionContext) extends StrictLogging {

  var maybeBinding: Option[Future[Http.ServerBinding]] = None

  // @formatter:off
  val route: Route = logRequestResult(("request", Logging.InfoLevel)) {
    (path("health-check") & get) {
        complete("I am here")
    } ~
    pathPrefix("v1") {
      routes.users
    }
  }
  // @formatter:on

  def start(): Future[Http.ServerBinding] = {
    logger.info(s"Starting server ${settings.host}:${settings.port}")
    val eventualBinding = Http().bindAndHandle(route, settings.host, settings.port)
    maybeBinding = Some(eventualBinding)
    eventualBinding
  }

  def stop(): Unit = {
    maybeBinding.foreach { binding =>
      val eventualTerminated = binding.flatMap(_.terminate(settings.`stop-timeout`))
      Await.ready(eventualTerminated, settings.`stop-timeout`)
      logger.info("Server terminated")
    }
  }
}
