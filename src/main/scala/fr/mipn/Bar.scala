package fr.mipn.bar

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import scala.util.Random
/*
Bob avise un bar
Bob entre dans le bar
Bob hèle un serveur
le serveur demande à Bob ce qu'il veut boire
Bob dit un café

le serveur note la commande
le serveur prévient le barman
le barman prévient le serveur que la commande est prète
le serveur amène la commande à Bob

le serveur demande à Bob de régler la commande
Bob règle la commande
[ Bob avise son ami Oscar dans la rue
Bob commande deux cafés un autre pour lui et un pour son ami ]

Acteurs :
Bar
Serveurs
Consommateurs
Barmans

Client                  Serveur                           Barman                Bar
  |                        |                               |                     |
  |--------------------------------Bonjour(c)----------------------------------->|
  |<------------------VotreServeur(s)--------------------------------------------|
  |                        |                               |                     |
  |--Commande("café",c)--->|                               |                     |
  |                        |----Commande("café", c, s)---->|                     |
  |                        |<---Livraison("café", c, 150)--|                     |
  |                        |                               |                     |
  |<-Livraison("café",150)-|                               |                     |
  |--Merci(150,c)--------->|                               |                     |


Pour commencer :
- [X] Créer un bar
- [X] Créer un serveur
- [X] Créer Bob (dans le bar)
- [X] Bob dit bonjour au bar

Pour continuer :
- [X] Bob s'adresse au bar pour demander l'ActorRef d'un serveur
- [X] le bar réponds avec un ActorRef de serveur

Pour finir :
- [-] Bob passe sa commande le serveur la gère

Comment utiliser le Barman pour préparer la commande ?
*/

object Serveur {
  trait Command
  final case class Order(conso: String, client: ActorRef[Client.Command]) extends Command
  final case class Livraison(conso: String, prix: String, client: ActorRef[Client.Command]) extends Command
  final case class Merci(prix: String, client: ActorRef[Client.Command]) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup(context => new Serveur(context))
}

class Serveur(context: ActorContext[Serveur.Command]) extends AbstractBehavior[Serveur.Command](context) {
  context.log.info("Hello !")
  import Serveur._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case msg @ Order(conso, client) =>
        client ! Client.Livraison(conso, "2€")
        this
      case Merci(prix, client) =>
        context.log.info("Bonne journée {}", client)
        this
    }
}

object Barman {
  trait Command
  final case class Order(conso: String, 
    client: ActorRef[Client.Command], 
    serveur: ActorRef[Serveur.Command]) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup(context => new Barman(context))
}

class Barman(context: ActorContext[Barman.Command]) extends AbstractBehavior[Barman.Command](context) {
  context.log.info("Hello !")
  import Barman._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case msg @ Order(conso, client, serveur) =>
        serveur ! Serveur.Livraison(conso, "2€", client)
        this
    }
}

object Client {
  trait Command
  final case class VotreServeur(serveur: ActorRef[Serveur.Command]) extends Command
  final case class Livraison(conso: String, prix: String) extends Command

  def apply(bar: ActorRef[Bar.Command]): Behavior[Client.Command] =
    Behaviors.setup(context => new Client(bar, context))
}

class Client(bar: ActorRef[Bar.Command], context: ActorContext[Client.Command]) extends AbstractBehavior[Client.Command](context) {
  var monserveur: Option[ActorRef[Serveur.Command]] = None 
  context.log.info("Hello !")
  import Bar.Bonjour
  import Client._
  import Serveur.Order

  bar ! Bonjour(context.self)
 
  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case VotreServeur(serveur) =>
        monserveur = Some(serveur)
        serveur ! Order("un café", context.self)
        this
      case Livraison(conso, prix) =>
        context.log.info("{} délicieux", conso)
        monserveur.map(_ ! Serveur.Merci(prix, context.self)) 
        this
    }
}

object BarMain {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new BarMain(context))
}

class BarMain(context: ActorContext[String]) extends AbstractBehavior[String](context) {
   var serveurs = Set.empty[ActorRef[Serveur.Command]]

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "start" =>
        /* créons un bar */
        val bar = context.spawn(Bar(), "LeTurfu")
        bar ! Bar.Start
        /* créons un premier client, Bob et donnons lui l'adresse du Bar */
        val bob = context.spawn(Client(bar), "Bob")
        /* créons un million de clients */
        // Range(0,1000000).map( (i) => context.spawn(Client(bar), s"client-$i"))
        this
    }
}

object Bar {
  trait Command
  final case object Start extends Command
  final case class Bonjour(replyTo: ActorRef[Client.Command]) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup(context => new Bar(context))
}

class Bar(context: ActorContext[Bar.Command]) extends AbstractBehavior[Bar.Command](context) {
  var serveurs = Set.empty[ActorRef[Serveur.Command]]
  import Bar._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case Start =>
        /* créons des serveurs */
        serveurs = Range(1,3).map( 
          (i) => context.spawn(Serveur(), s"serveur-$i")
        ).toSet
        this
      case Bonjour(client) =>
        import Client.VotreServeur
        context.log.info("On a un client {} !", client)
        val i = Random.nextInt(serveurs.size)
        val msg = VotreServeur(serveurs.view(i, i + 1).head)
        client ! msg
        this
    }
}
object BarWebMain {
    trait Command
    case object Start extends Command
    case class GetStats(msg: String, replyTo: ActorRef[Reply]) extends Command
    case class GetStatsList(replyTo: ActorRef[Reply]) extends Command
    case class PostCreerClients(consommation: String, nombre: Int, replyTo: ActorRef[Reply]) extends Command

    trait Reply
    case class Stats(conso: String, quantite: Int, produit: Int) extends Reply
    case class ListStats(stats: List[Stats]) extends Reply
    case object FailedCreation extends Reply
    case object SuccessfulCreation extends Reply

    def apply(): Behavior[Command] =
        Behaviors.setup(context => new BarWebMain(context))
}

class BarWebMain(context: ActorContext[BarWebMain.Command]) extends AbstractBehavior[BarWebMain.Command](context) {
   var serveurs = Set.empty[ActorRef[Serveur.Command]]

   import BarWebMain._

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
    case Start =>
        /* créons un bar */
        val bar = context.spawn(Bar(), "LeTurfu")
        bar ! Bar.Start
        /* créons un premier client, Bob et donnons lui l'adresse du Bar */
        val bob = context.spawn(Client(bar), "Bob")
        /* créons un million de clients */
        Range(0,100).map( (i) => context.spawn(Client(bar), s"client-$i"))
        this
    case GetStats(msg, replyTo) =>
        replyTo ! Stats(s"$msg chaud⋅e", 2, 500)
        this
    case PostCreerClients(consommation, nombre, replyTo) => 
        replyTo ! FailedCreation
        this
    case GetStatsList(replyTo) =>
      replyTo ! ListStats(List(Stats("café", 2, 500), Stats("thé", 3, 750)))
        this
    }
}
