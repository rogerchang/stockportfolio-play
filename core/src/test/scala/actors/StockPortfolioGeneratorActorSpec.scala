package actors

import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.actor.{ActorRef, ActorSystem}
import org.scalatest.{Matchers, WordSpecLike}
import com.sungard.poc.pricingengine.actors.StockPortfolioGeneratorActor
import com.sungard.poc.pricingengine.actors.StockPortfolioGeneratorActor.{PortfolioGenerated, GeneratePortfolio}
import com.sungard.poc.pricingengine.portfolio.StockPortfolio

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 2/6/14
 * Time: 10:40 PM
 * To change this template use File | Settings | File Templates.
 */
class StockPortfolioGeneratorActorSpec extends TestKit(ActorSystem("stockPortfolioGeneratorSpec"))
            with WordSpecLike with Matchers with ImplicitSender {
     def makeActor() : ActorRef = {
         system.actorOf(StockPortfolioGeneratorActor.props())
     }

     "Stock portfolio generator" should {
         "generate a portfolio upon request" in {
             val stockPortfolioGeneratorActor = makeActor()//TestActorRef[StockPortfolioGeneratorActor](StockPortfolioGeneratorActor.props()).underlyingActor
             stockPortfolioGeneratorActor ! GeneratePortfolio

             var rcvdPortfolio : StockPortfolio = null

             this.expectMsgPF() {
                case PortfolioGenerated(portfolio) =>  {
                    rcvdPortfolio = portfolio
                }
                case _ => fail("Unexpected message")
             }

             assert(rcvdPortfolio.portfolioElements.size > 0)

         }
     }
}
