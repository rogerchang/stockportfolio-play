package actors

import akka.testkit.{TestLatch, TestProbe, ImplicitSender, TestKit, EventFilter}
import akka.actor._
import org.scalatest.{Matchers, WordSpecLike}
import com.sungard.poc.pricingengine.actors.{StockPricerActor, StockPricerActorProvider, StockDirectoryActor, StockPortfolioValuationActor}
import com.sungard.poc.pricingengine.actors.StockDirectoryActor.{StockPricerRetrieved, GetStockPricer, DefaultStockPricerActorProvider}
import com.sungard.poc.pricingengine.portfolio.StockPortfolio
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.{PortfolioValued, GetPortfolioValuation}
import scala.concurrent.Await
import scala.concurrent.duration._
import com.sungard.poc.pricingengine.actors.StockDirectoryActor.GetStockPricer
import com.sungard.poc.pricingengine.actors.StockDirectoryActor.StockPricerRetrieved

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 3/10/14
 * Time: 9:12 PM
 * To change this template use File | Settings | File Templates.
 */
class StockDirectoryActorSpec extends TestKit(ActorSystem("stockDirectoryActorSpec"))
            with WordSpecLike with Matchers with ImplicitSender{
    var testIBM : ActorRef = null
    var testAAPL : ActorRef = null
    var ibmCnt = 0
    var aaplCnt = 0
    var testLatchIBM : TestLatch = null
    var testLatchAAPL : TestLatch = null

    object TestStockPricerActor {
       case object BadMessage

       def props = Props(new TestStockPricerActor)
    }

    import TestStockPricerActor._

    class TestStockPricerActor extends Actor {
         def receive : Receive = {
           case BadMessage => throw new RuntimeException("Something bad happened")
           case _ =>
         }
    }

    trait TestStockPricerActorProvider extends StockPricerActorProvider {
      ibmCnt = 0
      aaplCnt = 0

      def createStockPricerActor(context : ActorContext, ticker : String) : ActorRef = {
        ticker match {
          case "IBM" => {
             testIBM = context.actorOf(TestStockPricerActor.props, ticker)
             testLatchIBM.countDown()

             ibmCnt += 1
             testIBM
          }
          case "AAPL" => {
             testAAPL = context.actorOf(TestStockPricerActor.props, ticker)
             testLatchAAPL.countDown()

             aaplCnt += 1
             testAAPL
          }
        }
      }
    }

    def makeActor() : ActorRef = {
        system.actorOf(Props(new StockDirectoryActor with TestStockPricerActorProvider))
    }

  "Stock directory actor" should {
    "create a stock pricer actor once and only once for a ticker" in {

      val stockDirectoryActor = makeActor()
      testLatchIBM = TestLatch(1)
      stockDirectoryActor ! GetStockPricer("IBM")
      Await.ready(testLatchIBM, 1.second)
      expectMsg(StockPricerRetrieved("IBM", testIBM))

      stockDirectoryActor ! GetStockPricer("IBM")

      expectMsg(StockPricerRetrieved("IBM", testIBM))
      assert(ibmCnt <= 1)

      testLatchAAPL = TestLatch(1)
      stockDirectoryActor ! GetStockPricer("AAPL")
      Await.ready(testLatchAAPL, 1.second)
      expectMsg(StockPricerRetrieved("AAPL", testAAPL))
    }


    "Stock directory actor" should {
      "create a stock pricer actor again once forced to stop" in {

        val stockDirectoryActor = makeActor()

        testLatchIBM = TestLatch(1)
        stockDirectoryActor ! GetStockPricer("IBM")
        Await.ready(testLatchIBM, 1.second)
        expectMsg(StockPricerRetrieved("IBM", testIBM))
        val originalTestIBM = testIBM

        stockDirectoryActor ! GetStockPricer("IBM")
        assert(ibmCnt <= 1)

        expectMsg(StockPricerRetrieved("IBM", testIBM))
        assert(testIBM == originalTestIBM)

        testLatchAAPL = TestLatch(1)
        val originalTestAAPL = testAAPL

        stockDirectoryActor ! GetStockPricer("AAPL")
        Await.ready(testLatchAAPL, 1.second)
        expectMsg(StockPricerRetrieved("AAPL", testAAPL))

        stockDirectoryActor ! GetStockPricer("AAPL")
        stockDirectoryActor ! GetStockPricer("AAPL")

        expectMsg(StockPricerRetrieved("AAPL", testAAPL))
        expectMsg(StockPricerRetrieved("AAPL", testAAPL))

        /*
        stockDirectoryActor ! "Invalid message"
        testLatchAAPL = TestLatch(1)
        stockDirectoryActor ! GetStockPricer("AAPL")
        Await.ready(testLatchAAPL, 1.second)
        expectMsg(StockPricerRetrieved("AAPL", testAAPL))
        assert(aaplCnt == 2)

        assert(originalTestAAPL != testAAPL)
        */

//        EventFilter[RuntimeException](occurrences = 1) intercept {
          watch(testAAPL)
//          stockDirectoryActor ! GetStockPricer("AAPL")
//          expectMsg(StockPricerRetrieved("AAPL", testAAPL))
          testAAPL ! BadMessage
          expectMsgPF() { case Terminated(testAAPL) => () }

          testLatchAAPL = TestLatch(1)
          stockDirectoryActor ! GetStockPricer("AAPL")
          Await.ready(testLatchAAPL, 1.second)
          expectMsg(StockPricerRetrieved("AAPL", testAAPL))
          assert(testAAPL != originalTestAAPL)

          stockDirectoryActor ! GetStockPricer("IBM")
          expectMsg(StockPricerRetrieved("IBM", testIBM))
          assert(testIBM == originalTestIBM)
//        }
      }
    }
  }
}
