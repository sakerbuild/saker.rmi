package testing.saker.build.tests.rmi;

import java.io.EOFException;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.rmi.exception.RMICallFailedException;
import testing.saker.SakerTest;

@SakerTest
public class RMIExternalizeLimitsTest extends BaseVariablesRMITestCase {

	private static int setVal1 = 0;
	private static int setVal2 = 0;
	private static String stringSet = null;

	public static abstract class Base implements Externalizable, Runnable {
		private static final long serialVersionUID = 1L;

		protected int i1;
		protected int i2;

		public Base() {
		}

		public Base(int i1, int i2) {
			this.i1 = i1;
			this.i2 = i2;
		}

		@Override
		public void run() {
			setVal1 = i1;
			setVal2 = i2;
		}
	}

	public interface Stub {
		public void test(Base b, Object dummy);
	}

	public static class Impl implements Stub {

		@Override
		public void test(Base b, Object dummy) {
			stringSet = dummy.toString();
			b.run();
		}

	}

	public static class EOFReadFailBase extends Base {
		private static final long serialVersionUID = 1L;

		public EOFReadFailBase() {
		}

		public EOFReadFailBase(int i1, int i2) {
			super(i1, i2);
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeInt(i1);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			i1 = in.readInt();
			try {
				i2 = in.readInt();
				fail();
			} catch (EOFException e) {
				throw e;
			}
		}
	}

	public static class EOFReadSucceedBase extends Base {
		private static final long serialVersionUID = 1L;

		public EOFReadSucceedBase() {
			super();
		}

		public EOFReadSucceedBase(int i1, int i2) {
			super(i1, i2);
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeInt(i1);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			i1 = in.readInt();
			try {
				i2 = in.readInt();
				fail();
			} catch (EOFException e) {
			}
		}
	}

	public static class NotFullyReadBase extends Base {
		private static final long serialVersionUID = 1L;

		public NotFullyReadBase() {
			super();
		}

		public NotFullyReadBase(int i1, int i2) {
			super(i1, i2);
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeInt(i1);
			out.writeInt(i2);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			i1 = in.readInt();
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		setVal1 = 0;
		setVal2 = 0;
		stringSet = null;

		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
		assertException(RMICallFailedException.class, () -> s.test(new EOFReadFailBase(1, 2), "dummy1"));
		assertEquals(setVal1, 0);
		assertEquals(setVal2, 0);
		assertEquals(stringSet, null);

		s.test(new EOFReadSucceedBase(3, 4), "dummy2");
		assertEquals(setVal1, 3);
		assertEquals(setVal2, 0);
		assertEquals(stringSet, "dummy2");

		assertException(RMICallFailedException.class, () -> s.test(new NotFullyReadBase(5, 6), "dummy3"));
	}

}
