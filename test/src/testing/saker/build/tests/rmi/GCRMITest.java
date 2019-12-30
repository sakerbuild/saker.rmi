package testing.saker.build.tests.rmi;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;

import saker.rmi.connection.RMITestUtil;
import testing.saker.SakerTest;

@SakerTest
public class GCRMITest extends BaseVariablesRMITestCase {
	private static final int GC_TIMEOUT_MS = 50;

	public static class SomeRunnable implements Runnable {
		public static int run = 0;
		private String id = "";

		public SomeRunnable() {
		}

		public SomeRunnable(String id) {
			this.id = id;
		}

		@Override
		public void run() {
			System.out.println("GCRMITest.SomeRunnable.run() " + this);
		}

		@Override
		public String toString() {
			return super.toString() + "(" + id + ")";
		}
	}

	public static void runRunnable(Runnable run) {
		run.run();
	}

	private ReferenceQueue<Object> queue = new ReferenceQueue<>();

	@Override
	protected void runVariablesTestImpl() throws Exception {
		//hold reference to the references to get the queued
		@SuppressWarnings("unused")
		Collection<PhantomReference<Object>> refs = testInvocations();
		//this test is non deterministic, however it should probably pass if it works, and fail if it doesnt
		int gcdrefs = 0;
		for (int i = 0; i < 5000 / GC_TIMEOUT_MS && gcdrefs < 3; i++) {
			System.gc();
			while (queue.poll() != null) {
				gcdrefs++;
			}
			Thread.sleep(GC_TIMEOUT_MS);
		}
		assertEquals(gcdrefs, 3);

		assertEquals(RMITestUtil.getLiveLocalObjectCount(clientVariables), 0);
		assertEquals(RMITestUtil.getLiveLocalObjectCount(serverVariables), 0);
	}

	private Collection<PhantomReference<Object>> testInvocations() throws Exception {
		Collection<PhantomReference<Object>> result = new ArrayList<>();
		Runnable runref = (Runnable) clientVariables.newRemoteInstance(SomeRunnable.class);
		runref.run();
		runref.run();
		clientVariables.invokeRemoteStaticMethod(GCRMITest.class.getMethod("runRunnable", Runnable.class), runref);
		SomeRunnable cs = new SomeRunnable("clientside");
		clientVariables.invokeRemoteStaticMethod(GCRMITest.class.getMethod("runRunnable", Runnable.class), cs);
		clientVariables.invokeRemoteStaticMethod(GCRMITest.class.getMethod("runRunnable", Runnable.class), cs);

		result.add(new PhantomReference<>(runref, queue));
		result.add(new PhantomReference<>(RMITestUtil.getRemoteVariablesVariable(clientConnection, runref), queue));
		result.add(new PhantomReference<>(cs, queue));

		return result;
	}

}
