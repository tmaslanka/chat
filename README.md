### Simple chat application backend
Build using akka persistence, akka http and cassandra.

### Requirements
#### build
 * Java 8 or later
 * sbt 1.2
#### optional
 * cassandra 3.x - required for console connection to database `test-cql.sh`

### Build

Run `./build.sh`. It will create a fat jar in target directory (`target/scala-2.12/chat-assembly.jar`).
During tests cassandra database will be started (see tmaslanka.chat.Main.startCassandra).

### API
For api reference please take a look to `RestApiTestTemplate` trait. It's mixed into:
 * `RestApiTests` - tests with cassandra and http server,
 * `IntegrationTest` - tests with akka cluster (3 nodes) with test requests to sent to cluster nodes in round robin fashion.

Example request:

Get list of users
```
GET http://localhost:8080/v1/users
```

Create user
```
PUT http://localhost:8080/v1/users
{
  "userName": "Bob"
}
```

List of user chats
```
GET http://localhost:8080/v1/users/{userId}/chats
```

Create chat thread
```
PUT http://localhost:8080/v1/chats
{
  "userIds": ["userId1", "userId2"]
}
```

Add message to chat from user
```
PUT http://localhost:8080/v1/chats/{chatId}/messages
{
"message": {
    "userSeq": 0, //sequence number of message from userId
    "userId": "userId",
    "text": "first message text"
  }
}
```

Get chat messages:
```
GET http://localhost:8080/v1/chats/{chatId}/messages
```

Get short chat description
```
GET v1/chats/{chatId}
```

### Scripts
To run cluster please first `./build.sh`.
Then `cassandra-start.sh` finally `servers-start.sh`.

#### cassandra
 * cassandra-start.sh - starts cassandra (cassandra files are locate in `target/cassandra`)

##### servers
 * servers-start.sh - start akka cluster
 * servers-list.sh - list cluster processes
 * servers-kill.sh - kill cluster jvms


### Implementation notes
JavaSerializer is used (I am lazy).