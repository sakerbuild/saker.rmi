package testing.saker.build.tests.rmi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import saker.rmi.annot.invoke.RMICacheResult;
import saker.rmi.annot.invoke.RMIDefaultOnFailure;
import saker.rmi.annot.invoke.RMIExceptionRethrow;
import saker.rmi.annot.invoke.RMIForbidden;
import saker.rmi.annot.invoke.RMIRedirect;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.exception.RMIInvalidConfigurationException;
import saker.rmi.exception.RMIRuntimeException;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
@SuppressWarnings("unused")
public class RMIMethodInvokePropertiesValidationsTest extends SakerTestCase {

	private interface ValidMethods {
		public void f();

		@RMICacheResult
		public int caching();

		@RMICacheResult
		public int multiCaching(int x, int y, int z);

		@RMIDefaultOnFailure
		public default void defaulted() {
		}

		@RMIForbidden
		public void forbidden();

		@RMIForbidden
		@RMIDefaultOnFailure
		public default void forbiddenDefault() {
		}

		@RMIExceptionRethrow(RuntimeException.class)
		public void rethrowRuntime();

		@RMIExceptionRethrow(Error.class)
		public void rethrowError();

		@RMIExceptionRethrow(IOException.class)
		public void rethrowChecked() throws IOException;

		@RMIExceptionRethrow(RuntimeException.class)
		@RMIForbidden
		public void rethrowRuntimeForbidden();

		@RMIExceptionRethrow(Error.class)
		@RMIForbidden
		public void rethrowErrorForbidden();

		@RMIExceptionRethrow(IOException.class)
		@RMIForbidden
		public void rethrowCheckedForbidden() throws IOException;

		@RMIExceptionRethrow(IOSubClassException.class)
		public void rethrowCheckedSubclass() throws IOException;

		@RMIExceptionRethrow(RMIRuntimeExceptionConstructorException.class)
		public void rethrowRMIExceptionConstructor();

		@RMIRedirect
		public void redirect();

		public static void redirect(ValidMethods methods) {
		}

		@RMIRedirect
		@RMIForbidden
		public void forbiddenRedirect();

		public static void forbiddenRedirect(ValidMethods methods) {
		}

		@RMIRedirect(method = "namedRedirectHandler")
		public void namedRedirect();

		public static void namedRedirectHandler(ValidMethods methods) {
		}

		@RMIRedirect(type = ValidRedirects.class)
		public void classedRedirect();

		@RMIRedirect(type = ValidRedirects.class, method = "namedRedirectHandler")
		public void classedNamedRedirect();
	}

	private static class ValidRedirects {
		public static void classedRedirect(ValidMethods methods) {
		}

		public static void namedRedirectHandler(ValidMethods methods) {
		}
	}

	private static class InvalidConfigurations {
		public interface VoidCaching {
			@RMICacheResult
			public void f();
		}

		public interface StaticCaching {
			@RMICacheResult
			public static int f() {
				return 0;
			}
		}

		public interface ForbiddenCaching {
			@RMIForbidden
			@RMICacheResult
			public int f();
		}

		public interface RedirectCaching {
			@RMIRedirect
			@RMICacheResult
			public int f();

			public static int f(RedirectCaching c) {
				return 0;
			}
		}

		public interface NonDefaultFailure {
			@RMIDefaultOnFailure
			public void f();
		}

		public interface RedirectDefaultOnFailure {
			@RMIRedirect
			@RMIDefaultOnFailure
			public void f();

			public static void f(RedirectCaching c) {
			}
		}

		public interface NoRedirectMethod {
			@RMIRedirect
			public void f();
		}

		public interface InvalidArgumentsRedirectMethod {
			@RMIRedirect
			public void f();

			public static void f(RedirectCaching c, int x, int y) {
			}
		}

		public interface NoProxyArgumentRedirectMethod {
			@RMIRedirect
			public void f();

			public static void f(int z) {
			}
		}

		public interface DifferentReturnTypeRedirectMethod {
			@RMIRedirect
			public void f();

			public static int f(DifferentReturnTypeRedirectMethod p) {
				return 0;
			}
		}

		public interface NonStaticRedirectMethod {
			@RMIRedirect
			public void f();

			public void f(RedirectCaching c);
		}

		public interface StaticRedirectMethod {
			@RMIRedirect
			public static void f() {
			}

			public static void f(RedirectCaching c) {
			}
		}

		public interface ExceptionRethrowRedirectMethod {
			@RMIRedirect
			@RMIExceptionRethrow(RuntimeException.class)
			public void f();

			public static void f(RedirectCaching c) {
			}
		}

		public interface NotDeclaredThrowableRethrow {
			@RMIExceptionRethrow(Throwable.class)
			public void f();
		}

		public interface NotDeclaredCheckedRethrow {
			@RMIExceptionRethrow(Exception.class)
			public void f();
		}

		public interface NoConstructorRethrow {
			@RMIExceptionRethrow(NoConstructorRethrowException.class)
			public void f();
		}

		public interface InvalidRedirectExceptions {
			@RMIRedirect
			public default void f() {
			}

			public static void f(InvalidRedirectExceptions c) throws Exception {
			}
		}

		public interface UpclassRedirectExceptions {
			@RMIRedirect
			public default void f() throws FileNotFoundException {
			}

			public static void f(UpclassRedirectExceptions c) throws IOException {
			}
		}

		public interface ThrowableRedirectExceptions {
			@RMIRedirect
			public default void f() {
			}

			public static void f(ThrowableRedirectExceptions c) throws Throwable {
			}
		}

		public interface Throwable2RedirectExceptions {
			@RMIRedirect
			public default void f() throws FileNotFoundException {
			}

			public static void f(Throwable2RedirectExceptions c) throws Throwable {
			}
		}
	}

	public interface SubclassRedirectExceptions {
		@RMIRedirect
		public default void f() throws IOException {
		}

		public static void f(SubclassRedirectExceptions c) throws FileNotFoundException {
		}
	}

	private static class NoConstructorRethrowException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	private static class IOSubClassException extends IOException {
		private static final long serialVersionUID = 1L;

		public IOSubClassException(Throwable cause) {
			super(cause);
		}
	}

	private static class RMIRuntimeExceptionConstructorException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public RMIRuntimeExceptionConstructorException(RMIRuntimeException cause) {
			super(cause);
		}

	}

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		RMITestUtil.validateMethodProperties(ValidMethods.class);
		RMITestUtil.validateMethodProperties(SubclassRedirectExceptions.class);

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
}
