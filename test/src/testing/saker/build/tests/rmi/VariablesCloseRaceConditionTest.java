package testing.saker.build.tests.rmi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIConnection.IOErrorListener;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMIVariables;
import saker.rmi.exception.RMICallFailedException;
import saker.rmi.exception.RMIResourceUnavailableException;
import saker.util.ReflectUtils;
import saker.util.io.function.IOFunction;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

/**
 * Tests some scenarios when the other endpoint {@link RMIVariables} is closed before a request can fully arrive.
 */
@SakerTest
public class VariablesCloseRaceConditionTest extends SakerTestCase {

	private static final String CONTEXT_VAR_IMPL_CLASSLOADER = "impl.cl";
	private static final String CONTEXT_VAR_IMPL_INSTANCE = "impl.instance";

	public interface Stub {
		public static final Method METHOD_DOIT = ReflectUtils.getMethodAssert(Stub.class, "doIt",
				VariablesClosingReadExternalizable.class);

		/**
		 * @param arg
		 *            Dummy arg for closing the variables when the arg is read
		 */
		public default void doIt(VariablesClosingReadExternalizable arg) {
		}
	}

	public static class Impl implements Stub {
		public Impl() {
		}

		/**
		 * @param arg
		 *            Dummy arg for closing the variables when the arg is read
		 */
		public Impl(VariablesClosingReadExternalizable arg) {
		}
	}

	public static class VariablesClosingReadExternalizable implements Externalizable {
		private static final long serialVersionUID = 1L;

		public VariablesClosingReadExternalizable() {
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			//ignore
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			//reading on the server endpoint
			varsToClose.close();
		}

	}

	private static RMIVariables varsToClose = null;

