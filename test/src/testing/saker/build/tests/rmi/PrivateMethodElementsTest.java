package testing.saker.build.tests.rmi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.rmi.exception.RMICallFailedException;
import testing.saker.SakerTest;

@SakerTest
public class PrivateMethodElementsTest extends BaseVariablesRMITestCase {

	private static class PrivateClass implements Externalizable {
		private static final long serialVersionUID = 1L;

		/**
		 * For {@link Externalizable}.
		 */
		public PrivateClass() {
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		}
	}

	public interface PrivateArgStub {
		public void f(PrivateClass pc);

		public default void publicf() {
			System.out.println("PrivateMethodElementsTest.PrivateArgStub.publicf()");
		}
	}

	public interface PrivateReturnStub {
		public PrivateClass f();

		public default void publicf() {
			System.out.println("PrivateMethodElementsTest.PrivateReturnStub.publicf()");
		}
	}

	public static class PrivateArgImpl implements PrivateArgStub {
		@Override
		public void f(PrivateClass pc) {
			System.out.println("PrivateMethodElementsTest.PrivateArgImpl.f() " + pc);
		}
	}

	public static class PrivateReturnImpl implements PrivateReturnStub {
		@Override
		public PrivateClass f() {
			return new PrivateClass();
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		PrivateArgStub pas = (PrivateArgStub) clientVariables.newRemoteInstance(PrivateArgImpl.class);
		pas.publicf();
		assertException(RMICallFailedException.class, () -> pas.f(null));

		PrivateReturnStub prs = (PrivateReturnStub) clientVariables.newRemoteInstance(PrivateReturnImpl.class);
		prs.publicf();
		assertException(RMICallFailedException.class, () -> prs.f());
	}

}
