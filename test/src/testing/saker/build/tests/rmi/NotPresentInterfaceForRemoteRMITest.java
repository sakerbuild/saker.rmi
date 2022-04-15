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

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMITestUtil;
import saker.util.ReflectUtils;
import saker.util.classloader.SingleClassLoaderResolver;
import testing.saker.SakerTest;
import testing.saker.build.tests.TestUtils;

@SakerTest
public class NotPresentInterfaceForRemoteRMITest extends BaseVariablesRMITestCase {
	private static final ClassLoader CLIENTCLASSLOADER = TestUtils.createClassLoaderForClasses(Stub.class, Common.class,
			Impl.class);
	private static final ClassLoader SERVERCLASSLOADER = TestUtils.createClassLoaderForClasses(NotCommonImpl.class,
			NotCommon.class, Stub.class, Common.class, Impl.class);

	public interface Common {
	}

	public interface NotCommon {
	}

	public static class NotCommonImpl implements Stub, NotCommon, Common {
	}

	public interface Stub {
		public default int f() {
			return 99;
		}

		public default Stub makeStub() {
			Stub stub = new NotCommonImpl();
			return stub;
		}
	}

	public static class Impl implements Stub, Common {
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Object s = clientVariables.newRemoteInstance(CLIENTCLASSLOADER.loadClass(Impl.class.getName()));
		assertEquals(s.getClass().getMethod("f").invoke(s), 99);
		Object madestub = s.getClass().getMethod("makeStub").invoke(s);
		//madestub is an instance of NotCommonImpl
		//    however, NotCommon interface is not present in the client classloader, therefore it should not be an instance of if
		//    this test ensures that the transfer succeeds, but with partial interface presenec
		assertInstanceOf(madestub, CLIENTCLASSLOADER.loadClass(Stub.class.getName()));
		assertInstanceOf(madestub, CLIENTCLASSLOADER.loadClass(Common.class.getName()));
		assertEquals(ReflectUtils.getInterfaces(madestub.getClass()).size(), 2);
	}

	@Override
	protected RMIConnection[] createConnections(RMIOptions baseoptions) throws Exception {
		return RMITestUtil.createPipedConnection(
				new RMIOptions(baseoptions).nullClassLoader(null)
						.classResolver(new SingleClassLoaderResolver("cl", CLIENTCLASSLOADER)),
				new RMIOptions(baseoptions).nullClassLoader(null)
						.classResolver(new SingleClassLoaderResolver("cl", SERVERCLASSLOADER)));
	}

}
