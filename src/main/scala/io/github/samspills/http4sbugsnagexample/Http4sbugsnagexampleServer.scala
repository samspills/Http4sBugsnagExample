package io.github.samspills.http4sbugsnagexample

import cats.Applicative
import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import cats.implicits._
import fs2.Stream
import com.bugsnag.logback.BugsnagMarker
import com.bugsnag.Report
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.Request
import org.http4s.server.blaze.BlazeServerBuilder
import scala.concurrent.ExecutionContext.global
import com.typesafe.scalalogging.Logger

object Http4sbugsnagexampleServer {
  val logger = Logger("ROOT")
  def bugsnagAction[F[_]: Applicative](req: Request[F], err: Throwable): F[Unit] = {
    logger.error(
      new BugsnagMarker({ (report: Report) =>
          report.addToTab("request", "method", req.method.name)
          report.addToTab("request", "uri", req.uri.renderString)
          ()
        }),
        s"request ${req.method} ${req.uri} failed",
        err
    ).pure[F]
  }

  def stream[F[_]: ConcurrentEffect](implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {
    import org.http4s.server.middleware.{ Logger, ErrorAction }
    for {
      client <- BlazeClientBuilder[F](global).stream
      helloWorldAlg = HelloWorld.impl[F]
      jokeAlg = Jokes.impl[F](client)
      exampleExceptionsAlg = ExampleExceptions.impl[F]

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = (
        Http4sbugsnagexampleRoutes.helloWorldRoutes[F](helloWorldAlg) <+>
          Http4sbugsnagexampleRoutes.jokeRoutes[F](jokeAlg) <+>
          Http4sbugsnagexampleRoutes.exceptionRoutes[F](exampleExceptionsAlg)
      ).orNotFound

      // With Middlewares in place
      loggerHttpApp = Logger.httpApp(true, true)(httpApp)
      bugsnagHttpApp = ErrorAction.httpApp(loggerHttpApp, bugsnagAction[F])

      exitCode <- BlazeServerBuilder[F](global)
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(bugsnagHttpApp)
        .serve
    } yield exitCode
  }.drain
}
