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
import saker.util.ObjectUtils;
import saker.util.ReflectUtils;
import saker.util.classloader.FilteringClassLoader;
import saker.util.classloader.SingleClassLoaderResolver;
import testing.saker.SakerTest;
import testing.saker.build.tests.TestUtils;

@SakerTest
public class PartiallyFoundInterfaceProxyRMITest extends BaseVariablesRMITestCase {
	private static final ClassLoader SUBCLASSLOADER = TestUtils.createClassLoaderForClasses(
			new FilteringClassLoader(PartiallyFoundInterfaceProxyRMITest.class.getClassLoader(), ObjectUtils
					.newTreeSet(AvailableStub.class.getName(), AvailableStub.AvailableHiddenClass.class.getName())),
			HiddenStub.class, Impl.class);

	public interface AvailableStub {
		public static class AvailableHiddenClass {

		}

		public default void availablef() {
		}

		public default void hiddenparam(AvailableHiddenClass p) {
		}

		public default void hiddenparamOverridden(AvailableHiddenClass p) {
		}
	}

	public interface HiddenStub {

		public default void hiddenf() {
		}

		public default void hiddenstubparam(AvailableStub.AvailableHiddenClass p) {
		}
	}

	public static class Impl implements AvailableStub, HiddenStub {

		@Override
		public void hiddenparamOverridden(AvailableHiddenClass p) {
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Class<?> implclass = SUBCLASSLOADER.loadClass(Impl.class.getName());
		Object s = clientVariables.newRemoteInstance(implclass);
		assertEquals(ReflectUtils.getInterfaces(implclass), ReflectUtils.getInterfaces(s.getClass()));
		assertInstanceOf(s, AvailableStub.class);
		assertInstanceOf(s, SUBCLASSLOADER.loadClass(HiddenStub.class.getName()));
		assertNotInstanceOf(s, HiddenStub.class);

		AvailableStub as = (AvailableStub) s;
		//this call would throw an exception that AvailableStub class is not found when transferring the method to be called
		//    (as the classloader for it is not supported by the set classloader resolver)
		//    this is fixed by trying to find the declaring class relative to the proxy that is the subject of the call
		as.availablef();
		//the parameter type is not directly available through the subclassloader Impl class
		//    it is not in the hierarchy of the impl class
		//    it should be found through the class loader of the method declaring type
		as.hiddenparam(null);
		as.hiddenparamOverridden(null);

		s.getClass().getMethod("availablef").invoke(s);
		s.getClass().getMethod("hiddenf").invoke(s);
		s.getClass().getMethod("hiddenstubparam", AvailableStub.AvailableHiddenClass.class).invoke(s, (Object) null);
	}

	@Override
	protected RMIConnection[] createConnections() throws Exception {
		RMIOptions options = new RMIOptions().classResolver(new SingleClassLoaderResolver("cl", SUBCLASSLOADER));
		return RMITestUtil.createPipedConnection(options);
	}

}
