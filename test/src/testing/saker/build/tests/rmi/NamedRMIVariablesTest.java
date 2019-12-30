package testing.saker.build.tests.rmi;

import saker.rmi.connection.RMIVariables;
import testing.saker.SakerTest;

@SakerTest
public class NamedRMIVariablesTest extends BaseRMITestCase {
	@SuppressWarnings("resource")
	@Override
	protected void runTestImpl() {
		RMIVariables v1;
		RMIVariables v2;
		try (RMIVariables vars1 = clientConnection.getVariables("vars")) {
			v1 = vars1;
			try (RMIVariables vars2 = clientConnection.getVariables("vars")) {
				v2 = vars2;
				assertTrue(!v1.isClosed());
				assertTrue(!v2.isClosed());
				assertIdentityEquals(vars1, vars2);
			}
			assertTrue(!v1.isClosed());
			assertTrue(!v2.isClosed());
		}
		assertTrue(v1.isClosed());
		assertTrue(v2.isClosed());
		try (RMIVariables vars1 = clientConnection.getVariables("vars")) {
			//as it was closed, we now need to get a new instance
			assertNotIdentityEquals(v1, vars1);
			v1 = vars1;
		}
		assertTrue(v1.isClosed());
	}
}
