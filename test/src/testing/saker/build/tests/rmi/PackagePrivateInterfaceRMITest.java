package testing.saker.build.tests.rmi;

import testing.saker.SakerTest;

@SakerTest
public class PackagePrivateInterfaceRMITest extends BaseVariablesRMITestCase {
	public interface PublicStub {
		public default String pub() {
			return "pub";
		}
	}

	private interface PrivateStub {
		public default String priv() {
			return "priv";
		}
	}

	protected interface ProtectedStub {
		public default String prot() {
			return "prot";
		}
	}

	interface PackageStub {
		public default String pack() {
			return "pack";
		}
	}

	public static class Impl implements PublicStub, PrivateStub, ProtectedStub, PackageStub {
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Object impl = clientVariables.newRemoteInstance(Impl.class);
		assertInstanceOf(impl, PublicStub.class);
		assertNotInstanceOf(impl, PrivateStub.class);
		assertNotInstanceOf(impl, ProtectedStub.class);
		assertNotInstanceOf(impl, PackageStub.class);
	}

}
