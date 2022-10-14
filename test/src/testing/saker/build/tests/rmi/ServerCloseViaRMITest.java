package testing.saker.build.tests.rmi;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMIServer;
import saker.rmi.connection.RMIVariables;
import saker.util.function.ThrowingRunnable;
import saker.util.io.IOUtils;
import saker.util.thread.ExceptionThread;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class ServerCloseViaRMITest extends SakerTestCase {
	private static final String CONTEXT_VAR_NAME = "closer";

	public interface Stub {
		public void closeTheServer() throws Exception;
	}

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		RMIOptions options = new RMIOptions().classLoader(ServerCloseViaRMITest.class.getClassLoader())
				.maxStreamCount(1);
		ExceptionThread[] servercloserthread = { null };
		try (RMIServer server = new RMIServerWithOptions(options) {
			@Override
			protected void setupConnection(Socket acceptedsocket, RMIConnection connection)
					throws IOException, RuntimeException {
				super.setupConnection(acceptedsocket, connection);
				RMIServerWithOptions theserver = this;
				connection.putContextVariable(CONTEXT_VAR_NAME, new Stub() {
					@Override
					public void closeTheServer() throws Exception {
						//simulate asynch closing in which case we wait for the server to clean itself up
						servercloserthread[0] = new ExceptionThread((ThrowingRunnable) () -> {
							theserver.closeWait();
						});
						servercloserthread[0].start();
					}
				});
			}
		}) {
			server.start();
			try (RMIConnection connection = options.connect(server.getLocalSocketAddress())) {
				try (RMIVariables vars = connection.newVariables()) {
					Stub s = (Stub) vars.getRemoteContextVariable(CONTEXT_VAR_NAME);
					s.closeTheServer();
				}
			}
		}

		joinThreadAndCheckException(servercloserthread[0]);
	}

	private static void joinThreadAndCheckException(ExceptionThread servercloserthread)
			throws InterruptedException, Throwable {
		if (servercloserthread == null) {
			return;
		}
		servercloserthread.join();
		Throwable closethreadexc = servercloserthread.takeException();
		if (closethreadexc != null) {
			IOUtils.addExc(closethreadexc, new RuntimeException("stacktrace"));
			throw closethreadexc;
		}
	}

}
