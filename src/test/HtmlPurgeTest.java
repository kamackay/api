import org.junit.Test;

import static com.keithmackay.api.utils.HtmlPurgerKt.purgeHtml;
import static com.keithmackay.api.utils.UtilsKt.threadSafeList;
import static junit.framework.TestCase.assertEquals;

public class HtmlPurgeTest {
  @Test
  public void testHtmlPurge() {
    assertEquals("<div></div>",
        purgeHtml("<div><script>ROUGE CODE</script></div>"));
    assertEquals("<div></div>",
        purgeHtml("<div><SCRIPT>ROUGE CODE</script></div>"));
    assertEquals("<div></div>",
        purgeHtml("<div><script source='ROUGE URL' /></div>"));

    assertEquals("<tag source=\"/stuff\"></tag>",
        purgeHtml("<tag source=\"http://doubleclick.net/stuff\"></tag>",
            threadSafeList("doubleclick.net")));
  }
}
