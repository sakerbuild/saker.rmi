package testing.saker.build.tests.rmi;

import testing.saker.SakerTest;

@SakerTest
public class RemoteOnlyInstanceRMITest extends BaseVariablesRMITestCase {
	public interface Stub {

	}

	public static class Impl implements Stub {
		public Impl() {
		}

		public Impl(Object o) {
		}

		public Impl(int i) {
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		serverConnection.putContextVariable("cl", Impl.class.getClassLoader());
		Object cl = clientVariables.getRemoteContextVariable("cl");

		clientVariables.newRemoteOnlyInstance(cl, Impl.class.getName(), new String[] {}, new Object[] {});
		clientVariables.newRemoteOnlyInstance(cl, Impl.class.getName(), new String[] { Object.class.getName() },
				new Object[] { 1 });
		//make sure primitive class names are looked up accordingly
		clientVariables.newRemoteOnlyInstance(cl, Impl.class.getName(), new String[] { int.class.getName() },
				new Object[] { 1 });
	}

}
