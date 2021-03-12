package clicker2.networking

import clicker2.Game
import clicker2.networking.Database

import akka.actor.Actor

case object Update

case object ClickGold

case object Save

case object Setup

case class BuyEquipment(equipmentID: String)

class GameActor(username: String) extends Actor {

  var initializeGame = new Game(username)

  override def receive: Receive = {
    case Setup => if(Database.playerExists(username)){
      Database.loadGame(username, initializeGame)
    }else{
      Database.createPlayer(username)
    }
    case Update => initializeGame.update(System.nanoTime())
          sender() ! GameState(initializeGame.toJSON())
    case Save => Database.saveGame(username, initializeGame.gold, initializeGame.equipment("shovel").numberOwned, initializeGame.equipment("excavator").numberOwned, initializeGame.equipment("mine").numberOwned, initializeGame.lastUpdateTime)
    case ClickGold => initializeGame.clickGold()
    case buy: BuyEquipment => initializeGame.buyEquipment(buy.equipmentID)

  }
}
