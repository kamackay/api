import java.util.Collection;
import java.util.Optional;

import static kotlin.test.AssertionsKt.assertFalse;
import static kotlin.test.AssertionsKt.assertNotNull;
import static kotlin.test.AssertionsKt.assertTrue;


public class Assertions {

  public static void assertNotEmpty(final Collection<?> list, final String message) {
    assertNotNull(list, message);
    assertFalse(list.isEmpty(), message);
  }

  public static void assertPresent(final Optional<?> elective, final String message) {
    assertTrue(elective.isPresent(), message);
  }
}
