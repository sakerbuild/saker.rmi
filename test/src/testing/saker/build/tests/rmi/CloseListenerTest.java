package testing.saker.build.tests.rmi;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMIServer;
import saker.rmi.connection.RMITestUtil;
import saker.util.thread.ThreadUtils;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class CloseListenerTest extends SakerTestCase {

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		testSimple();
		testServer();
	}

	private static void testServer() throws Throwable {
		AtomicInteger serverclosecounter = new AtomicInteger();
		try (RMIServer server = new RMIServer() {

			@Override
			protected void setupConnection(Socket acceptedsocket, RMIConnection connection)
					throws IOException, RuntimeException {
				connection.addCloseListener(() -> {
					int nval = serverclosecounter.incrementAndGet();
					System.out
							.println("CloseListenerTest.testServer().new RMIServer() {...}.setupConnection() " + nval);
				});
				super.setupConnection(acceptedsocket, connection);
			}
		}) {
			ThreadUtils.startDaemonThread(() -> {
				server.acceptConnections();
			});
			new RMIOptions().connect(server.getLocalSocketAddress()).closeWait();

			new RMIOptions().connect(server.getLocalSocketAddress()).closeWait();
			new RMIOptions().connect(server.getLocalSocketAddress()).closeWait();

			RMIConnection c = new RMIOptions().connect(server.getLocalSocketAddress());
			c.addCloseListener(() -> {
				int nval = serverclosecounter.getAndIncrement();
				System.out.println("CloseListenerTest.testServer() conn-listener " + nval);
			});
			c.closeWait();

			//close wait on the server, so all listeners are called before we check the value below
			server.closeWait();
		}
		assertEquals(serverclosecounter.getAndSet(0), 5);

	}

	private static void testSimple() throws Throwable {
		RMIConnection[] connections = RMITestUtil.createPipedConnection(new RMIOptions());
		RMIConnection clientConnection = connections[0];
		RMIConnection serverConnection = connections[1];
		AtomicBoolean clientclose = new AtomicBoolean();
		AtomicBoolean serverclose = new AtomicBoolean();
		clientConnection.addCloseListener(() -> clientclose.set(true));
		serverConnection.addCloseListener(() -> serverclose.set(true));
		clientConnection.closeWait();
		assertTrue(clientclose.get());

		serverConnection.closeWait();
		assertTrue(serverclose.get());
	}

}
