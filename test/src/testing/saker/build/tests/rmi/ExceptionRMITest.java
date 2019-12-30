package testing.saker.build.tests.rmi;

import testing.saker.SakerTest;

@SakerTest
public class ExceptionRMITest extends BaseVariablesRMITestCase {

	public static class MyException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	public static void throwRuntime() throws MyException {
		throw new MyException();
	}

	public static class InstantiationThrower {
		public InstantiationThrower() {
			throw new MyException();
		}
	}

	public static class CallThrower implements Runnable {
		@Override
		public void run() {
			throw new MyException();
		}
	}

	@Override
	protected void runVariablesTestImpl() {
		assertException(MyException.class, unwrapInvocationTargetException(
				() -> clientVariables.invokeRemoteStaticMethod(ExceptionRMITest.class.getMethod("throwRuntime")))::run);
		assertException(MyException.class, unwrapInvocationTargetException(
				() -> clientVariables.newRemoteInstance(InstantiationThrower.class))::run);
		assertException(MyException.class,
				() -> ((Runnable) clientVariables.newRemoteInstance(CallThrower.class)).run());
	}

}
