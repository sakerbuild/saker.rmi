package testing.saker.build.tests.rmi;

import java.util.function.Predicate;

import saker.util.function.Functionals;
import testing.saker.SakerTest;

@SakerTest
public class InterfaceEnumWriteRMITest extends BaseVariablesRMITestCase {

	public interface Stub {
		public void f(Predicate<?> pred);
	}

	public static class Impl implements Stub {
		@Override
		public void f(Predicate<?> pred) {
			assertIdentityEquals(pred, Functionals.alwaysPredicate());
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
		s.f(Functionals.alwaysPredicate());
	}
}
