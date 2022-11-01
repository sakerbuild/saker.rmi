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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import saker.rmi.annot.transfer.RMISerialize;
import saker.rmi.annot.transfer.RMIWrap;
import saker.rmi.connection.RMIConnection;
import saker.rmi.exception.RMICallFailedException;
import saker.rmi.io.RMIObjectInput;
import saker.rmi.io.RMIObjectOutput;
import saker.rmi.io.wrap.RMIWrapper;
import testing.saker.SakerTest;

@SakerTest
public class RMITransferTest extends BaseVariablesRMITestCase {
	public static final int FIELD = 0;

	public static class MyExternalizable implements Externalizable {
		private static final long serialVersionUID = 1L;

		protected String value;

		public MyExternalizable() {
		}

		public MyExternalizable(String value) {
			this.value = value;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeUTF(value);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			value = in.readUTF();
		}
	}

	public static class MySerializable implements Serializable {
		private static final long serialVersionUID = 1L;
		protected String value;

		public MySerializable(String value) {
			this.value = value;
		}
	}

	@RMIWrap(MyWrappableWrapper.class)
	public static class MyWrappable {
		public String v;

		public MyWrappable(String v) {
			this.v = v;
		}
	}

	public static class MyWrappableWrapper implements RMIWrapper {
		private MyWrappable obj;

		public MyWrappableWrapper() {
		}

		public MyWrappableWrapper(MyWrappable obj) {
			this.obj = obj;
		}

		@Override
		public void writeWrapped(RMIObjectOutput out) throws IOException {
			out.writeWrappedObject(obj, MyWrappableRedirectWrapper.class);
		}

		@Override
		public void readWrapped(RMIObjectInput in) throws IOException, ClassNotFoundException {
			obj = (MyWrappable) in.readObject();
		}

		@Override
		public Object resolveWrapped() {
			return obj;
		}

		@Override
		public Object getWrappedObject() {
			throw new UnsupportedOperationException();
		}
	}

	public static class MyWrappableRedirectWrapper implements RMIWrapper {
		private MyWrappable obj;

		public MyWrappableRedirectWrapper() {
		}

		public MyWrappableRedirectWrapper(MyWrappable obj) {
			this.obj = obj;
		}

		@Override
		public void writeWrapped(RMIObjectOutput out) throws IOException {
			out.writeUTF(obj.v + "_wrapped");
		}

		@Override
		public void readWrapped(RMIObjectInput in) throws IOException, ClassNotFoundException {
			obj = new MyWrappable(in.readUTF());
		}

		@Override
		public Object resolveWrapped() {
			return obj;
		}

		@Override
		public Object getWrappedObject() {
			throw new UnsupportedOperationException();
		}

	}

	public interface Stub {
		public byte[] f(byte[] b);

		public short[] f(short[] b);

		public int[] f(int[] b);

		public long[] f(long[] b);

		public float[] f(float[] b);

		public double[] f(double[] b);

		public boolean[] f(boolean[] b);

		public char[] f(char[] b);

		public byte f(byte b);

		public short f(short b);

		public int f(int b);

		public long f(long b);

		public float f(float b);

		public double f(double b);

		public boolean f(boolean b);

		public char f(char b);

		public String f(String b);

		public Object[] f(Object[] b);

		public Runnable defaultRemote(Runnable runnable);

		public ClassLoader cl(ClassLoader cl);

		public <C> Class<C> clazz(Class<C> c);

		public Method method(Method m);

		public <C> Constructor<C> constructor(Constructor<C> c);

		public Field field(Field f);

		public default String passThroughString(String s) {
			return s;
		}

		public default MyExternalizable myExternalizable() {
			return new MyExternalizable("test");
		}

		//as the return type is an interface it should be transferred as a remote object, even though it is externalizable
		public default Externalizable externalizableItf() {
			return new MyExternalizable("test");
		}

		@RMISerialize
		public default MySerializable serializable() {
			return new MySerializable("test");
		}

		public default MyWrappable getWrappable() {
			return new MyWrappable("val");
		}

		@RMIWrap(MissingConstructorRMIWrapper.class)
		public default Object getMissingConstructorRMIWrapper() {
			return new MySerializable("123");
		}

		public default void redispatch(Runnable run) {
			run.run();
		}
	}

	public static class Impl implements Stub {

		@Override
		public byte[] f(byte[] b) {
			return b;
		}

		@Override
		public short[] f(short[] b) {
			return b;
		}

		@Override
		public int[] f(int[] b) {
			return b;
		}

		@Override
		public long[] f(long[] b) {
			return b;
		}

		@Override
		public float[] f(float[] b) {
			return b;
		}

		@Override
		public double[] f(double[] b) {
			return b;
		}

		@Override
		public boolean[] f(boolean[] b) {
			return b;
		}

