package testing.saker.build.tests.rmi;

import saker.rmi.exception.RMICallFailedException;
import testing.saker.SakerTest;

@SakerTest
public class MisconfiguredMethodCallTest extends BaseVariablesRMITestCase {

	public static class MyClass {
	}

	public interface Stub {
		public void f(MyClass mc);
	}

	public static class Impl implements Stub {
		@Override
		public void f(MyClass mc) {
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
		s.f(null);
		assertException(RMICallFailedException.class, () -> s.f(new MyClass()));
	}

}
