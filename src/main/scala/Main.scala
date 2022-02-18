import akka.actor.typed.{ActorSystem, ActorRef}
import akka.actor.typed.scaladsl.Behaviors

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
// for JSON serialization/deserialization following dependency is required:
// "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.io.StdIn

import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import org.openapitools.server.api._
import org.openapitools.server.model._

import akka.actor.typed.scaladsl.AskPattern._

import akka.actor.typed.Behavior
import fr.mipn.bar.BarWebMain
import fr.mipn.bar.BarWebMain._
import spray.json.RootJsonFormat

object Main extends App {

  // needed to run the route
  implicit val system = ActorSystem(BarWebMain(), "the-online-bar")
  // implicit val materializer = ActorMaterializer()
  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext = system.executionContext

  val barWeb: ActorRef[BarWebMain.Command] = system
  barWeb ! Start

  object DefaultMarshaller extends DefaultApiMarshaller {
    
    def toEntityMarshallerVente: ToEntityMarshaller[Vente] = jsonFormat3(Vente)
    def toEntityMarshallerVentearray: ToEntityMarshaller[Seq[Vente]] = immSeqFormat(jsonFormat3(Vente))
    def creerClientsFormat: RootJsonFormat[CreerClients] = jsonFormat2(CreerClients)
    def fromEntityUnmarshallerCreerClients: FromEntityUnmarshaller[CreerClients] = creerClientsFormat
  }

  object DefaultService extends DefaultApiService {
    def livraisonsStatsConsommationGet(consommation: String)
      (implicit toEntityMarshallerVente: ToEntityMarshaller[Vente]): Route = {
      /* Plutôt que de répondre directement comme ceci :
       livraisonsStatsConsommationGet200(Vente(consommation, 2, 500))
       On simule une réponse du future */
      implicit val timeout: Timeout = 3.seconds
      val futurStats: Future[Vente] = 
        barWeb.ask(ref => GetStats(consommation, ref))
          .mapTo[Reply]
          .map {
            case Stats(message, quantite, produit) => 
              Vente(message, quantite, produit)
          }

      /* Route  est un alias de type :
       type Route = RequestContext => Future[RouteResult]
       Si on souhaite réutiliser les réponses toutes faites préparées par OpenAPI dans
       le trait DefaultApiService en y insérant des valeurs venant du future
       (typiquement la réponse au message d'un acteur) alors il faut composer
       la future valeur avec le Future[RouteResult] à l'aide de flatMap à l'intérieur
       de la flèche (on utilise une abstraction sur la RequestContext).
       */

      requestcontext => {
        (futurStats).flatMap {
         livraisonsStatsConsommationGet200(_)(toEntityMarshallerVente)(requestcontext)
        }
      }

      /* Alternative :
       On peut également ne pas utiliser les réponses toutes prêtes proposées dans le code
       généré par OpenAPI, et manipuler directement les Futures.

       onSuccess(reponse){(vente:Vente) => complete((200,vente))}
       */
    }

    def clientsConsommationNombrePost(consommation: String, nombre: Int) = {
      implicit val timeout: Timeout = 5.seconds  
      val futurStatus: Future[Boolean] = 
        barWeb.ask(ref => PostCreerClients(consommation, nombre, ref))
          .mapTo[Reply]
          .map {
            case FailedCreation => false
            case SuccessfulCreation => true
          }

      requestcontext => {
        (futurStatus).flatMap {
          (succes: Boolean) =>
          if (succes) creerClients200(requestcontext)
          else creerClients400(requestcontext)
        }
      }
    }

    def creerClients(creerClients: CreerClients): Route = {
      creerClients400
    }

    def livraisonsStatsGet()(implicit toEntityMarshallerVentearray: ToEntityMarshaller[Seq[Vente]]) = {
      implicit val timeout: Timeout = 1.seconds  
      val futurListVentes: Future[Seq[Vente]]= barWeb.ask(ref => GetStatsList(ref))
          .mapTo[Reply]
          .map {
            case ListStats(stats) => 
              stats.toSeq.map({case Stats(message, quantite, produit) => Vente(message, quantite, produit)})
          }

      requestcontext => {
        (futurListVentes).flatMap {
          (ventes: Seq[Vente]) => livraisonsStatsGet200(ventes)(toEntityMarshallerVentearray)(requestcontext)
        }
      }
    }
  }

  val api = new DefaultApi(DefaultService, DefaultMarshaller)
  val host = "localhost"
  val port = 8080

  val bindingFuture = Http().newServerAt(host, port).bind(pathPrefix("api"){api.route})
  println(s"Server online at http://${host}:${port}/\nPress RETURN to stop...")

  bindingFuture.failed.foreach { ex =>
    println(s"${ex} Failed to bind to ${host}:${port}!")
  }

  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
