package testing.saker.build.tests.rmi;

import java.io.IOException;

import saker.rmi.annot.transfer.RMIWrap;
import saker.rmi.io.RMIObjectInput;
import saker.rmi.io.RMIObjectOutput;
import saker.rmi.io.wrap.RMIWrapper;
import testing.saker.SakerTest;

@SakerTest
public class RecursiveRemoteWrappingClassRMITest extends BaseVariablesRMITestCase {
	public interface Stub {

		public default Stub forward(Stub s) {
			return s;
		}
	}

	@RMIWrap(ImplWrapper.class)
	public static class Impl implements Stub {
	}

	public static class ImplWrapper implements RMIWrapper {
		private Stub impl;

		public ImplWrapper() {
		}

		public ImplWrapper(Impl impl) {
			this.impl = impl;
		}

		@Override
		public void writeWrapped(RMIObjectOutput out) throws IOException {
			//impl should be written as a remote, and the class transfer properties shouldnt override it
			out.writeRemoteObject(impl);
		}

		@Override
		public void readWrapped(RMIObjectInput in) throws IOException, ClassNotFoundException {
			impl = (Stub) in.readObject();
		}

		@Override
		public Object getWrappedObject() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object resolveWrapped() {
			return impl;
		}

	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
		Impl i = new Impl();
		Stub fi = s.forward(i);
		assertIdentityEquals(i, fi);
	}

}
