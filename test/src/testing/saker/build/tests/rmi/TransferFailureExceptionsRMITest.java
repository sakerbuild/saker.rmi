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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import saker.rmi.connection.MethodTransferProperties;
import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMITransferProperties;
import saker.rmi.exception.RMICallFailedException;
import saker.rmi.exception.RMIObjectTransferFailureException;
import saker.rmi.io.writer.RMIObjectWriteHandler;
import saker.util.classloader.SingleClassLoaderResolver;
import testing.saker.SakerTest;
import testing.saker.build.tests.TestUtils;

@SakerTest
public class TransferFailureExceptionsRMITest extends BaseVariablesRMITestCase {

	private static final ClassLoader SUBCLASSLOADER = TestUtils.createClassLoaderForClasses(Stub.class, Impl.class,
			NonReadableExternalizable.class, NonWriteableExternalizable.class, CustomException.class,
			NonReadableCustomException.class, NonWritableCustomException.class);

	public interface Stub {
		public default void f(Object o) {
		}

		public default void customException() {
			throw new CustomException();
		}

		public default void nonReadableCustomException() {
			throw new NonReadableCustomException();
		}

		public default void nonWritableCustomException() {
			throw new NonWritableCustomException();
		}
	}

	public static class CustomException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	public static class NonReadableCustomException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
			throw new IOException("Can't read.");
		}
	}

	public static class NonWritableCustomException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private void writeObject(java.io.ObjectOutputStream out) throws IOException {
			throw new IOException("Can't read.");
		}
	}

	public static class Impl implements Stub {
	}

	public static class NonReadableExternalizable implements Externalizable {
		private static final long serialVersionUID = 1L;

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			throw new IOException("Can't read.");
		}
	}

	public static class NonWriteableExternalizable implements Externalizable {
		private static final long serialVersionUID = 1L;

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			throw new IOException("Can't write.");
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		}
	}

	public static class OnlyLocal implements Serializable {
		private static final long serialVersionUID = 1L;
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Class<?> implclass = SUBCLASSLOADER.loadClass(Impl.class.getName());
		Object s = clientVariables.newRemoteInstance(implclass);
		Class<? extends Object> stubclass = s.getClass();
		Method m = stubclass.getMethod("f", Object.class);
		try {
			//the class will not be found on the other side
			m.invoke(s, new OnlyLocal());
			fail();
		} catch (InvocationTargetException e) {
			Class<? extends Throwable> causeclass = e.getCause().getClass();
			assertTrue(causeclass == RMIObjectTransferFailureException.class
					|| causeclass == RMICallFailedException.class);
		}
		try {
			m.invoke(s, new NonReadableExternalizable());
			fail();
		} catch (InvocationTargetException e) {
			Class<? extends Throwable> causeclass = e.getCause().getClass();
			assertTrue(causeclass == RMIObjectTransferFailureException.class
					|| causeclass == RMICallFailedException.class);
		}
		try {
			m.invoke(s, new NonWriteableExternalizable());
			fail();
		} catch (InvocationTargetException e) {
			Class<? extends Throwable> causeclass = e.getCause().getClass();
			assertTrue(causeclass == RMIObjectTransferFailureException.class);
		}
		try {
			stubclass.getMethod("customException").invoke(s);
			fail();
		} catch (InvocationTargetException e) {
			Class<? extends Throwable> causeclass = e.getCause().getClass();
			assertTrue(causeclass == SUBCLASSLOADER.loadClass(CustomException.class.getName()));
		}
		try {
			stubclass.getMethod("nonWritableCustomException").invoke(s);
			fail();
		} catch (InvocationTargetException e) {
			Class<? extends Throwable> causeclass = e.getCause().getClass();
			assertTrue(causeclass == RMICallFailedException.class);
		}
		try {
			stubclass.getMethod("nonReadableCustomException").invoke(s);
			fail();
		} catch (InvocationTargetException e) {
			Class<? extends Throwable> causeclass = e.getCause().getClass();
			assertTrue(causeclass == RMICallFailedException.class);
		}
	}

	@Override
	protected RMIConnection[] createConnections() throws Exception {
		//set the proper transfer properties for the method, as the annotation classes will not be available for the classes, therefore they are not visible
		Class<?> stubclass = SUBCLASSLOADER.loadClass(Stub.class.getName());
		Method m = stubclass.getMethod("f", Object.class);
		RMITransferProperties.Builder builder = RMITransferProperties.builder();
		builder.add(MethodTransferProperties.builder(m).parameterWriter(0, RMIObjectWriteHandler.serialize()).build());
		return RMITestUtil.createPipedConnection(
				new RMIOptions().classResolver(new SingleClassLoaderResolver("cl", SUBCLASSLOADER))
						.transferProperties(builder.build()));
	}
}
