package parser

import config.Configs.CrawlerConfig
import model.CrawlerProtocol.CrawlerResponse
import org.jsoup.Jsoup
import zio.{URIO, ZIO}
import zio.http.{Charsets, Client, Request, URL, ZClientAspect}

object HtmlTitleParser {

  def parseTitleForUrl(url: URL): URIO[Client with CrawlerConfig, CrawlerResponse] = for {
    cfg <- ZIO.service[CrawlerConfig]
    client <- ZIO.service[Client].map(_ @@ ZClientAspect.followRedirects(5)((_, _) => ZIO.fail("Ошибка редиректа")))
    res <- client.batched(Request.get(url))
      .filterOrFail(_.status.isSuccess)("Ошибка при запросе к сайту")
      .flatMap(_.body.asString(Charsets.Utf8))
      .mapError(_ => "Ошибка: не удалось достать тело запроса")
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
