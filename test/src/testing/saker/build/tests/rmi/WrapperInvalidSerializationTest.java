package testing.saker.build.tests.rmi;

import java.io.EOFException;
import java.io.IOException;

import saker.rmi.annot.transfer.RMIWrap;
import saker.rmi.io.RMIObjectInput;
import saker.rmi.io.RMIObjectOutput;
import saker.rmi.io.wrap.RMIWrapper;
import testing.saker.SakerTest;

@SakerTest
public class WrapperInvalidSerializationTest extends BaseVariablesRMITestCase {
	public interface Stub {
		//this function signature reliably reproduces the original issue
		//if the wrapper didn't fully read the data, then it would leave objects in the stream
		//and that would be read as method argumens, thus failing the call
		@SuppressWarnings("unused")
		public default Object getNotFull(@RMIWrap(NotFullyReadRMIWrapper.class) Object o, String a, String b, String c,
				int idx) {
			switch (idx) {
				case 0: {
					return a;
				}
				case 1: {
					return b;
				}
				case 2: {
					return c;
				}
				default: {
					return null;
				}
			}
		}

		@SuppressWarnings("unused")
		public default Object getReadMore(@RMIWrap(ReadMoreRMIWrapper.class) Object o, String a, String b, String c,
				int idx) {
			switch (idx) {
				case 0: {
					return a;
				}
				case 1: {
					return b;
				}
				case 2: {
					return c;
				}
				default: {
					return null;
				}
			}
		}
	}

	public static class Impl implements Stub {
	}

	public static class NotFullyReadRMIWrapper implements RMIWrapper {
		private Object object;

		public NotFullyReadRMIWrapper() {
		}

		public NotFullyReadRMIWrapper(Object object) {
			this.object = object;
		}

		@Override
		public void writeWrapped(RMIObjectOutput out) throws IOException {
			out.writeObject(object);
			out.writeObject(object);
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

	public static class ReadMoreRMIWrapper implements RMIWrapper {
		private Object object;

		public ReadMoreRMIWrapper() {
		}

		public ReadMoreRMIWrapper(Object object) {
			this.object = object;
		}

		@Override
		public void writeWrapped(RMIObjectOutput out) throws IOException {
			out.writeObject(object);
		}

		@Override
		public void readWrapped(RMIObjectInput in) throws IOException, ClassNotFoundException {
			object = in.readObject();
			assertException(EOFException.class, () -> in.readObject());
			assertException(EOFException.class, () -> in.readObject());
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

		assertEquals(s.getNotFull(new Object(), "a", "b", "c", 0), "a");
		assertEquals(s.getNotFull(new Object(), "a", "b", "c", 1), "b");
		assertEquals(s.getNotFull(new Object(), "a", "b", "c", 2), "c");
		assertEquals(s.getNotFull(new Object(), "a", "b", "c", 3), null);

		assertEquals(s.getReadMore(new Object(), "a", "b", "c", 0), "a");
		assertEquals(s.getReadMore(new Object(), "a", "b", "c", 1), "b");
		assertEquals(s.getReadMore(new Object(), "a", "b", "c", 2), "c");
		assertEquals(s.getReadMore(new Object(), "a", "b", "c", 3), null);
	}

}
