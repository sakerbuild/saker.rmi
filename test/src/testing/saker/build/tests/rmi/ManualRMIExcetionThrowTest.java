package testing.saker.build.tests.rmi;

import java.io.IOException;

import saker.rmi.annot.invoke.RMIDefaultOnFailure;
import saker.rmi.annot.invoke.RMIExceptionRethrow;
import saker.rmi.exception.RMICallFailedException;
import testing.saker.SakerTest;

@SakerTest
public class ManualRMIExcetionThrowTest extends BaseVariablesRMITestCase {

	public interface Stub {
		public default String simpleThrow(String msg) {
			throw new RMICallFailedException(msg);
		}

		@RMIDefaultOnFailure
		public default String defOnFailureThrow(String msg) {
			return msg;
		}

		@RMIExceptionRethrow(IOException.class)
		public default String rethrow(String msg) throws IOException {
			throw new RMICallFailedException(msg);
		}
	}

	public static class Impl implements Stub {
		@Override
		public String defOnFailureThrow(String msg) {
			throw new RMICallFailedException(msg);
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
		try {
			s.simpleThrow("stmsg");
			fail();
		} catch (RMICallFailedException e) {
			assertEquals(e.getMessage(), "stmsg");
		}
		try {
			s.defOnFailureThrow("dofmsg");
			fail();
		} catch (RMICallFailedException e) {
			assertEquals(e.getMessage(), "dofmsg");
		}
		try {
			s.rethrow("rtmsg");
			fail();
		} catch (IOException e) {
			fail(e);
		} catch (RMICallFailedException e) {
			assertEquals(e.getMessage(), "rtmsg");
		}
	}

}
