package testing.saker.build.tests.rmi;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIConnection.IOErrorListener;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMIVariables;
import saker.util.io.ByteArrayRegion;
import saker.util.io.DataInputUnsyncByteArrayInputStream;
import saker.util.io.ResourceCloser;
import saker.util.io.function.IOFunction;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class RMIAsyncResponseNotReceivedTest extends SakerTestCase {

	private static RMIVariables variablesStreamToClose;

	public interface Stub {
		public default void asyncCall() throws Exception {
			try {
				RMITestUtil.closeRMIStreamInput(variablesStreamToClose);
			} catch (IOException e) {
				e.printStackTrace();
				throw e;
			}
		}
	}

	public static class Impl implements Stub {

	}

	@Override

	public void runTest(Map<String, String> parameters) throws Throwable {
		testViaCommandHandler();

		testByDirectStreamClose();
	}

	@SuppressWarnings("try") // unused ResourceCloser
	private void testByDirectStreamClose()
			throws Exception, InvocationTargetException, NoSuchMethodException, IOException {
		RMIOptions baseoptions = new RMIOptions().maxStreamCount(1).classLoader(getClass().getClassLoader());
		RMIConnection[] connections = RMITestUtil.createPipedConnection(baseoptions, baseoptions);
		RMIConnection clientConnection = connections[0];
		RMIConnection serverConnection = connections[1];

		clientConnection.addErrorListener(new IOErrorListener() {
			@Override
			public void onIOError(Throwable exc) {
				PrintStream errout = System.err;
				synchronized (errout) {
					errout.println("testByDirectStreamClose / Client error:");
					exc.printStackTrace(errout);
				}
			}
		});
		serverConnection.addErrorListener(new IOErrorListener() {
			@Override
			public void onIOError(Throwable exc) {
				PrintStream errout = System.err;
				synchronized (errout) {
					errout.println("testByDirectStreamClose / Server error:");
					exc.printStackTrace(errout);
				}
			}
		});

		try (ResourceCloser closer = new ResourceCloser(clientConnection::closeWait, serverConnection::closeWait);
				RMIVariables clientVariables = clientConnection.newVariables()) {
			Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
			variablesStreamToClose = clientVariables;
			RMIVariables.invokeRemoteMethodAsync(s, Stub.class.getMethod("asyncCall"));
		}
	}

	@SuppressWarnings("try") // unused ResourceCloser
	private void testViaCommandHandler()
			throws Exception, InvocationTargetException, NoSuchMethodException, IOException {
		try {
			//in case some other faulty test left it in an invalid state
			RMITestUtil.restoreInternalHandlers();

			final short COMMAND_ASYNC_RESPONSE = 32;
			RMITestUtil.replaceCommandHandler(COMMAND_ASYNC_RESPONSE, new IOFunction<Object[], Object>() {
				@Override
				public Object apply(Object[] args) throws IOException {
					//replace the input bytes so an IOException is triggered during async response reading
					Object[] nargs = args.clone();
					nargs[1] = new DataInputUnsyncByteArrayInputStream(ByteArrayRegion.EMPTY);
					return RMITestUtil.callOriginalCommandHandler(COMMAND_ASYNC_RESPONSE, nargs);
				}
			});

			RMIOptions baseoptions = new RMIOptions().maxStreamCount(1).classLoader(getClass().getClassLoader());
			RMIConnection[] connections = RMITestUtil.createPipedConnection(baseoptions, baseoptions);
			RMIConnection clientConnection = connections[0];
			RMIConnection serverConnection = connections[1];

			clientConnection.addErrorListener(new IOErrorListener() {
				@Override
				public void onIOError(Throwable exc) {
					PrintStream errout = System.err;
					synchronized (errout) {
						errout.println("testViaCommandHandler / Client error:");
						exc.printStackTrace(errout);
					}
				}
			});
			serverConnection.addErrorListener(new IOErrorListener() {
				@Override
				public void onIOError(Throwable exc) {
					PrintStream errout = System.err;
					synchronized (errout) {
						errout.println("testViaCommandHandler / Server error:");
						exc.printStackTrace(errout);
					}
				}
			});

			try (ResourceCloser closer = new ResourceCloser(clientConnection::closeWait, serverConnection::closeWait);
					RMIVariables clientVariables = clientConnection.newVariables()) {
				Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
				RMIVariables.invokeRemoteMethodAsync(s, Object.class.getMethod("toString"));
			}
		} finally {
			RMITestUtil.restoreInternalHandlers();
		}
	}

}
