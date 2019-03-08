package tmaslanka.chat.actor

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.ShardRegion.ShardId
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.pattern.{ask => akkaAsk}
import akka.util.Timeout
import tmaslanka.chat.Settings
import tmaslanka.chat.actor.Sharding.EntityEnvelope
import tmaslanka.chat.model.StringValue
import tmaslanka.chat.model.commands._
import tmaslanka.chat.model.domain.ChatId

import scala.concurrent.Future
import scala.reflect.ClassTag

object ShardsNames {
  val chat = "chat"
}

object Sharding {

  //todo better id abstraction
  case class EntityEnvelope(id: StringValue, msg: Any)

  def startSharding(settings: Settings, chatActorCreator: () => ChatActor)
                   (implicit system: ActorSystem): ShardingProtocol = {

    val extractEntityId: ShardRegion.ExtractEntityId = {
      case EntityEnvelope(id, msg) => (id.value, msg)
    }

    def extractShardId[IdType: ClassTag]: ShardRegion.ExtractShardId = {
      case EntityEnvelope(id, _) => entityIdToShardId(id.value)
      case ShardRegion.StartEntity(id) => entityIdToShardId(id)
    }

    def entityIdToShardId(id: String): ShardId = {
      math.abs(id.hashCode % settings.numberOfShards).toString
    }

    Vector((ShardsNames.chat, Props(chatActorCreator()))).
      foreach { case (shardType, props) =>
        ClusterSharding(system).start(
          typeName = shardType,
          entityProps = props,
          settings = ClusterShardingSettings(system),
          extractEntityId = extractEntityId,
          extractShardId = extractShardId)
      }

    implicit val askTimeout: Timeout = settings.askTimeout
    new ShardingProtocol
  }
}

class ShardingProtocol(implicit system: ActorSystem, askTimeout: Timeout) {
  val chatShardRegion: ActorRef =  ClusterSharding(system).shardRegion(ShardsNames.chat)

  def command(chatId: ChatId, msg: ChatCommand)(implicit sender: ActorRef = ActorRef.noSender): Future[ChatCommandResponse] = {
    askChat(chatId, msg).mapTo[ChatCommandResponse]
  }

  def query(chatId: ChatId, query: ChatQuery)(implicit sender: ActorRef = ActorRef.noSender): Future[ChatQueryResponse] = {
    askChat(chatId, query).mapTo[ChatQueryResponse]
  }

  private def askChat(chatId: ChatId, msg: Any)(implicit sender: ActorRef) = {
    chatShardRegion ? EntityEnvelope(chatId, msg)
  }

}
