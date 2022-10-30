package testing.saker.build.tests.rmi;

import java.io.EOFException;
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

	private static class FailRuntimeException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public FailRuntimeException(String message) {
			super(message);
		}
	}

	public static class FailIOExternalizable implements Externalizable {
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

	public static class FailRuntimeExternalizable implements Externalizable {
		private static final long serialVersionUID = 1L;

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			throw new FailRuntimeException("write");
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			throw new FailRuntimeException("read");
		}
	}

	public static class FailRuntimeWriteExternalizable implements Externalizable {
		private static final long serialVersionUID = 1L;

		private Object value;

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			throw new FailRuntimeException("write");
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			value = in.readObject();
		}
	}

	public static class MyExternalizable implements Externalizable {
		private static final long serialVersionUID = 1L;

		private Object object;
		private Object next;
		private Object third;
		private Object fourth;

		public MyExternalizable() {
		}

		public MyExternalizable(Object object) {
			this.object = object;
			next = new FailIOExternalizable();
			third = new FailRuntimeExternalizable();
			fourth = new FailRuntimeWriteExternalizable();
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			System.out.println("SerializationFailRecoveryTest.MyExternalizable.writeExternal() enter");
			try {
				out.writeObject(object);
			} catch (NotSerializableException e) {
				System.out.println("SerializationFailRecoveryTest.MyExternalizable.writeExternal(): " + e);
				out.writeObject("1");
			}
			try {
				out.writeObject(next);
			} catch (FailIOException e) {
				// expected
				System.out.println("SerializationFailRecoveryTest.MyExternalizable.writeExternal(): next: " + e);
				out.writeObject("2");
			}
			try {
				out.writeObject(third);
			} catch (FailRuntimeException e) {
				// expected
				System.out.println("SerializationFailRecoveryTest.MyExternalizable.writeExternal(): third: " + e);
				out.writeObject("3");
			}
			try {
				out.writeObject(fourth);
			} catch (FailRuntimeException e) {
				// expected
				System.out.println("SerializationFailRecoveryTest.MyExternalizable.writeExternal(): fourth: " + e);
				out.writeObject("4");
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
			try {
				third = in.readObject();
			} catch (FailRuntimeException e) {
				//expected
				System.out.println("Got " + e);

				assertEquals(e.getMessage(), "read");
				Object readafter = in.readObject();
				third = FailRuntimeException.class.getName() + readafter;
			}
			try {
				fourth = in.readObject();
			} catch (EOFException e) {
				//expected
				System.out.println("Got " + e);

				Object readafter = in.readObject();
				fourth = EOFException.class.getName() + readafter;
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
				out.writeObject("1");
			}

			try {
				out.writeWrappedObject(new Object(), FailWrapper.class);
			} catch (FailIOException e) {
				System.out.println("SerializationFailRecoveryTest.RecoveryRMIWrapper.writeWrapped() failer: " + e);
				out.writeObject("2");
			}

			try {
				out.writeSerializedObject(new FailRuntimeExternalizable());
			} catch (FailRuntimeException e) {
				System.out.println(
						"SerializationFailRecoveryTest.RecoveryRMIWrapper.writeWrapped() FailRuntimeExternalizable: "
								+ e);
				out.writeObject("3");
			}
			try {
				out.writeSerializedObject(new FailRuntimeWriteExternalizable());
			} catch (FailRuntimeException e) {
				System.out.println(
						"SerializationFailRecoveryTest.RecoveryRMIWrapper.writeWrapped() FailRuntimeWriteExternalizable: "
								+ e);
				out.writeObject("4");
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
				object = object + "\n" + FailIOException.class.getName() + readafter;
			}
			try {
				in.readObject();
			} catch (FailRuntimeException e) {
				System.out.println("Got FailRuntimeExternalizable: " + e);

				Object readafter = in.readObject();
				object = object + "\n" + FailRuntimeException.class.getName() + readafter;
			}
			try {
				in.readObject();
			} catch (IOException e) {
				System.out.println("Got IOException: " + e);

				Object readafter = in.readObject();
				object = object + "\n" + IOException.class.getName() + readafter;
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
		assertEquals(s.get(), WriteAbortedException.class.getName() + "1" + "\n" + FailIOException.class.getName() + "2"
				+ "\n" + FailRuntimeException.class.getName() + "3" + "\n" + IOException.class.getName() + "4");

		MyExternalizable gotext = s.getExternalizable();
		assertEquals(gotext.object, WriteAbortedException.class.getName() + "1");
		assertEquals(gotext.next, FailIOException.class.getName() + "2");
		assertEquals(gotext.third, FailRuntimeException.class.getName() + "3");
		assertEquals(gotext.fourth, EOFException.class.getName() + "4");
	}

}
