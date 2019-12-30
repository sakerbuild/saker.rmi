package testing.saker.build.tests.rmi;

import java.util.Map;

import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMIServer;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class ServerConnectRMITest extends SakerTestCase {

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		try (RMIServer server = new RMIServer()) {
			server.start();
			new RMIOptions().connect(server.getLocalSocketAddress()).close();
		}
	}

}
