package testing.saker.build.tests.rmi;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIConnection.IOErrorListener;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMIVariables;
import saker.util.io.ResourceCloser;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class MultipleVariablesTest extends SakerTestCase {

	@Override
	@SuppressWarnings("try") // unused ResourceCloser
	public void runTest(Map<String, String> parameters) throws Throwable {
		try {
			RMIOptions baseoptions = new RMIOptions().maxStreamCount(1).classLoader(getClass().getClassLoader());
			RMIConnection[] connections = RMITestUtil.createPipedConnection(baseoptions, baseoptions);
			RMIConnection clientConnection = connections[0];
			RMIConnection serverConnection = connections[1];

			clientConnection.addErrorListener(new IOErrorListener() {
				@Override
				public void onIOError(Throwable exc) {
					PrintStream errout = System.err;
					synchronized (errout) {
						errout.println("Client error:");
						exc.printStackTrace(errout);
					}
				}
			});
			serverConnection.addErrorListener(new IOErrorListener() {
				@Override
				public void onIOError(Throwable exc) {
					PrintStream errout = System.err;
					synchronized (errout) {
						errout.println("Server error:");
						exc.printStackTrace(errout);
					}
				}
			});

			try (ResourceCloser closer = new ResourceCloser(clientConnection::closeWait, serverConnection::closeWait)) {

				List<RMIVariables> varlist = new ArrayList<>();
				try {
					for (int i = 0; i < 50; i++) {
						RMIVariables vars = clientConnection.newVariables();
						varlist.add(vars);
					}
				} catch (Throwable e) {
					closer.add(varlist);
					throw e;
				}

				//shuffle them, so the order of closing can be different
				//this is to test the RMIStream association addition, removal code
				Collections.shuffle(varlist);
				closer.add(varlist);
			}
		} finally {
			RMITestUtil.restoreInternalHandlers();
		}
	}

}
