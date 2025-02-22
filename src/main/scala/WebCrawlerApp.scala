import api.CrawlerApi
import config.Configs.{CrawlerConfig, ServerConfig}
import zio.http.{Server, ZClient}
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object WebCrawlerApp extends ZIOAppDefault {

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = (for {
    serverConfig <- ZIO.service[ServerConfig]
    _ <- Server.serve(CrawlerApi.routes)
      .provide(
        Server.defaultWithPort(serverConfig.port),
        ZClient.default,
        CrawlerConfig.live
      )
  } yield ())
    .provide(ServerConfig.live)

}
