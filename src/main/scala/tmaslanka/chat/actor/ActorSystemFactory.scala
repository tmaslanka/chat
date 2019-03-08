package tmaslanka.chat.actor

import akka.actor.ActorSystem
import akka.cluster.Cluster

object ActorSystemFactory {
  def startActorSystem: ActorSystem = {
    val system = ActorSystem("server")
    val cluster = Cluster(system)
    cluster.join(cluster.selfAddress)
    system
  }
}
