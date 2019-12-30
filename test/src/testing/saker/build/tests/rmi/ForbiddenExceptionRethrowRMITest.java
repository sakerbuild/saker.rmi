package testing.saker.build.tests.rmi;

import java.io.IOException;

import saker.rmi.annot.invoke.RMIExceptionRethrow;
import saker.rmi.annot.invoke.RMIForbidden;
import testing.saker.SakerTest;

@SakerTest
public class ForbiddenExceptionRethrowRMITest extends BaseVariablesRMITestCase {
	public interface Stub {

		@RMIExceptionRethrow(IOException.class)
		@RMIForbidden
		public String f() throws IOException;
	}

	public static class Impl implements Stub {

		@Override
		public String f() throws IOException {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
		assertException(IOException.class, s::f);
	}

}
