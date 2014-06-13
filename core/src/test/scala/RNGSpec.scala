import com.sungard.poc.pricingengine.generators.RNG
import com.sungard.poc.pricingengine.generators.RNG.ScalaRandom
import org.scalatest.FlatSpec

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 1/29/14
 * Time: 10:03 PM
 * To change this template use File | Settings | File Templates.
 */
class RNGSpec extends FlatSpec {
  trait RandomSetup {
      def random(num : Double) = new RNG() {
        def nextDouble: (Double, RNG) = {
          (num, this)
        }
      }
  }

  "Simple RNG operation" should "lead to reasonable values" in new RandomSetup() {
         assert(RNG.int(random(1.5))._1  == 1)
  }
}
