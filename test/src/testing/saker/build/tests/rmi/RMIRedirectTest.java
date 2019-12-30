package testing.saker.build.tests.rmi;

import java.lang.reflect.InvocationTargetException;

import saker.rmi.annot.invoke.RMIDefaultOnFailure;
import saker.rmi.annot.invoke.RMIForbidden;
import saker.rmi.annot.invoke.RMIRedirect;
import saker.rmi.annot.transfer.RMISerialize;
import saker.rmi.exception.RMICallFailedException;
import saker.rmi.exception.RMIProxyCreationFailedException;
import saker.rmi.util.RMIUtils;
import saker.util.ObjectUtils;
import saker.util.ReflectUtils;
import testing.saker.SakerTest;

@SakerTest
public class RMIRedirectTest extends BaseVariablesRMITestCase {
	private static boolean redirectRecalled = false;

	public interface SuperStub {
		@RMIDefaultOnFailure
		public default String superDefault() {
			return "superdef";
		}

		@RMIForbidden
		@RMIDefaultOnFailure
		public default String forbidden() {
			return "passed";
		}
	}

	public interface Stub extends SuperStub {
		@RMIRedirect(method = "redirectCall")
		public String call();

		@RMIRedirect(method = "redirectDefaultCall")
		public String defcall();

		@RMIRedirect(method = "redirectCall")
		public int call(int i);

		public default String def() {
			return "def";
		}

		@RMIDefaultOnFailure
		public default String deffailure(@RMISerialize Object param) {
			return "defaultfailure";
		}

		@RMIRedirect
		public int redirectRecall();

		@RMIRedirect
		@RMIForbidden
		public default int redirectForbidden() {
			System.out.println("RMIRedirectTest.Stub.redirectForbidden()");
			return 1;
		}

		public static int redirectCall(Stub s, int i) {
			return i;
		}

		public static int redirectRecall(Stub s) {
			redirectRecalled = true;
			try {
				return (int) RMIUtils.invokeRedirectRemoteMethod(Stub.class.getMethod("redirectRecall"), s);
			} catch (InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw ObjectUtils.sneakyThrow(e);
			}
		}

		public static String redirectCall(Stub s) {
			return "redirect";
		}

		public static String redirectDefaultCall(Stub s) {
			return s.def();
		}

		public static int redirectForbidden(Stub s) {
			try {
				return (int) RMIUtils
						.invokeRedirectRemoteMethod(ReflectUtils.getMethodAssert(Stub.class, "redirectForbidden"), s);
			} catch (RMICallFailedException | InvocationTargetException e) {
				throw ObjectUtils.sneakyThrow(e);
			}
		}
	}

	public static class Impl implements Stub {
		@Override
		public String call() {
			return "impl";
		}

		@Override
		public String def() {
			return "defimpl";
		}

		@Override
		public String forbidden() {
			return "failed";
		}

		@Override
		public String defcall() {
			return "defimpl";
		}

		@Override
		public String deffailure(Object param) {
			return "defaultsucceeded";
		}

		@Override
		public int redirectRecall() {
			return 999;
		}

		@Override
		public int call(int i) {
			return i * i;
		}
	}

	public interface Conflict1 {
		public Object f();
	}

	public interface Conflict2 {
		@RMIForbidden
		public Float f();
	}

	public static class ConflictImpl implements Conflict1, Conflict2 {
		@Override
		public Float f() {
			return null;
		}
	}

	@Override
	protected void runVariablesTestImpl() throws AssertionError {
		try {
			Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
			assertEquals(s.call(), "redirect");
			assertEquals(s.call(4), 4);
			assertEquals(s.defcall(), "defimpl");
			assertEquals(s.forbidden(), "passed");
			assertEquals(s.superDefault(), "superdef");
			assertEquals(s.deffailure(new Object()), "defaultfailure");
			assertException(RMIProxyCreationFailedException.class, () -> {
				clientVariables.newRemoteInstance(ConflictImpl.class);
			});
			assertEquals(s.redirectRecall(), 999);
			assertTrue(redirectRecalled);
			assertException(RMICallFailedException.class, () -> s.redirectForbidden());
		} catch (RMICallFailedException | InvocationTargetException | NoSuchMethodException e) {
			throw new AssertionError(e);
		}
	}

}
