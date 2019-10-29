
import com.keithmackay.api.utils.purgeHtml
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlPurgerTest {
  @Test
  fun testBasicScript() {
    assertEquals("<div></div>",
        purgeHtml("<div><script>ROUGE CODE</script></div>"))
    assertEquals("<div></div>",
        purgeHtml("<div><SCRIPT>ROUGE CODE</script></div>"))
    assertEquals("<div></div>",
        purgeHtml("<div><script source='ROUGE URL' /></div>"))
  }
}