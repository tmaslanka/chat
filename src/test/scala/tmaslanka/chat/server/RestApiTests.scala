package tmaslanka.chat.server

class RestApiTests extends RestApiTestTemplate {

  val module = new ServerModule(settings)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    module.start()
  }

  override protected def afterAll(): Unit = {
    module.stop()
  }
}
