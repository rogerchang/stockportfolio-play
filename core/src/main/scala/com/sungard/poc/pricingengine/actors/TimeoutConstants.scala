package com.sungard.poc.pricingengine.actors

import akka.util.Timeout
import scala.concurrent.duration._

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 2/24/14
 * Time: 1:16 PM
 * To change this template use File | Settings | File Templates.
 */
object TimeoutConstants {
  implicit val askTimeout = Timeout(5.second)
}
