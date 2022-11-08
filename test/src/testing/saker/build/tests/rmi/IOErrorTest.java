package testing.saker.build.tests.rmi;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIConnection.IOErrorListener;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMIVariables;
import saker.rmi.exception.RMIIOFailureException;
import saker.rmi.exception.RMIResourceUnavailableException;
import saker.util.io.DataInputUnsyncByteArrayInputStream;
import saker.util.io.function.IOConsumer;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class IOErrorTest extends SakerTestCase {
	public interface Stub {
	}

	public static class Impl implements Stub {

	}

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		// TODO Auto-generated method stub

		RMIOptions baseOptions = new RMIOptions().classLoader(getClass().getClassLoader());

		{
			RMIConnection[] connections = RMITestUtil.createPipedConnection(baseOptions);
			try (RMIConnection clientConnection = connections[0];
					RMIConnection serverConnection = connections[1];) {
				try (RMIVariables clientVariables = clientConnection.newVariables()) {
					checkErrorWithoutReqId(clientConnection, serverConnection, clientVariables);
				}
			}
		}
		{
			RMIConnection[] connections = RMITestUtil.createPipedConnection(baseOptions);
			try (RMIConnection clientConnection = connections[0];
					RMIConnection serverConnection = connections[1];) {
				try (RMIVariables clientVariables = clientConnection.newVariables()) {
					checkErrorWithReqId(clientConnection, serverConnection, clientVariables);
				}
			}
		}
	}

	private static void checkErrorWithReqId(RMIConnection clientConnection, RMIConnection serverConnection,
			RMIVariables clientVariables) throws InvocationTargetException, NoSuchMethodException, AssertionError {
		try {
			//in case some other faulty test left it in an invalid state
			RMITestUtil.restoreInternalHandlers();
			clientConnection.addErrorListener(new IOErrorListener() {
				@Override
				public void onIOError(Throwable exc) {
					PrintStream errout = System.err;
					synchronized (errout) {
						errout.println("Client error:");
						exc.printStackTrace(errout);
					}
				}
			});
			serverConnection.addErrorListener(new IOErrorListener() {
				@Override
				public void onIOError(Throwable exc) {
					PrintStream errout = System.err;
					synchronized (errout) {
						errout.println("Server error:");
						exc.printStackTrace(errout);
					}
				}
			});

			final short COMMAND_METHODRESULT = 3;

			Stub stub = (Stub) clientVariables.newRemoteInstance(Impl.class);

			RMITestUtil.replaceCommandHandler(COMMAND_METHODRESULT, new IOConsumer<Object[]>() {
				@Override
				public void accept(Object[] args) throws IOException {
					//replace the input of the method handler instead of directly throwing here
					//so the request handler pending request counter is properly decremented
					//by the original handler
					Object[] nargs = args.clone();
					//the index is 1
					//args:
					// RMIStream
					// DataInputUnsyncByteArrayInputStream
					// ReferencesReleasedAction

					System.out.println("IOErrorTest.runVariablesTestImpl().new IOConsumer() {...}.accept() "
							+ Arrays.toString(args));

					//get the first 5 bytes of the response, so it contains the request ID, and the response is properly dispatched
					DataInputUnsyncByteArrayInputStream originalin = (DataInputUnsyncByteArrayInputStream) args[1];
					System.out
							.println("IOErrorTest.runVariablesTestImpl().new IOConsumer() {...}.accept()" + originalin);
					nargs[1] = new DataInputUnsyncByteArrayInputStream(originalin.read(5));
					RMITestUtil.callOriginalCommandHandler(COMMAND_METHODRESULT, nargs);
				}
			});
			assertException(RMIIOFailureException.class, () -> stub.toString());
		} finally {
			RMITestUtil.restoreInternalHandlers();
		}
	}

	private static void checkErrorWithoutReqId(RMIConnection clientConnection, RMIConnection serverConnection,
			RMIVariables clientVariables) throws InvocationTargetException, NoSuchMethodException, AssertionError {
		try {
			//in case some other faulty test left it in an invalid state
			RMITestUtil.restoreInternalHandlers();
			clientConnection.addErrorListener(new IOErrorListener() {
				@Override
				public void onIOError(Throwable exc) {
					PrintStream errout = System.err;
					synchronized (errout) {
						errout.println("Client error:");
						exc.printStackTrace(errout);
					}
				}
			});
			serverConnection.addErrorListener(new IOErrorListener() {
				@Override
				public void onIOError(Throwable exc) {
					PrintStream errout = System.err;
					synchronized (errout) {
						errout.println("Server error:");
						exc.printStackTrace(errout);
					}
				}
			});

			final short COMMAND_METHODRESULT = 3;

			RMITestUtil.replaceCommandHandler(COMMAND_METHODRESULT, new IOConsumer<Object[]>() {
				@Override
				public void accept(Object[] args) throws IOException {
					//replace the input of the method handler instead of directly throwing here
					//so the request handler pending request counter is properly decremented
					//by the original handler
					Object[] nargs = args.clone();
					//the index is 1
					//args:
					// RMIStream
					// DataInputUnsyncByteArrayInputStream
					// ReferencesReleasedAction
					nargs[1] = new DataInputUnsyncByteArrayInputStream(new byte[] { 1 });
					RMITestUtil.callOriginalCommandHandler(COMMAND_METHODRESULT, nargs);
				}
			});

			Stub stub = (Stub) clientVariables.newRemoteInstance(Impl.class);
			assertException(RMIIOFailureException.class, () -> stub.toString());
		} finally {
			RMITestUtil.restoreInternalHandlers();
		}
	}

}
