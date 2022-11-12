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

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMIVariables;
import saker.util.ReflectUtils;
import saker.util.io.ResourceCloser;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;
import testing.saker.build.tests.ExecutionOrderer;

@SakerTest
public class RMIAsyncMethodInvokeTest extends SakerTestCase {
	private static ExecutionOrderer orderer;

	public interface Stub {
		public void f() throws InterruptedException;
	}

	public static class Impl implements Stub {
		@Override
		public void f() throws InterruptedException {
			System.out.println("RMIAsyncMethodInvokeTest.Impl.f()");
			orderer.enter("f");
		}
	}

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		runTestWithProtocolVersion((short) RMIConnection.PROTOCOL_VERSION_1);
		runTestWithProtocolVersion((short) RMIConnection.PROTOCOL_VERSION_2);
	}

	@SuppressWarnings("try") // unused ResourceCloser
	private void runTestWithProtocolVersion(short protocolversion) throws Exception, IOException,
			InvocationTargetException, NoSuchMethodException, AssertionError, InterruptedException {
		RMIOptions baseoptions = new RMIOptions().maxStreamCount(1).classLoader(getClass().getClassLoader());
		RMIConnection[] connections = RMITestUtil.createPipedConnection(baseoptions, baseoptions, protocolversion);
		RMIConnection clientConnection = connections[0];
		RMIConnection serverConnection = connections[1];
		try (ResourceCloser closer = new ResourceCloser(clientConnection::closeWait, serverConnection::closeWait);
				RMIVariables clientVariables = clientConnection.newVariables()) {
			runTest(clientVariables);
		}
	}

	private static void runTest(RMIVariables clientvars)
			throws InvocationTargetException, NoSuchMethodException, AssertionError, InterruptedException {
		orderer = new ExecutionOrderer();
		orderer.addSection("call");
		orderer.addSection("endcall");
		orderer.addSection("f");
		orderer.addSection("done");

		Stub s = (Stub) clientvars.newRemoteInstance(Impl.class);
		Method fmethod = ReflectUtils.getMethodAssert(Stub.class, "f");
		orderer.enter("call");
		RMIVariables.invokeRemoteMethodAsync(s, fmethod);
		orderer.enter("endcall");
		orderer.enter("done");
	}

}
