package testing.saker.build.tests.rmi;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import saker.rmi.annot.invoke.RMIForbidden;
import saker.rmi.annot.invoke.RMIRedirect;
import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMIVariables;
import saker.rmi.exception.RMICallFailedException;
import saker.rmi.exception.RMICallForbiddenException;
import saker.rmi.exception.RMIContextVariableNotFoundException;
import saker.util.ObjectUtils;
import saker.util.ReflectUtils;
import saker.util.io.ResourceCloser;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class ContextVarMethodInvokeTest extends SakerTestCase {

	public interface Stub {
		public default int add(int i) {
			return i + i;
		}

		public default void throwing() {
			throw new RuntimeException("excmsg");
		}

		public default void redispatchTo(Runnable run) {
			run.run();
		}

		@RMIForbidden
		public default int forbidden() {
			return 123;
		}

		@RMIRedirect
		public default int redirected(int i) {
			return i + i;
		}

		public static int redirected(Stub s, int i) {
			return i * i;
		}
	}

	public interface NonImplemented {
		public default String nonimp() {
			return "ni";
		}

		public default int add(int i) {
			return i + i;
		}
	}

	public static class Impl implements Stub {
		@Override
		public int add(int i) {
			return i * 3;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Impl[]");
			return builder.toString();
		}

	}

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		testProtocolVersion((short) RMIConnection.PROTOCOL_VERSION_1);
		testProtocolVersion((short) RMIConnection.PROTOCOL_VERSION_2);
	}

	@SuppressWarnings("try") // unused ResourceCloser
	private void testProtocolVersion(short protocolversion)
			throws Exception, IOException, InvocationTargetException, AssertionError {
		System.out.println("ContextVarMethodInvokeTest.testProtocolVersion() test protocol version " + protocolversion);
		Thread currentthread = Thread.currentThread();
		RMIOptions baseoptions = new RMIOptions().maxStreamCount(1).classLoader(getClass().getClassLoader());
		RMIConnection[] connections = RMITestUtil.createPipedConnection(baseoptions, baseoptions, protocolversion);
		RMIConnection clientConnection = connections[0];
		RMIConnection serverConnection = connections[1];
		try (ResourceCloser closer = new ResourceCloser(clientConnection::closeWait, serverConnection::closeWait);
				RMIVariables clientVariables = clientConnection.newVariables();) {

			final String varname = "implvar";
			Impl implvar = new Impl();
			serverConnection.putContextVariable(varname, implvar);

			Object tostringresult = clientVariables.invokeContextVariableMethod(varname,
					ReflectUtils.getMethodAssert(Object.class, "toString"));
			System.out.println("ContextVarMethodInvokeTest.runVariablesTestImpl() " + tostringresult);
			assertEquals(tostringresult, "Impl[]");

			assertException(RMIContextVariableNotFoundException.class, () -> clientVariables
					.invokeContextVariableMethod("nonexist", ReflectUtils.getMethodAssert(Object.class, "toString")));

			Object addresult = clientVariables.invokeContextVariableMethod(varname,
					ReflectUtils.getMethodAssert(Stub.class, "add", int.class), 10);
			assertEquals(addresult, 10 * 3);

			//wrong arguments
			assertException(RMICallFailedException.class, () -> clientVariables.invokeContextVariableMethod(varname,
					ReflectUtils.getMethodAssert(Stub.class, "add", int.class)));
			//wrong argument types
			assertException(RMICallFailedException.class, () -> clientVariables.invokeContextVariableMethod(varname,
					ReflectUtils.getMethodAssert(Stub.class, "add", int.class), "aaa"));

			//try calling a method for which the variable is not an instance of
			assertException(RMICallFailedException.class, () -> clientVariables.invokeContextVariableMethod(varname,
					ReflectUtils.getMethodAssert(NonImplemented.class, "nonimp")));

			//try calling this method, which has a common signature with an implementing interface,
			//but through a non-implemented interface
			assertException(RMICallFailedException.class, () -> clientVariables.invokeContextVariableMethod(varname,
					ReflectUtils.getMethodAssert(NonImplemented.class, "add", int.class), 10));

			assertException(RuntimeException.class, () -> {
				InvocationTargetException ite = assertException(InvocationTargetException.class, () -> {
					clientVariables.invokeContextVariableMethod(varname,
							ReflectUtils.getMethodAssert(Stub.class, "throwing"));
				});
				Throwable cause = ite.getCause();
				assertEquals(cause.getMessage(), "excmsg");
				throw ObjectUtils.sneakyThrow(cause);
			});

			//test redispatching
			Method redispatchmethod = ReflectUtils.getMethodAssert(Stub.class, "redispatchTo", Runnable.class);
			clientVariables.invokeContextVariableMethod(varname, redispatchmethod, (Runnable) () -> {
				Thread thread1 = Thread.currentThread();
				System.out.println("ContextVarMethodInvokeTest.testProtocolVersion() redispatch 1 " + thread1);
				assertIdentityEquals(currentthread, thread1);
				try {
					clientVariables.invokeContextVariableMethod(varname, redispatchmethod, (Runnable) () -> {
						Thread thread2 = Thread.currentThread();
						System.out.println("ContextVarMethodInvokeTest.testProtocolVersion() redispatch 2 " + thread2);
						assertIdentityEquals(currentthread, thread2);
					});
				} catch (Exception e) {
					throw ObjectUtils.sneakyThrow(e);
				}
			});

			//forbidden methods are allowed through this function
			assertEquals(clientVariables.invokeContextVariableMethod(varname,
					ReflectUtils.getMethodAssert(Stub.class, "forbidden")), 123);
			//the methods are not redirected
			assertEquals(clientVariables.invokeContextVariableMethod(varname,
					ReflectUtils.getMethodAssert(Stub.class, "redirected", int.class), 123), 123 + 123);

			Stub stub = (Stub) clientVariables.getRemoteContextVariable(varname);
			assertException(RMICallForbiddenException.class, stub::forbidden);
			assertEquals(stub.redirected(123), 123 * 123);
		}
	}

}