		@Override
		public char[] f(char[] b) {
			return b;
		}

		@Override
		public byte f(byte b) {
			return b;
		}

		@Override
		public short f(short b) {
			return b;
		}

		@Override
		public int f(int b) {
			return b;
		}

		@Override
		public long f(long b) {
			return b;
		}

		@Override
		public float f(float b) {
			return b;
		}

		@Override
		public double f(double b) {
			return b;
		}

		@Override
		public boolean f(boolean b) {
			return b;
		}

		@Override
		public char f(char b) {
			return b;
		}

		@Override
		public String f(String b) {
			return b;
		}

		@Override
		public Object[] f(Object[] b) {
			return b;
		}

		@Override
		public Runnable defaultRemote(Runnable runnable) {
			return runnable;
		}

		@Override
		public ClassLoader cl(ClassLoader cl) {
			return cl;
		}

		@Override
		public <C> Class<C> clazz(Class<C> c) {
			return c;
		}

		@Override
		public Method method(Method m) {
			return m;
		}

		@Override
		public <C> Constructor<C> constructor(Constructor<C> c) {
			return c;
		}

		@Override
		public Field field(Field f) {
			return f;
		}
	}

	/**
	 * No constructor that accepts an object.
	 */
	public static class MissingConstructorRMIWrapper implements RMIWrapper {
		private Object object;

		public MissingConstructorRMIWrapper() {
		}

		@Override
		public void writeWrapped(RMIObjectOutput out) throws IOException {
			out.writeObject(object);
		}

		@Override
		public void readWrapped(RMIObjectInput in) throws IOException, ClassNotFoundException {
			object = in.readObject();
		}

		@Override
		public Object resolveWrapped() {
			return object;
		}

		@Override
		public Object getWrappedObject() {
			return null;
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);

		assertEquals(s.f(true), true);
		assertEquals(s.f((byte) 1), (byte) 1);
		assertEquals(s.f((short) 1), (short) 1);
		assertEquals(s.f(1), 1);
		assertEquals(s.f((long) 1), (long) 1);
		assertEquals(s.f((float) 1), (float) 1);
		assertEquals(s.f((double) 1), (double) 1);
		assertEquals(s.f((char) 1), (char) 1);
		assertEquals(s.f("1"), "1");

		assertEquals(s.f(new byte[] { 1 }), new byte[] { 1 });
		assertEquals(s.f(new short[] { 1 }), new short[] { 1 });
		assertEquals(s.f(new int[] { 1 }), new int[] { 1 });
		assertEquals(s.f(new long[] { 1 }), new long[] { 1 });
		assertEquals(s.f(new float[] { 1 }), new float[] { 1 });
		assertEquals(s.f(new double[] { 1 }), new double[] { 1 });
		assertEquals(s.f(new char[] { 1 }), new char[] { 1 });
		assertEquals(s.f(new boolean[] { true }), new boolean[] { true });

		assertEquals(s.f(new String[] { "s" }), new String[] { "s" });

		Runnable r = () -> {
		};
		assertTrue(s.defaultRemote(r) == r);

		Class<RMITransferTest> clazz = RMITransferTest.class;
		Constructor<RMITransferTest> constr = RMITransferTest.class.getConstructor();
		Method meth = RMITransferTest.class.getMethods()[0];
		ClassLoader cl = RMITransferTest.class.getClassLoader();
		Field f = RMITransferTest.class.getFields()[0];

		assertIdentityEquals(clazz, s.clazz(clazz));
		assertIdentityEquals(cl, s.cl(cl));
		assertEquals(constr, s.constructor(constr));
		assertEquals(meth, s.method(meth));
		assertEquals(f, s.field(f));

		assertEquals(s.myExternalizable().value, "test");
		assertEquals(s.serializable().value, "test");
		assertTrue(RMIConnection.isRemoteObject(s.externalizableItf()));

		assertEquals(s.getWrappable().v, "val_wrapped");

		assertException(RMICallFailedException.class, () -> s.getMissingConstructorRMIWrapper());

		//there was a bug that closed down the RMI connection after the above missing constructor RMI wrapper check
		//so check again that it still works
		assertEquals(s.passThroughString("abc"), "abc");

		{
			RuntimeException thrownexc = assertException(RuntimeException.class, () -> s.redispatch(() -> {
				throw new RuntimeException("dummy");
			}));
			try {
				assertEquals(thrownexc.getClass(), RuntimeException.class);
				assertEquals(thrownexc.getMessage(), "dummy");
			} catch (Throwable e) {
				e.addSuppressed(thrownexc);
				throw e;
			}
		}

		assertException(RMICallFailedException.class, () -> s.redispatch(() -> s.getMissingConstructorRMIWrapper()));

		//check again that connection still works
		assertEquals(s.passThroughString("xyz"), "xyz");
	}

}
