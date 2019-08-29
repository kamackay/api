import com.keithmackay.api.ServerModule;
import com.keithmackay.api.db.Database;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AuthTest {
  private static Database db;

  @BeforeAll
  static void setUp() {
    db = ServerModule.getInjector().getInstance(Database.class);
  }

  @Test
  void testLogin() {
//    final Elective<Document> newToken = doLogin();
//    Assertions.assertPresent(newToken, "Could not Login");
//    assertEquals("someone", newToken.get().getString("username"));
  }

//  Elective<Document> doLogin() {
////    return AuthUtils.login(db.getCollection("auth"), LoginModel.builder()
////        .username("someone")
////        .build());
//  }

  @Test
  void testReLogin() {
//    this.testLogin();
//    Assertions.assertPresent(doLogin(), "Could not re-login");
  }
}
