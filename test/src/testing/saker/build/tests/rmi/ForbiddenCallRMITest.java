package testing.saker.build.tests.rmi;

import saker.rmi.annot.invoke.RMIDefaultOnFailure;
import saker.rmi.annot.invoke.RMIExceptionRethrow;
import saker.rmi.annot.invoke.RMIForbidden;
import saker.rmi.exception.RMICallForbiddenException;
import testing.saker.SakerTest;

@SakerTest
public class ForbiddenCallRMITest extends BaseVariablesRMITestCase {
	public interface Stub {

		@RMIForbidden
		public default int forbidden(int i) {
			return i * i;
		}

		@RMIForbidden
		@RMIDefaultOnFailure
		public default int defforbidden(int i) {
			return i + i;
		}

		@RMIForbidden
		@RMIExceptionRethrow(IllegalStateException.class)
		public default int rethrow(int i) {
			return i * 4;
		}
	}

	public static class Impl implements Stub {

	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
		assertException(RMICallForbiddenException.class, () -> s.forbidden(123));
		assertEquals(10, s.defforbidden(5));

		assertException(IllegalStateException.class, () -> s.rethrow(123));
	}

}
