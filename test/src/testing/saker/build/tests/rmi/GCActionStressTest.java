package testing.saker.build.tests.rmi;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMITestUtil;
import saker.util.ReflectUtils;
import saker.util.thread.ThreadUtils;
import saker.util.thread.ThreadUtils.ThreadWorkPool;
import testing.saker.SakerTest;

/**
 * Stress testing the garbage collection, and checking that every garbage collection command is processed when all
 * requests are done.
 */
@SakerTest
public class GCActionStressTest extends BaseVariablesRMITestCase {

	public interface Stub {
		public Stub createOne(String val);

		public String getVal();

		public default Stub passThrough(Stub s) {
			return s;
		}
	}

	public static class Impl implements Stub {

		public String val;

		public Impl(String val) {
			this.val = val;
		}

		@Override
		public String getVal() {
			return val;
		}

		@Override
		public Stub createOne(String val) {
			return new Impl(val);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Impl[");
			builder.append("val=");
			builder.append(val);
			builder.append("]");
			return builder.toString();
		}

	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		ReferenceQueue<Object> refqueue = new ReferenceQueue<>();
		Collection<WeakReference<Object>> objectrefs = ConcurrentHashMap.newKeySet();
		try (ThreadWorkPool pool = ThreadUtils.newDynamicWorkPool()) {
			//do it in parallel so gc and requests and things can be interleaved
			for (int i = 0; i < 8; i++) {
				int finali = i;
				pool.offer(() -> {
					Stub stub = (Stub) clientVariables.newRemoteInstance(
							ReflectUtils.getConstructorAssert(Impl.class, String.class), "run-" + finali);
					objectrefs.add(new WeakReference<Object>(stub, refqueue));

					String val = stub.getVal();

					for (int j = 0; j < 50; j++) {
						Stub theone = stub.createOne(val + "-one-" + j);
						Impl impl = new Impl(val + "-impl-" + j);

						objectrefs.add(new WeakReference<Object>(impl, refqueue));

						Stub pt = theone.passThrough(impl);
						assertIdentityEquals(pt, impl);
						impl = null;
						System.gc();

						System.out.println("GCActionStressTest.runVariablesTestImpl() " + theone);
						theone = null;
						System.gc();
						System.out.println("GCActionStressTest.runVariablesTestImpl() " + finali + "/" + j
								+ " local live counts: client: " + RMITestUtil.getLiveLocalObjectCount(clientVariables)
								+ " server: " + RMITestUtil.getLiveLocalObjectCount(serverVariables)
								+ " remote live counts: client: "
								+ RMITestUtil.getLiveRemoteObjectCount(clientVariables) + " server: "
								+ RMITestUtil.getLiveRemoteObjectCount(serverVariables));
					}
					stub = null;
					System.gc();
					System.out.println("GCActionStressTest.runVariablesTestImpl() " + finali
							+ " local live counts: client: " + RMITestUtil.getLiveLocalObjectCount(clientVariables)
							+ " server: " + RMITestUtil.getLiveLocalObjectCount(serverVariables)
							+ " remote live counts: client: " + RMITestUtil.getLiveRemoteObjectCount(clientVariables)
							+ " server: " + RMITestUtil.getLiveRemoteObjectCount(serverVariables));
				});
			}
		}

		for (int i = 0; i < 100 && !objectrefs.isEmpty(); i++) {
			System.out.println("GCActionStressTest.runVariablesTestImpl() object refs count: " + objectrefs.size());
			System.gc();
			while (true) {
				Reference<? extends Object> ref = refqueue.remove(10);
				if (ref == null) {
					break;
				}
				objectrefs.remove(ref);
			}
		}
		if (!objectrefs.isEmpty()) {
			StringBuilder alivesb = new StringBuilder();
			alivesb.append("Alive objects: ");
			for (WeakReference<Object> ref : objectrefs) {
				Object obj = ref.get();
				if (obj == null) {
					continue;
				}
				alivesb.append(obj.toString());
				alivesb.append(" remote: ");
				alivesb.append(RMIConnection.isRemoteObject(obj));
				alivesb.append(", ");
			}
			throw fail(alivesb.toString());
		}

		//if gc works correctly, the live objects of the RMIVariables should be cleared after enough garbage collection
		System.out.println("GCActionStressTest.runVariablesTestImpl() check alive objects");
		for (int i = 0; i < 100; i++) {
			if (RMITestUtil.getLiveLocalObjectCount(clientVariables) != 0) {
			} else if (RMITestUtil.getLiveLocalObjectCount(serverVariables) != 0) {
			} else if (RMITestUtil.getLiveRemoteObjectCount(clientVariables) != 0) {
			} else if (RMITestUtil.getLiveRemoteObjectCount(serverVariables) != 0) {
			} else {
				//no more objects, done garbage collecting
				break;
			}
			System.gc();
			Thread.sleep(20);
		}
		System.out.println("GCActionStressTest.runVariablesTestImpl() done checking alive objects");

		int clocal = RMITestUtil.getLiveLocalObjectCount(clientVariables);
		int slocal = RMITestUtil.getLiveLocalObjectCount(serverVariables);
		int cremote = RMITestUtil.getLiveRemoteObjectCount(clientVariables);
		int sremote = RMITestUtil.getLiveRemoteObjectCount(serverVariables);
		assertEquals(clocal, 0,
				"client local objects still alive: " + clocal + "/" + slocal + " - " + cremote + "/" + sremote);
		assertEquals(slocal, 0,
				"server local objects still alive: " + clocal + "/" + slocal + " - " + cremote + "/" + sremote);
		assertEquals(cremote, 0,
				"client remote objects still alive: " + clocal + "/" + slocal + " - " + cremote + "/" + sremote);
		assertEquals(sremote, 0,
				"server remote objects still alive: " + clocal + "/" + slocal + " - " + cremote + "/" + sremote);
	}

	@Override
	protected BaseRMITestSettings getTestSettings() {
		BaseRMITestSettings result = super.getTestSettings();
		//enough to run this test once, with a single executor
		result.maxStreamCount = 1;
		result.executors = new Executor[] { null };
		return result;
	}

}
