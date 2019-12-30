package testing.saker.build.tests.rmi;

import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMIVariables;

public abstract class BaseVariablesRMITestCase extends BaseRMITestCase {
	protected RMIVariables clientVariables;
	protected RMIVariables serverVariables;

	@Override
	protected final void runTestImpl() throws Exception {
		try (RMIVariables cv = clientConnection.newVariables()) {
			clientVariables = cv;
			serverVariables = RMITestUtil.getCorrespondingConnectionVariables(serverConnection, cv);
			runVariablesTestImpl();
		}
	}

	protected abstract void runVariablesTestImpl() throws Exception;

}
