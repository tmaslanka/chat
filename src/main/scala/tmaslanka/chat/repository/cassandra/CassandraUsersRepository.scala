package tmaslanka.chat.repository.cassandra

import com.datastax.driver.core.ProtocolVersion
import com.outworkers.phantom.builder.ConsistencyBound
import com.outworkers.phantom.builder.query.InsertQuery
import com.outworkers.phantom.dsl.{Ascending, CassandraConnection, ClusteringOrder, Database, KeySpace, ResultSet, Row, Table}
import com.outworkers.phantom.keys.PartitionKey
import shapeless.HList
import tmaslanka.chat.model.domain.{ChatId, User, UserId, UserName}
import tmaslanka.chat.repository.UsersRepository
import tmaslanka.chat.repository.cassandra.CustomTypes._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.implicitConversions

object CassandraKeySpace {
  val keyspace = KeySpace("chat")

  val userNameUserIdTableName = "userName_userId"
  val userIdUserNameTableName = "userId_userName"
  val userChatsTableName = "user_chats"
}

class CassandraDatabase(override val connector: CassandraConnection)
  extends Database[CassandraDatabase](connector) {

  object userNameUserIdsTable extends UserNameUserIdTable with Connector

  object userIdUserNameTable extends UserIdUserNameTable with Connector
  object userChats extends UserChatsTable with Connector

  implicit val ex: ExecutionContextExecutor = BaseCassandraTable.ex

  object usersRepository extends UsersRepository {

    override def findAll(): Future[Vector[User]] = {
      userNameUserIdsTable.findAll()
    }

    override def createUserIfNotExists(user: User): Future[Boolean] = {
      for {
        (updated, fromDB) <- userNameUserIdsTable.saveIfNotExists(user.userName, user.userId)
        _ <- userIdUserNameTable.save(fromDB.userId, fromDB.userName)
      } yield updated
    }

    override def findUser(userId: UserId): Future[Option[User]] = for {
      record <- userIdUserNameTable.find(userId)
    } yield record.map(User(userId, _))

    override def saveUserChat(userId: UserId, chatId: ChatId): Future[Unit] = userChats.save(userId, chatId)

    override def findUserChats(userId: UserId): Future[Vector[ChatId]] = userChats.findUserChats(userId)
  }
}

object BaseCassandraTable {
  import com.outworkers.phantom.dsl.context

  val ex: ExecutionContextExecutor = context
}

trait BaseCassandraTable[Tbl <: Table[Tbl, Record], Record] extends Table[Tbl, Record] {
  self: Tbl =>

  protected implicit def ex: ExecutionContextExecutor = BaseCassandraTable.ex

  implicit def toFutureUnit[A <: ResultSet](f: Future[A]): Future[Unit] = f.map(_ => ())

  def storeIfNotExists[Status <: ConsistencyBound, PS <: HList](record: Record,
                                                                storeStatement: InsertQuery[Tbl, Record, Status, PS]): Future[(Boolean, Record)] = {
    storeStatement.ifNotExists().future().map { resultSet =>
      val wasApplied = resultSet.wasApplied()
      val fromDb = if (wasApplied) {
        record
      } else {
        helper.fromRow(self, new Row(resultSet.one(), ProtocolVersion.V5))
      }
      wasApplied -> fromDb
    }
  }
}

private[cassandra] final case class UserNameUserId(userName: UserName, userId: UserId)

abstract class UserNameUserIdTable extends BaseCassandraTable[UserNameUserIdTable, UserNameUserId] {

  object userName extends Col[UserName] with PartitionKey
  object userId extends Col[UserId]

  override def tableName: String = CassandraKeySpace.userNameUserIdTableName

  def saveIfNotExists(userName: UserName, userId: UserId): Future[(Boolean, UserNameUserId)] = {
    val record = UserNameUserId(userName, userId)
    storeIfNotExists(record, store(record))
  }

  def find(userName: UserName): Future[Option[UserId]] = for {
    record <- select.where(_.userName eqs userName).one()
    userId = record.map(_.userId)
  } yield userId

  def findAll(): Future[Vector[User]] = {
    for {
      records <- select.fetch()
      users = records.map(record => User(record.userId, record.userName))
    } yield users.toVector
  }
}

private[cassandra] final case class UserIdUserName(userId: UserId, userName: UserName)

abstract class UserIdUserNameTable extends BaseCassandraTable[UserIdUserNameTable, UserIdUserName] {

  object userId extends Col[UserId] with PartitionKey
  object userName extends Col[UserName]

  override def tableName: String = CassandraKeySpace.userIdUserNameTableName

  def save(userId: UserId, userName: UserName): Future[Unit] = {
    store(UserIdUserName(userId, userName)).future()
  }

  def find(userId: UserId): Future[Option[UserName]] = for {
    record <- select.where(_.userId eqs userId).one()
    userName = record.map(_.userName)
  } yield userName
}

private[cassandra] final case class UserChats(userId: UserId, chatId: ChatId)

abstract class UserChatsTable extends BaseCassandraTable[UserChatsTable, UserChats] {

  object userId extends Col[UserId] with PartitionKey
  object chatId extends Col[ChatId] with ClusteringOrder with Ascending

  override def tableName: String = CassandraKeySpace.userChatsTableName

  def save(userId: UserId, chatId: ChatId): Future[Unit] = {
    store(UserChats(userId, chatId)).future()
  }

  def findUserChats(userId: UserId): Future[Vector[ChatId]] = {
    select.where(_.userId eqs userId).fetch().map(_.toVector.map(_.chatId))
  }
}