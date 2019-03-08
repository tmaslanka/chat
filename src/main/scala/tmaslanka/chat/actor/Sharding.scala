package tmaslanka.chat.actor

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.ShardRegion.ShardId
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import tmaslanka.chat.actor.Sharding.EntityEnvelope
import tmaslanka.chat.model.StringValue
import tmaslanka.chat.model.commands.{ChatCommand, ChatCommandResponse}
import tmaslanka.chat.model.domain.ChatId
import akka.util.Timeout
import tmaslanka.chat.Settings
import akka.pattern.{ask => akkaAsk}

import scala.concurrent.Future
import scala.reflect.ClassTag

object ShardsNames {
  val chat = "chat"
}

object Sharding {
  //todo better id abstraction
  case class EntityEnvelope(id: StringValue, msg: Any)

  def startSharding(settings: Settings)(implicit system: ActorSystem): ShardingProtocol = {
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

    Vector((ShardsNames.chat, Props[ChatActor])).
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

  def tell(chatId: ChatId, msg: ChatCommand)(implicit sender: ActorRef = ActorRef.noSender): Unit = {
    chatShardRegion ! EntityEnvelope(chatId, msg)
  }

  def ask(chatId: ChatId, msg: ChatCommand)(implicit sender: ActorRef = ActorRef.noSender): Future[ChatCommandResponse] = {
    (chatShardRegion ? EntityEnvelope(chatId, msg)).mapTo[ChatCommandResponse]
  }
}
