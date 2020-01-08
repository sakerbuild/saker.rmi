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

import java.lang.reflect.Method;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMIVariables;
import saker.rmi.exception.RMICallForbiddenException;
import saker.util.ReflectUtils;
import testing.saker.SakerTest;

@SakerTest
public class ForbiddenDirectCallsRMITest extends BaseVariablesRMITestCase {

	public interface Stub {
		public default String f() {
			return "f";
		}
	}

	public static class Impl implements Stub {
	}

	public static String staticfunc() {
		return "";
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		String CONTEXT_VAR_NAME = "contextvar";
		serverConnection.putContextVariable(CONTEXT_VAR_NAME, new Impl());
		Method staticmethod = ReflectUtils.getMethodAssert(ForbiddenDirectCallsRMITest.class, "staticfunc");

		assertException(RMICallForbiddenException.class, () -> clientVariables.newRemoteInstance(Impl.class));
		assertException(RMICallForbiddenException.class,
				() -> clientVariables.newRemoteOnlyInstance(Impl.class.getName()));
		assertException(RMICallForbiddenException.class, () -> clientVariables.invokeRemoteStaticMethod(staticmethod));
		Stub implvar = (Stub) clientVariables.getRemoteContextVariable(CONTEXT_VAR_NAME);
		assertException(RMICallForbiddenException.class, () -> RMIVariables.invokeMethod(implvar, staticmethod));
		assertException(RMICallForbiddenException.class, () -> RMIVariables.invokeRemoteMethod(implvar, staticmethod));
		System.out.println("ForbiddenDirectCallsRMITest.runVariablesTestImpl() " + implvar.toString());
		assertEquals(implvar.f(), "f");
	}

	@Override
	protected RMIConnection[] createConnections(int maxthreads) throws Exception {
		return RMITestUtil.createPipedConnection(new RMIOptions().classLoader(getClass().getClassLoader())
				.allowDirectRequests(false).maxStreamCount(maxthreads));
	}
}
