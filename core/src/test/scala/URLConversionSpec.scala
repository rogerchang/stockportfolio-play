import com.sungard.poc.pricingengine.io.URLComponents
import org.scalatest.FlatSpec
import com.sungard.poc.pricingengine.io.URLConverter._

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 10/28/13
 * Time: 11:10 PM
 * To change this template use File | Settings | File Templates.
 */
class URLConversionSpec extends FlatSpec {
       "Simple GET Request" should "have normally formatted URL" in {
             val urlComponents = URLComponents(prefix="http://www.testurl.com",
                            getParams=Map(
                              "param_a" -> Seq("test1", "test2"),
                              "param_b" -> Seq("test3")))
             assert("http://www.testurl.com?param_a=test1+test2&param_b=test3" === urlComponentsToUrl(urlComponents).toString())
       }

       "Request with no parameters" should "have normally formatted URL" in {
         val urlComponents = URLComponents(prefix="http://www.testurl.com",
           getParams=Map())
         assert("http://www.testurl.com" === urlComponentsToUrl(urlComponents).toString())

       }
}
