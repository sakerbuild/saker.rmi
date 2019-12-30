package testing.saker.build.tests.rmi;

import java.util.Map;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMITestUtil;
import saker.util.function.ThrowingRunnable;
import saker.util.io.IOUtils;
import saker.util.io.ResourceCloser;
import saker.util.thread.ExceptionThread;
import saker.util.thread.ThreadUtils;
import testing.saker.SakerTestCase;

public abstract class BaseRMITestCase extends SakerTestCase {
	protected RMIConnection clientConnection;
	protected RMIConnection serverConnection;

	@Override
	public final void runTest(Map<String, String> parameters) throws Throwable {
		ThreadGroup threadgroup = new ThreadGroup("RMI test group: " + getClass().getName());
		ExceptionThread thread = new ExceptionThread(threadgroup, (ThrowingRunnable) this::connectAndRunTest,
				"RMI test: " + getClass().getName());
		thread.start();
		thread.join();
		Throwable exc = thread.getException();
		//dump the threads before the destroy() call
		ThreadUtils.dumpThreadGroupStackTraces(System.out, threadgroup);
		try {
			threadgroup.destroy();
		} catch (IllegalThreadStateException e) {
			exc = IOUtils.addExc(exc, e);
		}
		if (exc instanceof AssertionError) {
			throw exc;
		}
		IOUtils.throwExc(exc);
	}

	protected abstract void runTestImpl() throws Exception;

	@SuppressWarnings("try")
	private void connectAndRunTest() throws Exception {
		RMIConnection[] connections = createConnections();
		clientConnection = connections[0];
		serverConnection = connections[1];
		try (ResourceCloser closer = new ResourceCloser(connections[0]::closeWait, connections[1]::closeWait)) {
			runTestImpl();
		}
	}

	protected RMIConnection[] createConnections() throws Exception {
		return RMITestUtil.createPipedConnection(new RMIOptions().classLoader(getClass().getClassLoader()));
	}

}
