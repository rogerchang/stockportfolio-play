import com.sungard.poc.pricingengine.random.{RandomGaussianGenerator, GaussianGenerator}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.FlatSpec
import org.scalacheck.Prop.{forAll, BooleanOperators, all}

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 1/6/14
 * Time: 4:00 PM
 * To change this template use File | Settings | File Templates.
 */
class GaussianGeneratorSpec extends FlatSpec {
  private val TOLERANCE = 0.1

  "Small Gaussian set" should "have at least one value within 1 std dev to expected mean" in {
      val numTestGenerator = Gen.choose(10, 20)
      val numMeanGenerator = Gen.choose(-1000.0, 1000.0)
      val numStdDevGenerator = Gen.choose(0.0, 1000.0)

      val genValProp = forAll(numTestGenerator,
                               numMeanGenerator, numStdDevGenerator) {
        (numVals : Int, mean : Double, stdDev : Double) => {
          val generator = RandomGaussianGenerator()
          val gaussianStream = generator.normalizedGaussianStream(mean, stdDev)
          val inspectedVals = gaussianStream.take(numVals).toList
          inspectedVals.exists(nextVal => (Math.abs(mean - nextVal) < stdDev))
        }
      }
    genValProp.check
  }

  "Gaussian generated values" should "have expected values for large sample sets" in {
      val numTestGenerator = Gen.choose(200000,400000)
      val numMeanGenerator = Gen.choose(-1000.0, 1000.0)
      val numStdDevGenerator = Gen.choose(0.0, 1000.0)
      val genValProp = forAll(numTestGenerator, Arbitrary.arbitrary[Long],
                  numMeanGenerator, numStdDevGenerator) {
        (numVals : Int, seed : Long, mean : Double, stdDev : Double) =>
          //(numVals >= 50 && numVals <= 1000) ==>
        {
             val generator = RandomGaussianGenerator()
             val gaussianStream = generator.normalizedGaussianStream(mean, stdDev)
             val inspectedVals = gaussianStream.take(numVals).toList
             val observedMean = inspectedVals.sum / inspectedVals.size
             val numValsInOneStdDev = inspectedVals.count(nextVal => nextVal >= (mean - stdDev) && nextVal <= (mean + stdDev))

             (((observedMean - mean) / mean).abs < TOLERANCE) :| "observed mean of " + observedMean + " close to expected mean of " + mean &&
              (numValsInOneStdDev > (0.6 * numVals)) :| "expected number of values fall within expected range, of " + numVals + " values, " + numValsInOneStdDev + " values fell into 1 std dev" &&
             (inspectedVals.count(nextVal => nextVal > (mean + stdDev)) < (0.6 * numVals)) :| "not too many values fall outside expected range" &&
             (inspectedVals.count(nextVal => nextVal > (mean + 3 * stdDev)) < (0.05 * numVals)) :| "not too many values fall more than 3 standard devs away"
        }
      }

      genValProp.check
  }
}
