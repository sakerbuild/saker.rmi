package testing.saker.build.tests.rmi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;

import testing.saker.SakerTest;

@SakerTest
public class ExternalizableConstructorRMITest extends BaseVariablesRMITestCase {

	public static class MyClass implements Externalizable {
		private static final long serialVersionUID = 1L;
		private String val;

		public MyClass() {
		}

		public MyClass(String val) {
			this.val = val;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeUTF(val);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			val = in.readUTF();
		}

		@Override
		public String toString() {
			return getClass().getName() + "[" + val + "]";
		}
	}

	public static void call(MyClass c) {
		System.out.println("ExternalizableConstructorRMITest.call() " + c);
	}

	@Override
	protected void runVariablesTestImpl() throws AssertionError {
		try {
			clientVariables.invokeRemoteStaticMethod(
					ExternalizableConstructorRMITest.class.getDeclaredMethod("call", MyClass.class),
					new MyClass("Hello"));
		} catch (NoSuchMethodException | SecurityException e) {
			throw new AssertionError(e);
		} catch (InvocationTargetException e) {
			assertEquals(e.getTargetException().getClass(), InvalidClassException.class);
		}
	}

}
