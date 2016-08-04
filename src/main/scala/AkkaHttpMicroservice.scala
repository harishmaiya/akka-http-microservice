import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{ExecutionContextExecutor, Future}

trait Service {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config
  val logger: LoggingAdapter

  lazy val mongoApiConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection(config.getString("services.goku-mongo.host"), config.getInt("services.goku-mongo.port"))

  def mongoApiRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(mongoApiConnectionFlow).runWith(Sink.head)

  val routes = {
    logRequestResult("goku-mongo-microservice") {
      pathPrefix("update") {
        (get & path(Segment)) { ip =>
          complete {
            Mon
            BadRequest -> "ack"
          }
        } ~
        (post & entity(as[String])) { ipPairSummaryRequest =>
          complete {
            BadRequest -> "ack"
          }
        }
      }
    }
  }
}

object AkkaHttpMicroservice extends App with Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
