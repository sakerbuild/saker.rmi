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
import java.util.concurrent.Executor;

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
		BaseRMITestSettings settings = getTestSettings();
		int minstreams = settings.minStreamCount;
		int maxstreams = settings.maxStreamCount;
		if (minstreams < 1) {
			minstreams = 1;
		}
		if (maxstreams < 0) {
			maxstreams = BaseRMITestSettings.DEFAULT_MAX_STREAM_COUNT;
		}
		assertTrue(minstreams <= maxstreams, minstreams + " > " + maxstreams);
		for (int i = minstreams; i <= maxstreams; i++) {
			System.out.println("RMI test with max stream count: " + i);

			Executor[] executors = settings.executors;
			if (executors == null) {
				//test with multiple different executors by default
				//    null for the built-in task scheduling
				//    simple new thread / task executor
				executors = new Executor[] { null, new ThreadExecutor(), };
			}

			for (Executor executor : executors) {
				RMIOptions baseoptions = new RMIOptions().maxStreamCount(maxstreams)
						.classLoader(getClass().getClassLoader());
				if (executor != null) {
					baseoptions.executor(executor);
				}

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
	}

	protected RMIConnection[] createConnections(RMIOptions baseoptions) throws Exception {
		return RMITestUtil.createPipedConnection(new RMIOptions(baseoptions));
	}

	protected BaseRMITestSettings getTestSettings() {
		return new BaseRMITestSettings();
	}

	private static final class ThreadExecutor implements Executor {
		@Override
		public void execute(Runnable command) {
			new Thread(command).start();
		}
	}

	public static class BaseRMITestSettings {
		public static final int DEFAULT_MAX_STREAM_COUNT = Runtime.getRuntime().availableProcessors() * 2;

		public int minStreamCount = 1;
		public int maxStreamCount = DEFAULT_MAX_STREAM_COUNT;

		public Executor[] executors;
	}
}
