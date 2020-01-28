package testing.saker.build.tests.rmi;

import saker.rmi.annot.transfer.RMISerialize;
import testing.saker.SakerTest;

@SakerTest
public class ExceptionArgumentTest extends BaseVariablesRMITestCase {

	public interface Stub {
		public void f(Exception e);

		public void serf(@RMISerialize Exception e);
	}

	public static class Impl implements Stub {

		@Override
		public void f(Exception e) {
			e.printStackTrace();
		}

		@Override
		public void serf(Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
		s.serf(new Exception("first"));
		s.f(new Exception("second"));
	}

}
