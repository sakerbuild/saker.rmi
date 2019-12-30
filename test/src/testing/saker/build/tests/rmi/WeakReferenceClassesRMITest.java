package testing.saker.build.tests.rmi;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMIVariables;
import saker.util.classloader.ClassLoaderResolver;
import testing.saker.SakerTest;
import testing.saker.build.tests.TestUtils;

@SakerTest
public class WeakReferenceClassesRMITest extends BaseRMITestCase {
	private static final int GC_TIMEOUT_MS = 50;

	public interface Stub {
		public String f(String s);
	}

	public static class Impl implements Stub {
		@Override
		public String f(String s) {
			return s + s;
		}
	}

	private volatile ClassLoader cl = TestUtils.createClassLoaderForClasses(Stub.class, Impl.class);

	ClassLoader getClassloader() {
		return cl;
	}

	@Override
	protected void runTestImpl() throws Exception {
		Thread currentthread = Thread.currentThread();
		WeakReference<Class<?>> wref = execute();
		//null out any outstanding references to the classloader or the classes
		this.cl = null;
		for (int i = 0; i < 1000 / GC_TIMEOUT_MS; i++) {
			System.gc();
			System.runFinalization();
			if (wref.get() == null) {
				return;
			}
			Thread.sleep(GC_TIMEOUT_MS);
			if (currentthread.isInterrupted()) {
				throw new InterruptedException();
			}
		}
		fail("Failed to garbage collect class");
	}

	private WeakReference<Class<?>> execute() throws Exception {
		Class<?> implclass = cl.loadClass(Impl.class.getName());
		try (RMIVariables cv = clientConnection.newVariables()) {
			Object impl = cv.newRemoteInstance(implclass);
			Method m = impl.getClass().getMethod("f", String.class);
			assertEquals(m.invoke(impl, "x"), "xx");
		}
		return new WeakReference<Class<?>>(implclass);
	}

	@Override
	protected RMIConnection[] createConnections() throws Exception {
		ClassLoaderResolver resolver = new ClassLoaderResolver() {
			@Override
			public String getClassLoaderIdentifier(ClassLoader classloader) {
				if (classloader == WeakReferenceClassesRMITest.this.getClassloader()) {
					return "cl";
				}
				return null;
			}

			@Override
			public ClassLoader getClassLoaderForIdentifier(String identifier) {
				if ("cl".equals(identifier)) {
					return WeakReferenceClassesRMITest.this.getClassloader();
				}
				return null;
			}
		};
		return RMITestUtil.createPipedConnection(
				new RMIOptions().classResolver(resolver).nullClassLoader(RMIConnection.class.getClassLoader()));
	}
}
