package testing.saker.build.tests.rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;

import saker.rmi.exception.RMIStackTracedException;
import saker.util.ObjectUtils;
import saker.util.io.UnsyncByteArrayInputStream;
import saker.util.io.UnsyncByteArrayOutputStream;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class StackTracedExceptionTest extends SakerTestCase {

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		testException(new RuntimeException("abc"));
		testException(new RuntimeException("abc", new IOException("xyz")));
		testException(
				new RuntimeException("abc", new InvocationTargetException(new IllegalArgumentException("xxx"), "ite")));

		recursiveCause();
		recursiveSuppressed();
		recursiveCauseSuppressed();
		recursiveSuppressedCause();
	}

	private static void recursiveCause() throws Exception {
		RuntimeException recursing = new RuntimeException("recursing");
		IllegalArgumentException main = new IllegalArgumentException("main", recursing);
		recursing.initCause(main);
		testException(main);
	}

	private static void recursiveSuppressed() throws Exception {
		RuntimeException recursing = new RuntimeException("recursing");
		IllegalArgumentException main = new IllegalArgumentException("main");
		recursing.addSuppressed(main);
		main.addSuppressed(recursing);
		testException(main);
	}

	private static void recursiveCauseSuppressed() throws Exception {
		RuntimeException recursing = new RuntimeException("recursing");
		IllegalArgumentException main = new IllegalArgumentException("main", recursing);
		recursing.addSuppressed(main);
		testException(main);
	}

	private static void recursiveSuppressedCause() throws Exception {
		RuntimeException recursing = new RuntimeException("recursing");
		IllegalArgumentException main = new IllegalArgumentException("main");
		main.addSuppressed(recursing);
		recursing.initCause(main);
		testException(main);
	}

	private static void testException(RuntimeException exc) throws Exception {
		RMIStackTracedException tracedexc = new RMIStackTracedException(exc);
		String stackstr = printStackTraceToString(tracedexc);
		System.out.println("Stack trace:");
		System.out.println(stackstr);
		checkExceptionStringsPresent(stackstr, exc, ObjectUtils.newIdentityHashSet());

		//test serialization
		try (UnsyncByteArrayOutputStream baos = new UnsyncByteArrayOutputStream()) {
			try (ObjectOutputStream os = new ObjectOutputStream(baos)) {
				os.writeObject(tracedexc);
			}
			try (ObjectInputStream is = new ObjectInputStream(
					new UnsyncByteArrayInputStream(baos.toByteArrayRegion()))) {
				RMIStackTracedException read = (RMIStackTracedException) is.readObject();

				//the re-read stack trace should be the same
				assertEquals(stackstr, printStackTraceToString(read));
			}
		}
	}

	private static String printStackTraceToString(RMIStackTracedException tracedexc) throws IOException {
		String stackstr;
		try (StringWriter sw = new StringWriter()) {
			tracedexc.printStackTrace(new PrintWriter(sw));
			stackstr = sw.toString();
		}
		return stackstr;
	}

	private static void checkExceptionStringsPresent(String stacktrace, Throwable exc,
			Set<? super Throwable> exceptions) {
		if (exc == null || !exceptions.add(exc)) {
			return;
		}
		String excstr = exc.toString();
		assertTrue(stacktrace.contains(excstr), excstr);
		checkExceptionStringsPresent(stacktrace, exc.getCause(), exceptions);

		for (Throwable s : exc.getSuppressed()) {
			checkExceptionStringsPresent(stacktrace, s, exceptions);
		}
	}

}
