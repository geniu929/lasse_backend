package org.openapitools.server.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.http.scaladsl.unmarshalling.FromStringUnmarshaller
import org.openapitools.server.AkkaHttpHelper._
import org.openapitools.server.model.CreerClients
import org.openapitools.server.model.Vente


class DefaultApi(
    defaultService: DefaultApiService,
    defaultMarshaller: DefaultApiMarshaller
) {

  
  import defaultMarshaller._

  lazy val route: Route =
    path("clients" / Segment / IntNumber) { (consommation, nombre) => 
      post {  
            defaultService.clientsConsommationNombrePost(consommation = consommation, nombre = nombre)
      }
    } ~
    path("clients") { 
      post {  
            entity(as[CreerClients]){ creerClients =>
              defaultService.creerClients(creerClients = creerClients)
            }
      }
    } ~
    path("livraisons" / "stats" / Segment) { (consommation) => 
      get {  
            defaultService.livraisonsStatsConsommationGet(consommation = consommation)
      }
    } ~
    path("livraisons" / "stats") { 
      get {  
            defaultService.livraisonsStatsGet()
      }
    }
}


trait DefaultApiService {

  def clientsConsommationNombrePost200: Route =
    complete((200, "Clients créés."))
  def clientsConsommationNombrePost400: Route =
    complete((400, "Les clients n&#39;ont pas été créés."))
  /**
   * Code: 200, Message: Clients créés.
   * Code: 400, Message: Les clients n&#39;ont pas été créés.
   */
  def clientsConsommationNombrePost(consommation: String, nombre: Int): Route

  def creerClients200: Route =
    complete((200, "Clients créés."))
  def creerClients400: Route =
    complete((400, "Les clients n&#39;ont pas été créés."))
  /**
   * Code: 200, Message: Clients créés.
   * Code: 400, Message: Les clients n&#39;ont pas été créés.
   */
  def creerClients(creerClients: CreerClients): Route

  def livraisonsStatsConsommationGet200(responseVente: Vente)(implicit toEntityMarshallerVente: ToEntityMarshaller[Vente]): Route =
    complete((200, responseVente))
  /**
   * Code: 200, Message: Le nom de la consommation, la quantité et le produit de ses ventes, DataType: Vente
   */
  def livraisonsStatsConsommationGet(consommation: String)
      (implicit toEntityMarshallerVente: ToEntityMarshaller[Vente]): Route

  def livraisonsStatsGet200(responseVentearray: Seq[Vente])(implicit toEntityMarshallerVentearray: ToEntityMarshaller[Seq[Vente]]): Route =
    complete((200, responseVentearray))
  /**
   * Code: 200, Message: Une liste de noms de consommations servies avec la quantité et le produit des ventes, DataType: Seq[Vente]
   */
  def livraisonsStatsGet()
      (implicit toEntityMarshallerVentearray: ToEntityMarshaller[Seq[Vente]]): Route

}

trait DefaultApiMarshaller {
  implicit def fromEntityUnmarshallerCreerClients: FromEntityUnmarshaller[CreerClients]



  implicit def toEntityMarshallerVentearray: ToEntityMarshaller[Seq[Vente]]

  implicit def toEntityMarshallerVente: ToEntityMarshaller[Vente]

}

