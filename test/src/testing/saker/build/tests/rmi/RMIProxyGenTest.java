package testing.saker.build.tests.rmi;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import saker.rmi.annot.invoke.RMICacheResult;
import saker.rmi.annot.invoke.RMIDefaultOnFailure;
import saker.rmi.annot.invoke.RMIForbidden;
import saker.rmi.annot.transfer.RMIRemote;
import saker.rmi.annot.transfer.RMISerialize;
import saker.rmi.annot.transfer.RMIWriter;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.exception.RMIInvalidConfigurationException;
import saker.rmi.exception.RMIProxyCreationFailedException;
import saker.rmi.io.writer.RemoteOnlyRMIObjectWriteHandler;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class RMIProxyGenTest extends SakerTestCase {

	public interface IncompRet1 {
		public int f();
	}

	public interface IncompRet2 {
		public long f();
	}

	public interface DefItf1 {
		@RMIDefaultOnFailure
		public default void f() {

		}
	}

	public interface DefItf2 {
		@RMIDefaultOnFailure
		public default void f() {

		}
	}

	public interface ConflictItf1 {
		@RMIDefaultOnFailure
		public default void f() {

		}
	}

	public interface ConflictItf2 {
		public void f();
	}

	public interface SubDefItf1NoDefault extends DefItf1 {
		@Override
		public void f();
	}

	public interface ConflictResolve extends ConflictItf1, ConflictItf2 {
		@Override
		public void f();
	}

	public interface AnnotConflict1 {
		@RMIRemote
		public Object f();
	}

	public interface AnnotConflict2 {
		@RMISerialize
		public Object f();
	}

	public interface AnnotConflictUnresolved extends AnnotConflict1, AnnotConflict2 {
	}

	public interface AnnotConflictResolved extends AnnotConflict1, AnnotConflict2 {
		@Override
		@RMIWriter(RemoteOnlyRMIObjectWriteHandler.class)
		public Object f();
	}

	public interface MethodTypeConflict1 {
		public Object f();
	}

	public interface MethodTypeConflict2 {
		public String f();
	}

	public interface MethodTypeConflictResolve extends MethodTypeConflict1, MethodTypeConflict2 {
		@Override
		String toString();
	}

	public interface SuperFunc {
		public void f();
	}

	public interface SubFunc extends SuperFunc {
		@Override
		public void f();
	}

	private static class DefinerClassLoader extends ClassLoader {
		public DefinerClassLoader() {
			super(DefinerClassLoader.class.getClassLoader());
		}

		public Class<?> defineClass(String name, byte[] data) {
			return defineClass(name, data, 0, data.length);
		}
	}

	public interface CacheVoid {
		@RMICacheResult
		public void f();
	}

	public interface CacheForbidden {
		@RMICacheResult
		@RMIForbidden
		public int f();
	}

	private DefinerClassLoader classLoader = new DefinerClassLoader();
	private final AtomicInteger nameCounter = new AtomicInteger();

	@Override
	public void runTest(Map<String, String> parameters) {
		//both implement default error
		assertException(RMIProxyCreationFailedException.class, () -> testCreate(setOf(DefItf1.class, DefItf2.class)));

		//different annotations on methods (by default on failure call)
		assertException(RMIProxyCreationFailedException.class,
				() -> testCreate(setOf(ConflictItf1.class, ConflictItf2.class)));
		//resolution of previous error
		testCreate(setOf(ConflictResolve.class));
		testCreate(setOf(ConflictResolve.class, SubDefItf1NoDefault.class));

		//incompatible return types error test
		assertException(RMIProxyCreationFailedException.class,
				() -> testCreate(setOf(IncompRet1.class, IncompRet2.class)));

		//test of sub return type functions
		testCreate(setOf(MethodTypeConflict1.class, MethodTypeConflict2.class));
		testCreate(setOf(MethodTypeConflictResolve.class));

		testCreate(setOf(SubFunc.class));

		assertException(RMIInvalidConfigurationException.class, () -> testCreate(setOf(CacheVoid.class)));
		assertException(RMIInvalidConfigurationException.class, () -> testCreate(setOf(CacheForbidden.class)));

		//conflicting method configurations are unresolved
		assertException(RMIProxyCreationFailedException.class, () -> testCreate(setOf(AnnotConflictUnresolved.class)));
		testCreate(setOf(AnnotConflictResolved.class));
	}

	private void testCreate(Set<Class<?>> interfaces) {
		String name = "class" + nameCounter.getAndIncrement();
		Class<?> c = classLoader.defineClass(name, RMITestUtil.testProxyCreation(name, interfaces));

		//calling these might trigger some verification
		c.getDeclaredMethods();
		c.getMethods();
		c.getConstructors();
		c.getDeclaredConstructors();
	}

}
