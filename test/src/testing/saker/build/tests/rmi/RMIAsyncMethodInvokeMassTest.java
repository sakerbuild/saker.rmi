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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMIVariables;
import saker.rmi.exception.RMIResourceUnavailableException;
import saker.util.ReflectUtils;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class RMIAsyncMethodInvokeMassTest extends SakerTestCase {

	private static final Method INCREMENT_METHOD = ReflectUtils.getMethodAssert(Stub.class, "increment");

	public interface Stub {
		public void increment();

		public int getCount();
	}

	public static class Impl implements Stub {
		private static final AtomicIntegerFieldUpdater<RMIAsyncMethodInvokeMassTest.Impl> AIFU_count = AtomicIntegerFieldUpdater
				.newUpdater(RMIAsyncMethodInvokeMassTest.Impl.class, "count");
		public volatile int count;

		@Override
		public void increment() {
			AIFU_count.getAndIncrement(this);
		}

		@Override
		public int getCount() {
			return count;
		}
	}

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		runMassTest(false, false);
		runMassTest(false, true);
		runMassTest(true, false);
		runMassTest(true, true);
	}

	private void runMassTest(boolean callsyncmethodtoo, boolean waitvars) throws Exception, InvocationTargetException,
			NoSuchMethodException, AssertionError, IOException, InterruptedException {
		final int CALL_COUNT = 10000;

		RMIOptions baseoptions = new RMIOptions().classLoader(getClass().getClassLoader());

		Impl i = null;
		Stub s;
		Stub serverstub;
		RMIConnection[] connections = RMITestUtil.createPipedConnection(baseoptions);
		RMIConnection serverConnection = connections[1];
		try {
			RMIConnection clientConnection = connections[0];
			try {
				RMIVariables clientVariables = clientConnection.newVariables();
				try {
					System.out.println("Client variables: " + System.identityHashCode(clientVariables));
					s = (Stub) clientVariables.newRemoteInstance(Impl.class);
					RMIVariables serverVariables = RMITestUtil.getCorrespondingConnectionVariables(serverConnection,
							clientVariables);

					serverstub = (Stub) serverVariables.newRemoteInstance(Impl.class);
					i = (Impl) RMITestUtil.getRemoteVariablesVariable(serverConnection, s);
					assertNonNull(i);
					for (int j = 0; j < CALL_COUNT; j++) {
						if (callsyncmethodtoo && j % 3 == 0) {
							s.increment();
						} else {
							RMIVariables.invokeRemoteMethodAsync(s, INCREMENT_METHOD);
						}
					}
				} finally {
					clientVariables.close();
					if (waitvars) {
						clientVariables.waitClosure();
						//all the async calls should be finished when the variables is closed
						assertEquals(i.count, CALL_COUNT);
					}
				}
			} finally {
				clientConnection.closeWait();
			}
		} finally {
			serverConnection.closeWait();
		}
		//check that the variables properly closes down, on both sides
		assertException(RMIResourceUnavailableException.class, () -> s.getCount());
		assertException(RMIResourceUnavailableException.class, () -> serverstub.getCount());

		//all the async calls should be finished when the connections are closed
		assertEquals(i.count, CALL_COUNT);
	}

}
