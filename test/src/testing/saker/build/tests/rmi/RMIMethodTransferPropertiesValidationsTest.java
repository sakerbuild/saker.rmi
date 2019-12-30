package testing.saker.build.tests.rmi;

import java.io.IOException;
import java.util.Map;

import saker.rmi.annot.transfer.RMIRemote;
import saker.rmi.annot.transfer.RMISerialize;
import saker.rmi.annot.transfer.RMIWrap;
import saker.rmi.annot.transfer.RMIWriter;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RemoteProxyObject;
import saker.rmi.exception.RMIInvalidConfigurationException;
import saker.rmi.io.RMIObjectInput;
import saker.rmi.io.RMIObjectOutput;
import saker.rmi.io.wrap.RMIWrapper;
import saker.rmi.io.writer.DefaultRMIObjectWriteHandler;
import saker.rmi.io.writer.WrapperRMIObjectWriteHandler;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
@SuppressWarnings("unused")
public class RMIMethodTransferPropertiesValidationsTest extends SakerTestCase {
	private static class AnnotTestRMIWrapper implements RMIWrapper {
		public AnnotTestRMIWrapper(Object obj) {
		}

		@Override
		public Object getWrappedObject() {
			return null;
		}

		@Override
		public void writeWrapped(RMIObjectOutput out) throws IOException {
		}

		@Override
		public void readWrapped(RMIObjectInput in) throws IOException, ClassNotFoundException {
		}

		@Override
		public Object resolveWrapped() {
			return this;
		}

	}

	private interface ValidMethods {
		public void f();

		@RMIRemote
		public Object remoteObject();

		@RMIRemote
		public RemoteProxyObject remoteProxyObject();

		@RMIRemote
		public Runnable remoteInterface();

		@RMISerialize
		public Object serializeObject();

		@RMISerialize
		public Runnable serializeInterface();

		@RMIWriter(DefaultRMIObjectWriteHandler.class)
		public Object writerObject();

		@RMIWrap(AnnotTestRMIWrapper.class)
		public Object wrapperObject();
	}

	private static class InvalidConfigurations {
		public interface NonTransferableCustomizeInt {
			@RMISerialize
			public int f();
		}

		public interface NonTransferableCustomizeVoid {
			@RMISerialize
			public void f();
		}

		public interface MultiAnnot {
			@RMIRemote
			@RMISerialize
			public Object f();
		}

		public interface NonInterfaceRemote {
			@RMIRemote
			public Thread f();
		}

		public interface NonInstantiableWriter {
			@RMIWriter(NonInstantiableRMIObjectWriteHandler.class)
			public Object f();
		}

		public interface PrimitiveWrap {
			@RMIWrap(IntRMIWrapper.class)
			public int f();
		}

		public interface NotExtendedRmiWrapper {
			@RMIWrap(RMIWrapper.class)
			public Object f();
		}

		public interface EnumRmiWrapper {
			@RMIWrap(EnumRMIWrapper.class)
			public Object f();
		}
	}

	private static class NonInstantiableRMIObjectWriteHandler extends WrapperRMIObjectWriteHandler {
		public NonInstantiableRMIObjectWriteHandler() {
			super(AnnotTestRMIWrapper.class);
			throw new RuntimeException();
		}
	}

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		RMITestUtil.validateMethodProperties(ValidMethods.class);

		expectInvalidConfiguration(InvalidConfigurations.class.getDeclaredClasses());
	}

	private static void expectInvalidConfiguration(Class<?> type) {
		try {
			RMITestUtil.validateMethodProperties(type);
			fail("Validation succeeded for " + type);
		} catch (RMIInvalidConfigurationException e) {
		}
	}

	private static void expectInvalidConfiguration(Class<?>... types) {
		for (Class<?> t : types) {
			expectInvalidConfiguration(t);
		}
	}

	private static enum EnumRMIWrapper implements RMIWrapper {
		INSTANCE;
		@Override
		public void writeWrapped(RMIObjectOutput out) throws IOException {
		}

		@Override
		public void readWrapped(RMIObjectInput in) throws IOException, ClassNotFoundException {
		}

		@Override
		public Object resolveWrapped() {
			return INSTANCE;
		}

		@Override
		public Object getWrappedObject() {
			return INSTANCE;
		}
	}

	private static class IntRMIWrapper implements RMIWrapper {
		public IntRMIWrapper(int i) {
		}

		@Override
		public void writeWrapped(RMIObjectOutput out) throws IOException {
		}

		@Override
		public void readWrapped(RMIObjectInput in) throws IOException, ClassNotFoundException {
		}

		@Override
		public Object resolveWrapped() {
			return 3;
		}

		@Override
		public Object getWrappedObject() {
			return 3;
		}
	}
}
