/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
		int maxstreams = getTestMaxStreamCount();
		for (int i = 1; i <= maxstreams; i++) {
			System.out.println("RMI test with max stream count: " + i);
			RMIOptions baseoptions = new RMIOptions().maxStreamCount(maxstreams)
					.classLoader(getClass().getClassLoader());

			RMIConnection[] connections = createConnections(baseoptions);
			clientConnection = connections[0];
			serverConnection = connections[1];
			try (ResourceCloser closer = new ResourceCloser(connections[0]::closeWait, connections[1]::closeWait)) {
				runTestImpl();
			} finally {
				clientConnection = null;
				serverConnection = null;
			}
		}
	}

	protected int getTestMaxStreamCount() {
		return Runtime.getRuntime().availableProcessors() * 2;
	}

	protected RMIConnection[] createConnections(RMIOptions baseoptions) throws Exception {
		return RMITestUtil.createPipedConnection(new RMIOptions(baseoptions));
	}

}
