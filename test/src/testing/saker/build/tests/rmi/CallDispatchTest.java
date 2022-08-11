package testing.saker.build.tests.rmi;

import testing.saker.SakerTest;

/**
 * Test for checking that a remote call is dispatched back to the correct thread.
 * <p>
 * If remote calls are performed in a single session back and forth, then they should run on the thread on which the
 * first remote call is executed.
 * <p>
 * This test is important in case when virtual threads are used, and setting thread locals might be disabled. (And is
 * disable in case of the test as in {@link BaseRMITestCase}.)
 */
@SakerTest
public class CallDispatchTest extends BaseVariablesRMITestCase {
	public interface Stub {
		public default void callIt(Runnable run) {
			run.run();
		}

		public void assertIsOnCallItThread();
	}

	public static class Impl implements Stub {
		private Thread callItThread;

		@Override
		public void callIt(Runnable run) {
			try {
				callItThread = Thread.currentThread();
				run.run();
			} finally {
				callItThread = null;
			}
		}

		@Override
		public void assertIsOnCallItThread() {
			assertIdentityEquals(callItThread, Thread.currentThread());
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Thread currentthread = Thread.currentThread();
		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
		s.callIt(() -> {
			assertIdentityEquals(Thread.currentThread(), currentthread);
			try {
				s.assertIsOnCallItThread();

				//do 1 more level of dispatching
				Stub s2 = (Stub) clientVariables.newRemoteInstance(Impl.class);
				s2.callIt(() -> {
					assertIdentityEquals(Thread.currentThread(), currentthread);
					s2.assertIsOnCallItThread();
				});
			} catch (Exception e) {
				throw fail(e);
			}
		});
	}

}
