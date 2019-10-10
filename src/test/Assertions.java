import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;


public class Assertions {

  public static void assertNotEmpty(final Collection<?> list, final String message) {
    assertNotNull(list, message);
    assertFalse(list.isEmpty(), message);
  }

  public static void assertPresent(final Elective<?> elective, final String message) {
    assertTrue(elective.isPresent(), message);
  }
}