	private final RMIOptions baseOptions = new RMIOptions().classLoader(getClass().getClassLoader());

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		RMIConnection[] connections = RMITestUtil.createPipedConnection(baseOptions);
		RMIConnection serverConnection = connections[1];
		try {
			RMIConnection clientConnection = connections[0];
			try {
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

				serverConnection.putContextVariable(CONTEXT_VAR_IMPL_CLASSLOADER, Impl.class.getClassLoader());
				serverConnection.putContextVariable(CONTEXT_VAR_IMPL_INSTANCE, new Impl());

				checkMethodCallServerVariablesClosedAfterStartedRequest(serverConnection, clientConnection);
				checkMethodCallServerVariablesClosedBeforeStartedRequest(serverConnection, clientConnection);

				checkNewRemoteInstanceServerVariablesClosedAfterStartedRequest(serverConnection, clientConnection);
				checkNewRemoteInstanceServerVariablesClosedBeforeStartedRequest(serverConnection, clientConnection);

				checkNewRemoteOnlyInstanceServerVariablesClosedAfterStartedRequest(serverConnection, clientConnection);
				checkNewRemoteOnlyInstanceServerVariablesClosedBeforeStartedRequest(serverConnection, clientConnection);

				checkMethodCallAsyncServerVariablesClosedAfterStartedRequest(serverConnection, clientConnection);
				checkMethodCallAsyncServerVariablesClosedBeforeStartedRequest(serverConnection, clientConnection);

				checkContextVariableMethodCallServerVariablesClosedAfterStartedRequest(serverConnection,
						clientConnection);
				checkContextVariableMethodCallServerVariablesClosedBeforeStartedRequest(serverConnection,
						clientConnection);

				//just a last check, that after all these failure tests, the connection still works
				try (RMIVariables clientVariables = clientConnection.newVariables();) {
					Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
					s.toString();
				}
			} finally {
				clientConnection.closeWait();
			}
		} finally {
			serverConnection.closeWait();
		}
	}

	private static void checkMethodCallServerVariablesClosedAfterStartedRequest(RMIConnection serverConnection,
			RMIConnection clientConnection) throws Exception {
		RMIVariables clientVariables = clientConnection.newVariables();
		try {
			RMIVariables serverVariables = RMITestUtil.getCorrespondingConnectionVariables(serverConnection,
					clientVariables);
			varsToClose = serverVariables;

			Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
			//this should succeed, as the server variables is closed AFTER the 
			//request is started on the variables
			s.doIt(new VariablesClosingReadExternalizable());
			assertTrue(serverVariables.isClosed());

		} finally {
			varsToClose = null;
			clientVariables.close();
		}
	}

	private static void checkMethodCallServerVariablesClosedBeforeStartedRequest(RMIConnection serverConnection,
			RMIConnection clientConnection) throws Exception {
		try {
			//in case some other faulty test left it in an invalid state
			RMITestUtil.restoreInternalHandlers();

			RMIVariables clientVariables = clientConnection.newVariables();
			try {
				RMIVariables serverVariables = RMITestUtil.getCorrespondingConnectionVariables(serverConnection,
						clientVariables);

				final short COMMAND_METHODCALL = 2;

				RMITestUtil.replaceCommandHandler(COMMAND_METHODCALL, new IOFunction<Object[], Object>() {
					@Override
					public Object apply(Object[] args) throws IOException {
						serverVariables.close();
						return RMITestUtil.callOriginalCommandHandler(COMMAND_METHODCALL, args);
					}
				});
				Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
				RMICallFailedException exc = assertException(RMICallFailedException.class, () -> s.toString());
				//with a cause that the variables is not available
				assertInstanceOf(exc.getCause(), RMIResourceUnavailableException.class);
				assertTrue(serverVariables.isClosed());

			} finally {
				clientVariables.close();
			}
		} finally {
			RMITestUtil.restoreInternalHandlers();
		}
	}

	private static void checkNewRemoteInstanceServerVariablesClosedAfterStartedRequest(RMIConnection serverConnection,
			RMIConnection clientConnection) throws Exception {
		RMIVariables clientVariables = clientConnection.newVariables();
		try {
			RMIVariables serverVariables = RMITestUtil.getCorrespondingConnectionVariables(serverConnection,
					clientVariables);
			varsToClose = serverVariables;

			//this should succeed, as the server variables is closed AFTER the 
			//request is started on the variables
			@SuppressWarnings("unused")
			Stub s = (Stub) clientVariables.newRemoteInstance(
					Impl.class.getConstructor(VariablesClosingReadExternalizable.class),
					new VariablesClosingReadExternalizable());
			assertTrue(serverVariables.isClosed());

		} finally {
			varsToClose = null;
			clientVariables.close();
		}
	}

	private static void checkNewRemoteInstanceServerVariablesClosedBeforeStartedRequest(RMIConnection serverConnection,
			RMIConnection clientConnection) throws Exception {
		try {
			//in case some other faulty test left it in an invalid state
			RMITestUtil.restoreInternalHandlers();

			RMIVariables clientVariables = clientConnection.newVariables();
			try {
				RMIVariables serverVariables = RMITestUtil.getCorrespondingConnectionVariables(serverConnection,
						clientVariables);

				final short COMMAND_NEWINSTANCE = 1;

				RMITestUtil.replaceCommandHandler(COMMAND_NEWINSTANCE, new IOFunction<Object[], Object>() {
					@Override
					public Object apply(Object[] args) throws IOException {
						serverVariables.close();
						return RMITestUtil.callOriginalCommandHandler(COMMAND_NEWINSTANCE, args);
					}
				});

				RMICallFailedException exc = assertException(RMICallFailedException.class,
						() -> clientVariables.newRemoteInstance(Impl.class));
				//with a cause that the variables is not available
				assertInstanceOf(exc.getCause(), RMIResourceUnavailableException.class);
				assertTrue(serverVariables.isClosed());

			} finally {
				clientVariables.close();
			}
		} finally {
			RMITestUtil.restoreInternalHandlers();
		}
	}

	private static void checkNewRemoteOnlyInstanceServerVariablesClosedAfterStartedRequest(
			RMIConnection serverConnection, RMIConnection clientConnection) throws Exception {
		RMIVariables clientVariables = clientConnection.newVariables();
		try {
			RMIVariables serverVariables = RMITestUtil.getCorrespondingConnectionVariables(serverConnection,
					clientVariables);
			varsToClose = serverVariables;

			Object remotecl = clientVariables.getRemoteContextVariable(CONTEXT_VAR_IMPL_CLASSLOADER);

			clientVariables.newRemoteOnlyInstance(remotecl, Impl.class.getName(),
					new String[] { VariablesClosingReadExternalizable.class.getName() },
					new Object[] { new VariablesClosingReadExternalizable() });
			assertTrue(serverVariables.isClosed());

		} finally {
			varsToClose = null;
			clientVariables.close();
		}
	}

	private static void checkNewRemoteOnlyInstanceServerVariablesClosedBeforeStartedRequest(
			RMIConnection serverConnection, RMIConnection clientConnection) throws Exception {
		try {
			//in case some other faulty test left it in an invalid state
			RMITestUtil.restoreInternalHandlers();

			RMIVariables clientVariables = clientConnection.newVariables();
			try {
				RMIVariables serverVariables = RMITestUtil.getCorrespondingConnectionVariables(serverConnection,
						clientVariables);

				final short COMMAND_NEWINSTANCE_UNKNOWNCLASS = 13;

				RMITestUtil.replaceCommandHandler(COMMAND_NEWINSTANCE_UNKNOWNCLASS, new IOFunction<Object[], Object>() {
					@Override
					public Object apply(Object[] args) throws IOException {
						serverVariables.close();
						return RMITestUtil.callOriginalCommandHandler(COMMAND_NEWINSTANCE_UNKNOWNCLASS, args);
					}
				});

				Object remotecl = clientVariables.getRemoteContextVariable(CONTEXT_VAR_IMPL_CLASSLOADER);

				RMICallFailedException exc = assertException(RMICallFailedException.class,
						() -> clientVariables.newRemoteOnlyInstance(remotecl, Impl.class.getName(),
								new String[] { VariablesClosingReadExternalizable.class.getName() },
								new Object[] { new VariablesClosingReadExternalizable() }));
				//with a cause that the variables is not available
				assertInstanceOf(exc.getCause(), RMIResourceUnavailableException.class);
				assertTrue(serverVariables.isClosed());

			} finally {
				clientVariables.close();
			}
		} finally {
			RMITestUtil.restoreInternalHandlers();
		}
	}

	private static void checkMethodCallAsyncServerVariablesClosedAfterStartedRequest(RMIConnection serverConnection,
			RMIConnection clientConnection) throws Exception {
		RMIVariables clientVariables = clientConnection.newVariables();
		try {
			RMIVariables serverVariables = RMITestUtil.getCorrespondingConnectionVariables(serverConnection,
					clientVariables);
			varsToClose = serverVariables;

			final short COMMAND_METHODCALL_ASYNC_WITH_RESPONSE = 33;

			Semaphore sem = new Semaphore(0);

			RMITestUtil.replaceCommandHandler(COMMAND_METHODCALL_ASYNC_WITH_RESPONSE,
					new IOFunction<Object[], Object>() {
						@Override
						public Object apply(Object[] args) throws IOException {
							try {
								return RMITestUtil.callOriginalCommandHandler(COMMAND_METHODCALL_ASYNC_WITH_RESPONSE,
										args);
							} finally {
								sem.release();
							}
						}
					});

			Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
			//this should succeed, as the server variables is closed AFTER the 
			//request is started on the variables
			RMIVariables.invokeRemoteMethodAsync(s, Stub.METHOD_DOIT, new VariablesClosingReadExternalizable());
			assertTrue(sem.tryAcquire(30, TimeUnit.SECONDS), "failed to acquire semaphore");
			assertTrue(serverVariables.isClosed());
		} finally {
			varsToClose = null;
			clientVariables.close();
		}
	}

	private static void checkMethodCallAsyncServerVariablesClosedBeforeStartedRequest(RMIConnection serverConnection,
			RMIConnection clientConnection) throws Exception {
		try {
			//in case some other faulty test left it in an invalid state
			RMITestUtil.restoreInternalHandlers();

			RMIVariables clientVariables = clientConnection.newVariables();
			try {
				RMIVariables serverVariables = RMITestUtil.getCorrespondingConnectionVariables(serverConnection,
						clientVariables);

				final short COMMAND_METHODCALL_ASYNC_WITH_RESPONSE = 33;

				Semaphore sem = new Semaphore(0);

				RMITestUtil.replaceCommandHandler(COMMAND_METHODCALL_ASYNC_WITH_RESPONSE,
						new IOFunction<Object[], Object>() {
							@Override
							public Object apply(Object[] args) throws IOException {
								try {
									serverVariables.close();
									return RMITestUtil
											.callOriginalCommandHandler(COMMAND_METHODCALL_ASYNC_WITH_RESPONSE, args);
								} finally {
									sem.release();
								}
							}
						});

				Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
				RMIVariables.invokeRemoteMethodAsync(s, Object.class.getMethod("toString"));
				assertTrue(sem.tryAcquire(30, TimeUnit.SECONDS), "failed to acquire semaphore");
				assertTrue(serverVariables.isClosed());

			} finally {
				clientVariables.close();
			}
		} finally {
			RMITestUtil.restoreInternalHandlers();
		}
	}

	private static void checkContextVariableMethodCallServerVariablesClosedAfterStartedRequest(
			RMIConnection serverConnection, RMIConnection clientConnection) throws Exception {
		RMIVariables clientVariables = clientConnection.newVariables();
		try {
			RMIVariables serverVariables = RMITestUtil.getCorrespondingConnectionVariables(serverConnection,
					clientVariables);
			varsToClose = serverVariables;

			clientVariables.invokeContextVariableMethod(CONTEXT_VAR_IMPL_INSTANCE, Stub.METHOD_DOIT,
					new VariablesClosingReadExternalizable());
			assertTrue(serverVariables.isClosed());

		} finally {
			varsToClose = null;
			clientVariables.close();
		}
	}

	private static void checkContextVariableMethodCallServerVariablesClosedBeforeStartedRequest(
			RMIConnection serverConnection, RMIConnection clientConnection) throws Exception {
		try {
			//in case some other faulty test left it in an invalid state
			RMITestUtil.restoreInternalHandlers();

			RMIVariables clientVariables = clientConnection.newVariables();
			try {
				RMIVariables serverVariables = RMITestUtil.getCorrespondingConnectionVariables(serverConnection,
						clientVariables);

				final short COMMAND_METHODCALL_CONTEXTVAR = 29;

				RMITestUtil.replaceCommandHandler(COMMAND_METHODCALL_CONTEXTVAR,
						new IOFunction<Object[], Object>() {
							@Override
							public Object apply(Object[] args) throws IOException {
								serverVariables.close();
								return RMITestUtil.callOriginalCommandHandler(COMMAND_METHODCALL_CONTEXTVAR,
										args);
							}
						});
				RMICallFailedException exc = assertException(RMICallFailedException.class, () -> clientVariables
						.invokeContextVariableMethod(CONTEXT_VAR_IMPL_INSTANCE, Object.class.getMethod("toString")));
				exc.printStackTrace();
				//with a cause that the variables is not available
				assertInstanceOf(exc.getCause(), RMIResourceUnavailableException.class);
				assertTrue(serverVariables.isClosed());

			} finally {
				clientVariables.close();
			}
		} finally {
			RMITestUtil.restoreInternalHandlers();
		}
	}
}
