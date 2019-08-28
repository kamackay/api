import com.keithmackay.api.Server;
import com.keithmackay.api.ServerModule;
import org.junit.jupiter.api.BeforeAll;

public class ServerTests {

  private static Server server;

  @BeforeAll
  static void setUp() {
    server = ServerModule.getInjector().getInstance(Server.class);
    server.start();
  }


}
