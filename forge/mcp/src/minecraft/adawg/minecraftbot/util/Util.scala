package adawg.minecraftbot.util

import com.google.common.util.concurrent.RateLimiter
import scala.collection.mutable.HashMap

object Util {
  def throttledCachedFunction[U](permitsPerSecond: Double)(fun: => U): Unit => U = {
    val throttle = RateLimiter.create(permitsPerSecond)
    var cache: Option[U] = None
    Unit => {
      if (throttle.tryAcquire()) {
        cache = Some(fun)
      }
      cache getOrElse {
        cache = Some(fun)
        cache.get
      }
    }
  }
  lazy val functionThrottles = new HashMap[String, RateLimiter]
  
  /**
   * Prints the time a function takes to execute
   */
  def printExecTime[U](name: String, permitsPerSecond: Double)(body: => U): U = {
    val throttle = functionThrottles.getOrElseUpdate(name, RateLimiter.create(permitsPerSecond))
    val startTime = System.nanoTime
    val result = body
    val duration = System.nanoTime - startTime
    if (throttle.tryAcquire()) {
      println(name + " took " + duration + " nanoseconds")
    }
    result
  }
}