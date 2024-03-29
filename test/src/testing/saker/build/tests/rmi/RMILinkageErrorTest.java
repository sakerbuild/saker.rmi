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
import saker.rmi.exception.RMICallFailedException;
import saker.util.classloader.ClassLoaderResolverRegistry;
import saker.util.classloader.SingleClassLoaderResolver;
import testing.saker.SakerTest;
import testing.saker.build.tests.TestUtils;

@SakerTest
public class RMILinkageErrorTest extends BaseVariablesRMITestCase {
	public interface StubSuper {
		public default String ss() {
			return "ss";
		}
	}

	public interface Stub extends StubSuper {
		public default String s() {
			return "s";
		}

		public default String si(StubImpl sis) {
			return "si";
		}

		public default String sis(StubImplSuper sis) {
			return "sis";
		}

		public default String npi(NotPresentItf npi) {
			return "npi";
		}

		public default String pnpei(PresentNotPresentExtendItf pnpei) {
			return "pnpei";
		}
	}

	public interface NotPresentItf {
	}

	public interface PresentNotPresentExtendItf extends NotPresentItf {
	}

	public static class StubImplSuper {
	}

	public static class StubImpl extends StubImplSuper implements Stub {
	}

	public static class ValidStubImpl implements Stub {

	}

	private ClassLoaderResolverRegistry clientRegistry;
	private ClassLoaderResolverRegistry serverRegistry;

	@Override
	protected void runVariablesTestImpl() throws Exception {
		ClassLoader cl = TestUtils.createClassLoaderForClasses(StubImpl.class, Stub.class, StubSuper.class,
				ValidStubImpl.class, PresentNotPresentExtendItf.class);
		SingleClassLoaderResolver serverresolver = new SingleClassLoaderResolver("cl", cl);
		SingleClassLoaderResolver clientresolver = new SingleClassLoaderResolver("cl",
				this.getClass().getClassLoader());
		clientRegistry.register("cl", clientresolver);
		serverRegistry.register("cl", serverresolver);

		assertException(RMICallFailedException.class, () -> clientVariables.newRemoteInstance(StubImpl.class));

		Stub stub = (Stub) clientVariables.newRemoteInstance(ValidStubImpl.class);
		assertException(RMICallFailedException.class, () -> stub.si(null));
		assertException(RMICallFailedException.class, () -> stub.sis(null));
		assertException(RMICallFailedException.class, () -> stub.npi(null));
		assertException(RMICallFailedException.class, () -> stub.pnpei(null));
	}

	@Override
	protected RMIConnection[] createConnections(RMIOptions baseoptions) throws Exception {
		clientRegistry = new ClassLoaderResolverRegistry();
		serverRegistry = new ClassLoaderResolverRegistry();
		return RMITestUtil.createPipedConnection(
				new RMIOptions(baseoptions).classResolver(clientRegistry)
						.nullClassLoader(RMIConnection.class.getClassLoader()),
				new RMIOptions(baseoptions).classResolver(serverRegistry)
						.nullClassLoader(RMIConnection.class.getClassLoader()));
	}
}
