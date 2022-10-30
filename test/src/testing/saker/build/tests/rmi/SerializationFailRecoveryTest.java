package testing.saker.build.tests.rmi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.WriteAbortedException;

import saker.rmi.annot.transfer.RMIWrap;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.io.RMIObjectInput;
import saker.rmi.io.RMIObjectOutput;
import saker.rmi.io.wrap.RMIWrapper;
import testing.saker.SakerTest;

@SakerTest
public class SerializationFailRecoveryTest extends BaseVariablesRMITestCase {

	public interface Stub {
		@RMIWrap(RecoveryRMIWrapper.class)
		public default Object get() {
			return new Object();
		}

		public default MyExternalizable getExternalizable() {
			return new MyExternalizable(new MyNotSerializableException());
		}
	}

	public static class Impl implements Stub {
		protected Object obj;

		@Override
		public Object get() {
			return obj;
		}
	}

	private static class MyNotSerializableException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private final Object obj = new Object();
	}

	private static class FailIOException extends IOException {
		private static final long serialVersionUID = 1L;

		public FailIOException(String message) {
			super(message);
		}
	}

	public static class FailExternalizable implements Externalizable {
		private static final long serialVersionUID = 1L;

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			throw new FailIOException("write");
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			throw new FailIOException("read");
		}

	}

	public static class MyExternalizable implements Externalizable {
		private static final long serialVersionUID = 1L;

		private Object object;
		private Object next;

		public MyExternalizable() {
		}

		public MyExternalizable(Object object) {
			this.object = object;
			next = new FailExternalizable();
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			System.out.println("SerializationFailRecoveryTest.MyExternalizable.writeExternal() enter");
			try {
				out.writeObject(object);
			} catch (NotSerializableException e) {
				System.out.println("SerializationFailRecoveryTest.MyExternalizable.writeExternal(): " + e);
				out.writeObject("xyz");
			}
			try {
				out.writeObject(next);
			} catch (FailIOException e) {
				// expected
				System.out.println("SerializationFailRecoveryTest.MyExternalizable.writeExternal(): next: " + e);
				out.writeObject("ijk");
			}
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			System.out.println("SerializationFailRecoveryTest.MyExternalizable.readExternal() enter");
			try {
				in.readObject();
			} catch (WriteAbortedException e) {
				//expected exception
				System.out.println("Got " + e);
				//writing and reading after an exception should succeed
				Object readafter = in.readObject();

				object = WriteAbortedException.class.getName() + readafter;
			}
			try {
				next = in.readObject();
			} catch (FailIOException e) {
				//expected
				System.out.println("Got " + e);

				assertEquals(e.getMessage(), "read");
				Object readafter = in.readObject();
				next = FailIOException.class.getName() + readafter;
			}
		}

	}

	public static class FailWrapper implements RMIWrapper {
		public FailWrapper() {
		}

		public FailWrapper(Object o) {
		}

		@Override
		public void writeWrapped(RMIObjectOutput out) throws IOException {
			throw new FailIOException("write");
		}

		@Override
		public void readWrapped(RMIObjectInput in) throws IOException, ClassNotFoundException {
			throw new FailIOException("read");
		}

		@Override
		public Object resolveWrapped() {
			return null;
		}

		@Override
		public Object getWrappedObject() {
			return null;
		}

	}

	public static class RecoveryRMIWrapper implements RMIWrapper {
		private Object object;

		public RecoveryRMIWrapper() {
		}

		public RecoveryRMIWrapper(Object object) {
			this.object = object;
		}

		@Override
		public void writeWrapped(RMIObjectOutput out) throws IOException {
			try {
				out.writeSerializedObject(object);
			} catch (IOException e) {
				System.out.println("SerializationFailRecoveryTest.RecoveryRMIWrapper.writeWrapped(): " + e);
				//writing after an exception should succeed
				out.writeObject("abc");
			}

			try {
				out.writeWrappedObject(new Object(), FailWrapper.class);
			} catch (FailIOException e) {
				System.out.println("SerializationFailRecoveryTest.RecoveryRMIWrapper.writeWrapped() failer: " + e);
				out.writeObject("efg");
			}
		}

		@Override
		public void readWrapped(RMIObjectInput in) throws IOException, ClassNotFoundException {
			try {
				object = in.readObject();
			} catch (WriteAbortedException e) {
				//expected exception
				System.out.println("Got " + e);
				//writing and reading after an exception should succeed
				Object readafter = in.readObject();

				object = WriteAbortedException.class.getName() + readafter;
			}
			try {
				in.readObject();
			} catch (FailIOException e) {
				System.out.println("Got failer: " + e);

				Object readafter = in.readObject();
				object = object + FailIOException.class.getName() + readafter;
			}
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
		Impl i = (Impl) RMITestUtil.getRemoteVariablesVariable(serverConnection, s);

		//not serializable
		i.obj = new Object();
		assertEquals(s.get(), WriteAbortedException.class.getName() + "abc" + FailIOException.class.getName() + "efg");

		MyExternalizable gotext = s.getExternalizable();
		assertEquals(gotext.object, WriteAbortedException.class.getName() + "xyz");
		assertEquals(gotext.next, FailIOException.class.getName() + "ijk");
	}

}
