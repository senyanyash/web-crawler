package config

import com.typesafe.config.ConfigFactory
import zio.{ZIO, ZLayer, durationLong}

object Configs {
  case class ServerConfig(port: Int)

  object ServerConfig {
    val live = ZLayer {
      ZIO.attempt(ServerConfig(ConfigFactory.load().getInt("server.port")))
    }
  }

  case class CrawlerConfig(timeout: zio.Duration, concurrentRequests: Int)

  object CrawlerConfig {

    val live = ZLayer {
      ZIO.attempt {
        val cfg = ConfigFactory.load().getConfig("crawler")
        CrawlerConfig(cfg.getLong("single-request-timeout").millis, cfg.getInt("concurrent-requests"))
      }
    }
  }
}
