package actors

import akka.testkit.{TestLatch, ImplicitSender, TestKit}
import akka.actor._
import org.scalatest.{Matchers, WordSpecLike}
import com.sungard.poc.pricingengine.actors.{StockDirectoryRouterActor, StockDirectoryActor, StockDirectoryRouterActorProvider}
import com.sungard.poc.pricingengine.actors.StockDirectoryActor.{StockPricerRetrieved, GetStockPricer}
import scala.concurrent.Await
import com.sungard.poc.pricingengine.actors.StockPortfolioGeneratorActor.PortfolioGenerated
import com.sungard.poc.pricingengine.actors.StockDirectoryActor.GetStockPricer
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 3/16/14
 * Time: 10:29 PM
 * To change this template use File | Settings | File Templates.
 */
class StockDirectoryRouterActorSpec extends TestKit(ActorSystem("stockDirectoryRouterActorSpec",
  ConfigFactory.parseString("""
                               akka {
                                  actor {
                                      deployment {
                                          /StockDirectoryRouter/stockDirectoryRouter {
                                             router = consistent-hashing-pool
                                             nr-of-instances = 10
                                             virtual-nodes-factor = 10
                                          }

                                          /StockDirectoryRouter2/stockDirectoryRouter {
                                             router = consistent-hashing-pool
                                             nr-of-instances = 10
                                             virtual-nodes-factor = 10
                                          }
                                      }
                                  }
                               }
                            """)

))
with WordSpecLike with Matchers with ImplicitSender {
  object TestStockDirectoryActor {
      case object BadMessage
      case class GoodMessageReply(me : ActorRef, ticker : String)
      case object GetCount
  }

  import TestStockDirectoryActor._

  class TestStockDirectoryActor extends Actor {
    def receive : Receive = {
      case GetStockPricer(ticker) => sender ! GoodMessageReply(self, ticker)
      case _ =>
    }
  }

  class TestStockDirectoryActor2 extends Actor {
    var count = 1

    def receive : Receive = {
      case GetStockPricer(ticker) if count == 2 => throw new RuntimeException("Something bad happened")
      case GetStockPricer(ticker) => count += 1; sender ! GoodMessageReply(self, ticker)
      case GetCount => sender ! count
      case _ =>
    }
  }

  trait TestStockDirectoryRouterActorProvider extends StockDirectoryRouterActorProvider {
       override def directoryActorProps = Props(new TestStockDirectoryActor)
  }

  trait TestStockDirectoryRouterActorProvider2 extends StockDirectoryRouterActorProvider {
    override def directoryActorProps = Props(new TestStockDirectoryActor2)
  }

  def makeActor() : ActorRef = {
    system.actorOf(Props(new StockDirectoryRouterActor with TestStockDirectoryRouterActorProvider), "StockDirectoryRouter")
  }

  def makeActor2() : ActorRef = {
    system.actorOf(Props(new StockDirectoryRouterActor with TestStockDirectoryRouterActorProvider2), "StockDirectoryRouter2")
  }

  "Stock directory router actor" should {
    "consistently route to same test stock directory actor" in {


      val stockDirectoryRouterActor = makeActor()
      stockDirectoryRouterActor ! GetStockPricer("IBM")

      var testDirectoryActor : ActorRef = null
      var testDirectoryActorAAPL : ActorRef = null

      this.expectMsgPF() {
        case GoodMessageReply(directoryActor, "IBM") =>  {
          testDirectoryActor = directoryActor
        }
        case _ => fail("Unexpected message")
      }

      stockDirectoryRouterActor ! GetStockPricer("IBM")

      expectMsg(GoodMessageReply(testDirectoryActor, "IBM"))

      stockDirectoryRouterActor ! GetStockPricer("IBM")

      expectMsg(GoodMessageReply(testDirectoryActor, "IBM"))

      stockDirectoryRouterActor ! GetStockPricer("AAPL")

      this.expectMsgPF() {
        case GoodMessageReply(anotherDirectoryActor, "AAPL") =>  {
          testDirectoryActorAAPL = anotherDirectoryActor
        }
        case _ => fail("Unexpected message")
      }

      assert(testDirectoryActorAAPL != testDirectoryActor)
    }
  }


  "Stock directory router actor" should {
    "failover stock directory if it fails" in {
      implicit val timeout = Timeout(1 second)

      var testDirectoryActor : ActorRef = null
      val stockDirectoryRouterActor = makeActor2()
      stockDirectoryRouterActor ! GetStockPricer("IBM")

      this.expectMsgPF() {
        case GoodMessageReply(directoryActor, "IBM") =>  {
          testDirectoryActor = directoryActor
        }
        case _ => fail("Unexpected message")
      }

      stockDirectoryRouterActor ! GetStockPricer("IBM")
      Thread.sleep(1000)

      // This effectively indicates the actor has restarted
      assert(Await.result(testDirectoryActor ? GetCount, 1.second) === 1)
    }
  }
}
