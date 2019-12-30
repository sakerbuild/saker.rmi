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
	protected RMIConnection[] createConnections() throws Exception {
		return RMITestUtil.createPipedConnection(
				new RMIOptions().classResolver(new SingleClassLoaderResolver("cl", CLIENTCLASSLOADER)),
				new RMIOptions().classResolver(new SingleClassLoaderResolver("cl", SERVERCLASSLOADER)));
	}

}
