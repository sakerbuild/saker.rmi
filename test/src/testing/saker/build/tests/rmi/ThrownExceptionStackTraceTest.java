package testing.saker.build.tests.rmi;

import java.lang.reflect.InvocationTargetException;

import saker.util.StringUtils;
import testing.saker.SakerTest;

@SakerTest
public class ThrownExceptionStackTraceTest extends BaseVariablesRMITestCase {

	public interface Stub {
		public default void throwIt() {
			throw new UnsupportedOperationException("exception");
		}
	}

	public static class Impl implements Stub {
		public Impl() {
		}

		public Impl(int val) {
			throw new IllegalArgumentException("invalid val");
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);

		//this stacktrace should be merged, as invoked through the proxy
		UnsupportedOperationException thrown = assertException(UnsupportedOperationException.class,
				() -> callThrowIt(s));
		thrown.printStackTrace();
		System.err.println();
		assertStackTraceElement(thrown.getStackTrace(), Stub.class.getName(), "throwIt");
		assertStackTraceElement(thrown.getStackTrace(), ThrownExceptionStackTraceTest.class.getName(), "callThrowIt");

		//this stacktrace doesn't need to be merged, as the local one is available via the InvocationTargetException
		//and the remote one is via the cause
		InvocationTargetException invexc = assertException(InvocationTargetException.class, () -> createImplFail());
		invexc.printStackTrace();
		System.err.println();
		assertStackTraceElement(invexc.getStackTrace(), ThrownExceptionStackTraceTest.class.getName(),
				"createImplFail");
		assertStackTraceElement(invexc.getCause().getStackTrace(), Impl.class.getName(), "<init>");
	}

	private Object createImplFail() throws InvocationTargetException, NoSuchMethodException {
		return clientVariables.newRemoteInstance(Impl.class.getConstructor(int.class), 123);
	}

	private static int assertStackTraceElement(StackTraceElement[] trace, String clazz, String method) {
		int idx = findStackTraceElement(trace, clazz, method);
		assertTrue(idx >= 0, "Stack trace element not found: " + clazz + "." + method + "() in:\n --- START ---\n    "
				+ StringUtils.toStringJoin("\n    ", trace) + "\n ---  END  ---");
		return idx;
	}

	private static int findStackTraceElement(StackTraceElement[] trace, String clazz, String method) {
		for (int idx = 0; idx < trace.length; idx++) {
			StackTraceElement t = trace[idx];
			if (t.getClassName().equals(clazz) && t.getMethodName().equals(method)) {
				return idx;
			}
		}
		return -1;
	}

	private static void callThrowIt(Stub s) {
		s.throwIt();
	}

}
