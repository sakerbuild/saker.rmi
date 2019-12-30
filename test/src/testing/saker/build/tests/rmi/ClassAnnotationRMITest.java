package testing.saker.build.tests.rmi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.rmi.annot.transfer.RMIWrap;
import saker.rmi.annot.transfer.RMIWriter;
import saker.rmi.io.RMIObjectInput;
import saker.rmi.io.RMIObjectOutput;
import saker.rmi.io.wrap.RMIWrapper;
import saker.rmi.io.writer.SerializeRMIObjectWriteHandler;
import testing.saker.SakerTest;

@SakerTest
public class ClassAnnotationRMITest extends BaseVariablesRMITestCase {

	private static int externalizeCount = 0;
	private static int wrapCount = 0;

	public interface Stub {
		public default Stub forward(Stub s) {
			return s;
		}
	}

	@RMIWriter(SerializeRMIObjectWriteHandler.class)
	public static class ExternalizingImpl implements Stub, Externalizable {
		private static final long serialVersionUID = 1L;

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			++externalizeCount;
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		}
	}

	@RMIWrap(WrapperImpl.class)
	public static class WrappedImpl implements Stub {
	}

	public static class SimpleImpl implements Stub {

	}

	private static class WrapperImpl implements RMIWrapper {
		public WrapperImpl() {
		}

		public WrapperImpl(WrappedImpl i) {
		}

		@Override
		public void writeWrapped(RMIObjectOutput out) throws IOException {
			++wrapCount;
		}

		@Override
		public void readWrapped(RMIObjectInput in) throws IOException, ClassNotFoundException {
		}

		@Override
		public Object getWrappedObject() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object resolveWrapped() {
			return new WrappedImpl();
		}

	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		externalizeCount = 0;
		wrapCount = 0;

		Stub s = (Stub) clientVariables.newRemoteInstance(ExternalizingImpl.class);
		assertEquals(externalizeCount, 0);
		assertEquals(wrapCount, 0);

		Stub simple = new SimpleImpl();
		assertIdentityEquals(s.forward(simple), simple);
		assertEquals(externalizeCount, 0);

		ExternalizingImpl externing = new ExternalizingImpl();
		Stub extforwarded = s.forward(externing);
		assertEquals(externalizeCount, 2);
		assertNotIdentityEquals(extforwarded, externing);

		WrappedImpl wrapped = new WrappedImpl();
		assertEquals(wrapCount, 0);
		Stub wrapforwarded = s.forward(wrapped);
		assertEquals(wrapCount, 2);
		assertNotIdentityEquals(extforwarded, externing);
	}

}
