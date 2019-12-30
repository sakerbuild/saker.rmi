package testing.saker.build.tests.rmi;

import java.lang.reflect.Method;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.exception.RMICallFailedException;
import saker.util.classloader.ClassLoaderResolverRegistry;
import saker.util.classloader.SingleClassLoaderResolver;
import testing.saker.SakerTest;
import testing.saker.build.tests.TestUtils;

@SakerTest
public class RMIClassLookupTest extends BaseVariablesRMITestCase {
	public interface Stub {
		public String f(String s);
	}

	public static class Impl implements Stub {
		static int callCount = 0;

		@Override
		public String f(String s) {
			callCount++;
			return s + s;
		}
	}

	public static class Impl2 implements Stub {
		static int callCount = 0;

		@Override
		public String f(String s) {
			callCount++;
			return s + "2";
		}
	}

	public static class ImplSub implements Stub {
		static int callCount = 0;

		@Override
		public String f(String s) {
			callCount++;
			return s + "sub";
		}
	}

	private ClassLoaderResolverRegistry clientRegistry = new ClassLoaderResolverRegistry();
	private ClassLoaderResolverRegistry serverRegistry = new ClassLoaderResolverRegistry();

	@Override
	protected void runVariablesTestImpl() throws Exception {
		//dynamically modify the lookup classloaders
		assertException(RMICallFailedException.class, () -> clientVariables.newRemoteInstance(Impl.class));
		assertException(RMICallFailedException.class,
				() -> clientVariables.newRemoteOnlyInstance(Impl.class.getName()));

		ClassLoader clientcl1 = TestUtils.createClassLoaderForClasses(Stub.class, Impl.class);
		ClassLoader servercl1 = TestUtils.createClassLoaderForClasses(Stub.class, Impl.class);
		SingleClassLoaderResolver resolver1 = new SingleClassLoaderResolver("cl1", clientcl1);
		SingleClassLoaderResolver resolver2 = new SingleClassLoaderResolver("cl1", servercl1);
		clientRegistry.register("cl1", resolver1);
		serverRegistry.register("cl1", resolver2);
		{
			Object s = clientVariables.newRemoteInstance(clientcl1.loadClass(Impl.class.getName()));
			Method m = s.getClass().getMethod("f", String.class);
			assertEquals(m.invoke(s, "x"), "xx");
			assertEquals(Impl.callCount, 0);
		}

		ClassLoader clientsubcl = TestUtils.createClassLoaderForClasses(clientcl1, Impl2.class, ImplSub.class);
		ClassLoader serversubcl = TestUtils.createClassLoaderForClasses(servercl1, Impl2.class, ImplSub.class);
		SingleClassLoaderResolver clientsubclresolver = new SingleClassLoaderResolver("subcl", clientsubcl);
		SingleClassLoaderResolver serversubclresolver = new SingleClassLoaderResolver("subcl", serversubcl);
		clientRegistry.register("subcl", clientsubclresolver);
		serverRegistry.register("subcl", serversubclresolver);

		{
			Object s = clientVariables.newRemoteInstance(clientsubcl.loadClass(Impl2.class.getName()));
			Method m = s.getClass().getMethod("f", String.class);
			assertEquals(m.invoke(s, "x"), "x2");
			assertEquals(Impl2.callCount, 0);
		}

		clientRegistry.register("thiscl",
				new SingleClassLoaderResolver("thiscl", RMIClassLookupTest.class.getClassLoader()));
		serverRegistry.register("thiscl",
				new SingleClassLoaderResolver("thiscl", RMIClassLookupTest.class.getClassLoader()));

		{
			Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
			assertEquals(s.f("x"), "xx");
			assertEquals(Impl.callCount, 1);
		}

		clientRegistry.unregister("subcl", clientsubclresolver);
		serverRegistry.unregister("subcl", serversubclresolver);

		//after unregistering the sub classloader, only classes which have already been referenced should be available
		//therefore Impl2 succeeds, but ImplSub not
		{
			Object s = clientVariables.newRemoteInstance(clientsubcl.loadClass(Impl2.class.getName()));
			Method m = s.getClass().getMethod("f", String.class);
			assertEquals(m.invoke(s, "x"), "x2");
			assertEquals(Impl2.callCount, 0);

			assertException(RMICallFailedException.class,
					() -> clientVariables.newRemoteInstance(clientsubcl.loadClass(ImplSub.class.getName())));
		}
	}

	@Override
	protected RMIConnection[] createConnections() throws Exception {
		return RMITestUtil.createPipedConnection(
				new RMIOptions().classResolver(clientRegistry).nullClassLoader(RMIConnection.class.getClassLoader()),
				new RMIOptions().classResolver(serverRegistry).nullClassLoader(RMIConnection.class.getClassLoader()));
	}
}
