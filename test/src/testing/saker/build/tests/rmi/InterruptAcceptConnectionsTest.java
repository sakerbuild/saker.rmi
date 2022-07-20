package testing.saker.build.tests.rmi;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

import saker.rmi.connection.RMIServer;
import saker.util.thread.ExceptionThread;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class InterruptAcceptConnectionsTest extends SakerTestCase {

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		Throwable[] servererror = { null };
		try (RMIServer server = new RMIServer(null, 0, InetAddress.getLoopbackAddress()) {
			@Override
			protected void serverError(java.net.Socket socket, Throwable e) {
				servererror[0] = e;
				super.serverError(socket, e);
			}
		}) {
			Thread testthread = Thread.currentThread();
			ExceptionThread excthread = new ExceptionThread() {
				@Override
				protected void runImpl() throws Exception {
					Thread.sleep(500);
					testthread.interrupt();
				}
			};
			excthread.start();

			server.acceptConnections();
			//the accepting is interrupted, we expect an appropriate exception, that is propagated through serverError
			assertInstanceOf(servererror[0], IOException.class);

			//this shouldn't throw
			excthread.joinThrow();
		}
	}
}
