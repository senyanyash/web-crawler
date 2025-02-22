package parser

import config.Configs.CrawlerConfig
import model.CrawlerProtocol.CrawlerResponse
import org.jsoup.Jsoup
import zio.{URIO, ZIO, durationInt}
import zio.http.{Charsets, Client, Request, URL}

object HtmlTitleParser {

  def parseTitleForUrl(url: URL): URIO[Client with CrawlerConfig, CrawlerResponse] = for {
    cfg <- ZIO.service[CrawlerConfig]
    res <- Client.batched(Request.get(url))
      .flatMap(_.body.asString(Charsets.Utf8))
      .mapError(_ => "Ошибка при запросе к сайту")
      .timeout(cfg.timeout)
      .someOrFail("Ошибка: не удалось уложиться в таймаут")
      .flatMap(Jsoup.parse(_).title match {
        case "" => ZIO.fail("Ошибка: тэг title не найден")
        case v => ZIO.succeed(v)
      }).fold(
        e => CrawlerResponse(url.toString, None, Some(e)),
        v => CrawlerResponse(url.toString, Some(v), None)
      )
  } yield res
}
