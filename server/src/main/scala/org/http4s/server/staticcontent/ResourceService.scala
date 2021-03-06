package org.http4s
package server
package staticcontent

import cats.data.{Kleisli, OptionT}
import cats.effect.{Blocker, ContextShift, Sync}

object ResourceService {

  /** [[org.http4s.server.staticcontent.ResourceService]] configuration
    *
    * @param basePath prefix of the path files will be served from
    * @param blocker execution context to use when collecting content
    * @param pathPrefix prefix of the Uri that content will be served from
    * @param bufferSize size hint of internal buffers to use when serving resources
    * @param cacheStrategy strategy to use for caching purposes. Default to no caching.
    * @param preferGzipped whether to serve pre-gzipped files (with extension ".gz") if they exist
    */
  final case class Config[F[_]](
      basePath: String,
      blocker: Blocker,
      pathPrefix: String = "",
      bufferSize: Int = 50 * 1024,
      cacheStrategy: CacheStrategy[F] = NoopCacheStrategy[F],
      preferGzipped: Boolean = false)

  /** Make a new [[org.http4s.HttpRoutes]] that serves static files. */
  private[staticcontent] def apply[F[_]: Sync: ContextShift](config: Config[F]): HttpRoutes[F] =
    Kleisli {
      case request if request.pathInfo.startsWith(config.pathPrefix) =>
        StaticFile
          .fromResource(
            Uri.removeDotSegments(
              s"${config.basePath}/${getSubPath(request.pathInfo, config.pathPrefix)}"),
            config.blocker,
            Some(request),
            preferGzipped = config.preferGzipped
          )
          .semiflatMap(config.cacheStrategy.cache(request.pathInfo, _))
      case _ => OptionT.none
    }
}
