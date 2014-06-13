package actors

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.{ActorContext, ActorRef, Props, ActorSystem}
import org.scalatest.{Matchers, WordSpecLike}
import com.sungard.poc.pricingengine.actors.{PreInitialized, StockDataRetrieverActor, StockDataRetrieverProvider}
import com.sungard.poc.pricingengine.stock_data.{HistoricalStockPriceRow, StockData, StockDataRetriever}
import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.{StockDataRetrieved, RetrieveStockData}
import akka.routing.{Routee, Routees, GetRoutees}
import akka.remote.transport.ThrottlerTransportAdapter.Direction.Receive
import com.typesafe.config.ConfigFactory


/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 3/17/14
 * Time: 10:52 PM
 * To change this template use File | Settings | File Templates.
 */
class StockDataRetrieverActorSpec extends TestKit(ActorSystem("stockDataRetrieverActorSpec",
  ConfigFactory.parseString("""
                               akka {
                                  actor {
                                      deployment {
                                         /StockDataRetrieverRouter {
                                            router = "random"
                                             nr-of-instances=10
                                         }
                                      }
                                  }
                               }
                            """).withFallback(ConfigFactory.load())
))
    with WordSpecLike with Matchers with ImplicitSender{
  val ibmStockData = StockData("IBM", List(
    HistoricalStockPriceRow(DateTime.parse("2013-11-01"), 20.5, 31.0, 15.0, 22.5),
    HistoricalStockPriceRow(DateTime.parse("2013-12-15"), 24.5, 27.0, 21.0, 21.0),
    HistoricalStockPriceRow(DateTime.parse("2013-12-30"), 40.0, 45.0, 35.0, 37.0)))

  val aaplStockData = StockData("AAPL", List(
    HistoricalStockPriceRow(DateTime.parse("2013-11-01"), 200.0, 400.0, 150.0, 300.0)))

  object TestStockDataRetrieverActor {
      case object ReturnMe
      case class Me(actor : ActorRef)
  }

  import TestStockDataRetrieverActor._

  class TestStockDataRetrieverActor extends StockDataRetrieverActor with TestStockDataRetrieverProvider
          with PreInitialized {
      private def handleMe : Receive = {
        case ReturnMe => {
            sender ! Me(self)
        }
      }

      override def receive : Receive = super.receive orElse handleMe
  }


  trait TestStockDataRetrieverProvider extends StockDataRetrieverProvider {

    val dataRetriever: StockDataRetriever = {
          new StockDataRetriever {
            def retrieve(ticker : String, startDate : DateTime, endDate : DateTime) : StockData = {
              if ("IBM".equals(ticker))
                 ibmStockData
              else
                 aaplStockData
            }
          }
    }
  }

  def createStockDataRetrieverRouter()  = {
    StockDataRetrieverActor.createRouter(system,
      Props(new TestStockDataRetrieverActor with TestStockDataRetrieverProvider))
  }

  val router = createStockDataRetrieverRouter()

  "Stock data retriever actor" should {
    "retrieve the expected StockData" in {
            router ! RetrieveStockData("IBM")
            expectMsg(StockDataRetrieved(ibmStockData))

            router ! RetrieveStockData("AAPL")
            expectMsg(StockDataRetrieved(aaplStockData))
    }


    "route to more than one routees" in {
        router ! GetRoutees

        var theRoutees : Seq[Routee] = null
        this.expectMsgPF() {
          case Routees(routees) =>  {
            theRoutees = routees
          }
          case _ => fail("Unexpected message")
        }

        assert(theRoutees.size > 1)

        router ! ReturnMe

        theRoutees.contains(expectMsgType[Me].actor)
    }
  }

}
