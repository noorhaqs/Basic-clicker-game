package clicker2.networking

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import play.api.libs.json.{JsValue, Json}



case object UpdateGames

case object AutoSave

case class GameState(gameState: String)

class ClickerServer extends Actor {

  import akka.io.Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 8000))

  var meAp: Map[String, ActorRef] = Map()

  var clients: Set[ActorRef] = Set()

  override def receive: Receive = {
    case b: Bound => println("Listening on port: " + b.localAddress.getPort)
    case c: Connected =>
      println("Client Connected: " + c.remoteAddress)
      this.clients = this.clients + sender()
      sender() ! Register(self)
    case PeerClosed =>
      println("Client Disconnected: " + sender())
      this.clients = this.clients - sender()
    case r: Received =>
      val parsed: JsValue = Json.parse(r.data.utf8String)
      val username: String = (parsed\ "username").as[String]
      val action: String = (parsed\ "action").as[String]
      if(action == "connected"){
        val childActor = context.actorOf(Props(classOf[GameActor], username))
        meAp = meAp + (username -> childActor)
      }else if(action == "disconnected"){
        meAp = meAp - username
      }else if(action == "clickGold"){
        meAp(username) ! ClickGold
      }else if(action == "buyEquipment"){
        val Purchase: String = (parsed \ "equipmentID").as[String]
        meAp(username) ! BuyEquipment(Purchase)
      }

    case UpdateGames => this.clients.foreach((client: ActorRef) => client ! Update)
    case AutoSave => this.clients.foreach((client: ActorRef) => client ! AutoSave)
    case gs: GameState =>
      val delimiter = "~"
      this.clients.foreach((client: ActorRef) => client ! Write(ByteString(gs.gameState + delimiter )))

  }

}
object ClickerServer {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem()

    import actorSystem.dispatcher

    import scala.concurrent.duration._

    val server = actorSystem.actorOf(Props(classOf[ClickerServer]))

    actorSystem.scheduler.schedule(0 milliseconds, 100 milliseconds, server, UpdateGames)
    actorSystem.scheduler.schedule(0 milliseconds, 5000 milliseconds, server, AutoSave)
  }
}