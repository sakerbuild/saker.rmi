package testing.saker.build.tests.rmi;

import java.util.concurrent.ThreadLocalRandom;

import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMIVariables;
import saker.rmi.exception.RMIIOFailureException;
import testing.saker.SakerTest;

@SakerTest
public class DispatchFailureRMITest extends BaseVariablesRMITestCase {
	//scenario:
	//call a remote object of the server
	//that one dispatches a call back to the client
	//however, writing the dispatch call succeeds, but the response is never read
	//in some cases the connection gets deadlocked
	//because the Request is still in the RequestHandler

	private static RMIVariables varsStreamToClose;

	public interface Stub {
		public default String callToString(Stub stub) throws Exception {
			RMITestUtil.closeRMIStreamInput(varsStreamToClose);
			Thread.sleep(100);
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
	protected void runVariablesTestImpl() throws Exception {
		//synchronize, only a single test can be run at a time
		synchronized (DispatchFailureRMITest.class) {
			varsStreamToClose = serverVariables;
			try {
				Stub remoteobj = (Stub) clientVariables.newRemoteInstance(Impl.class);
				assertException(RMIIOFailureException.class, () -> {
					String str = remoteobj.callToString(new Impl());
					System.out.println("DispatchFailureRMITest.runVariablesTestImpl() " + str);
				});
			} finally {
				varsStreamToClose = null;
			}
		}
	}

}
