package tmaslanka.chat.actor

import akka.actor.ActorSystem

object ActorSystemFactory {
  def startActorSystem: ActorSystem = {
    ActorSystem("server")
  }
}
