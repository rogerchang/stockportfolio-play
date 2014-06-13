import com.sungard.poc.pricingengine.io.{CsvParser, URLComponents}
import com.sungard.poc.pricingengine.io.URLConverter._
import org.scalatest.FlatSpec

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 10/30/13
 * Time: 10:08 PM
 * To change this template use File | Settings | File Templates.
 */

object CsvParserSpec {
  trait CsvParserFixture {
    val csvParser = new CsvParser {};
  }

  trait ParsedFieldsBehavior extends CsvParserFixture { this: FlatSpec =>
    def validParsingOfTheFile(iterator : => Iterator[String], expectedNumRows : Int,
                              expectedNumFields : Int, expectedVals : Map[(Int, Int), String]) = {
      it should "have correct number of rows" in {
        val lines = csvParser.parse(iterator, ",", false)
        assert(lines.size == expectedNumRows)
      }

      it should "have correct number of fields" in {
        val lines = csvParser.parse(iterator, ",", false)
        lines.foreach(line => assert (expectedNumFields === line.length))
      }

      it should "match expected values" in {
        val lines = csvParser.parse(iterator, ",", false)
        expectedVals.keys.foreach(key =>
          assert(expectedVals(key) === lines(key._1)(key._2))
        )
      }
    }
  }

}

import CsvParserSpec._

class CsvParserSpec extends FlatSpec with ParsedFieldsBehavior {

  def emptyFile = {
      List().iterator
  }

  def singleFieldSingleLineFile = {
      List("testA").iterator
  }

  def multipleFieldsSingleLine = {
     List("testA,testB,testC").iterator
  }

  def multipleFieldsMultipleLines = {
     List("testA,testB,testC","testD,testE,testF","testG,testH,testI").iterator
  }

  "An empty CSV file" should behave like  validParsingOfTheFile(emptyFile, 0, 0, Map())
  "A one line single field CSV file" should behave like  validParsingOfTheFile(singleFieldSingleLineFile, 1, 1,
                            Map((0 , 0) -> "testA"))
  "A one line multi field CSV file" should behave like  validParsingOfTheFile(multipleFieldsSingleLine, 1, 3,
    Map((0 , 2) -> "testC"))
  "A multi line multi field CSV file" should behave like  validParsingOfTheFile(multipleFieldsMultipleLines, 3, 3,
    Map((0 , 2) -> "testC", (1, 1) -> "testE"))
}
