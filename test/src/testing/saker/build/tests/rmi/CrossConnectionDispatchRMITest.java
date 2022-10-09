package testing.saker.build.tests.rmi;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMIVariables;
import saker.util.io.ResourceCloser;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class CrossConnectionDispatchRMITest extends SakerTestCase {
	public interface Stub {
		public default String callToString(Stub stub) throws Exception {
			return stub.toString();
		}
	}

	public static class Impl implements Stub {
		private final int val = ThreadLocalRandom.current().nextInt();

		public Impl() {
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Impl[val=");
			builder.append(val);
			builder.append("]");
			return builder.toString();
		}
	}

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		RMIOptions options = new RMIOptions().classLoader(getClass().getClassLoader());
		try (ResourceCloser closer = new ResourceCloser()) {
			RMIConnection[] connections1 = RMITestUtil.createPipedConnection(options);
			closer.add(connections1);
			RMIConnection[] connections2 = RMITestUtil.createPipedConnection(options);
			closer.add(connections2);

			try (RMIVariables c1clientvars = connections1[0].newVariables();
					RMIVariables c2clientvars = connections2[0].newVariables()) {

				Stub c1stub = (Stub) c1clientvars.newRemoteInstance(Impl.class);
				Stub c2stub = (Stub) c2clientvars.newRemoteInstance(Impl.class);

				//run some remote call some time to consumer internal request identifiers
				for (int i = 0; i < 10; i++) {
					c1stub.toString();
				}
				c1stub.callToString(new Impl());

				//this used to throw an error, where the dispatch request identifier wasn't found
				//this was caused as the thread local that tracked this was a static final instance
				//and is solved by having it on a per-RMIconnection basis
				c1stub.callToString(c2stub);
			}
		}
	}

}
