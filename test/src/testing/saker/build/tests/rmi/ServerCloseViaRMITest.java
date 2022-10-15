package testing.saker.build.tests.rmi;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMIServer;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMIVariables;
import saker.util.function.ThrowingRunnable;
import saker.util.io.IOUtils;
import saker.util.io.function.IOConsumer;
import saker.util.thread.ExceptionThread;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

//The test is reproducing the scenario, when the connection is closed on one side
//via an RMI call. The response of the closeTheServer() call arrives to the client
//but the stream gets closed right away. This can cause a race condition
//in which the stream closing sequence runs before the response of the closeTheServer() 
//method call is dispatched, therefore the call will result in an exception, stating that
//the response for it was never received
@SakerTest
public class ServerCloseViaRMITest extends SakerTestCase {

	private static final String CONTEXT_VAR_NAME = "closer";

	public interface Stub {
		public void closeTheServer() throws Exception;
	}

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		try {
			//in case some other faulty test left it in an invalid state
			RMITestUtil.restoreInternalHandlers();

			final short COMMAND_METHODRESULT = 3;
			final short COMMAND_CLOSE_VARIABLES = 12;

			Semaphore methodresultsem = new Semaphore(0);

			RMITestUtil.replaceCommandHandler(COMMAND_METHODRESULT, new IOConsumer<Object[]>() {
				@Override
				public void accept(Object[] args) throws IOException {
					System.out.println("COMMAND_METHODRESULT() in");
					try {
						//a waiting here until the client side connection is closed
						//a small wait here reliably reproduces the issue and its fix
						methodresultsem.tryAcquire(1, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						//ignored
						e.printStackTrace();
					}
					System.out.println("COMMAND_METHODRESULT() call original");
					RMITestUtil.callOriginalCommandHandler(COMMAND_METHODRESULT, args);
				}
			});
			RMITestUtil.replaceCommandHandler(COMMAND_CLOSE_VARIABLES, new IOConsumer<Object[]>() {
				@Override
				public void accept(Object[] args) throws IOException {
					System.out.println("COMMAND_CLOSE_VARIABLES()");
					RMITestUtil.callOriginalCommandHandler(COMMAND_CLOSE_VARIABLES, args);
				}
			});

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
					connection.addCloseListener(() -> {
						System.out.println("server connection closed");
					});
				}
			}) {
				server.start();
				try (RMIConnection connection = options.connect(server.getLocalSocketAddress())) {
					connection.addCloseListener(() -> {
						System.out.println("client connection closed");
						methodresultsem.release();
					});
					try (RMIVariables vars = connection.newVariables()) {
						Stub s = (Stub) vars.getRemoteContextVariable(CONTEXT_VAR_NAME);
						s.closeTheServer();
						System.out.println("ServerCloseViaRMITest.runTest() closeTheServer response received");
					}
				}
			}

			joinThreadAndCheckException(servercloserthread[0]);
		} finally {
			RMITestUtil.restoreInternalHandlers();
		}
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
