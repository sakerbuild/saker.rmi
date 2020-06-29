package testing.saker.build.tests.rmi;

import java.lang.reflect.InvocationTargetException;

import saker.rmi.annot.invoke.RMICacheResult;
import saker.rmi.annot.invoke.RMIRedirect;
import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.exception.RMIRuntimeException;
import saker.rmi.util.RMIUtils;
import saker.util.ObjectUtils;
import saker.util.ReflectUtils;
import testing.saker.SakerTest;

@SakerTest
public class StatisticsTest extends BaseVariablesRMITestCase {

	public interface Stub {
		public default int f() {
			return 123;
		}

		public default int throwing() {
			throw new UnsupportedOperationException();
		}

		@RMICacheResult
		public default String cached() {
			return this.toString();
		}

		@RMIRedirect()
		public default String redirected() {
			return "redir";
		}

		public static String redirected(Stub s) {
			try {
				return (String) RMIUtils
						.invokeRedirectRemoteMethod(ReflectUtils.getMethodAssert(Stub.class, "redirected"), s);
			} catch (InvocationTargetException | RMIRuntimeException | NullPointerException | AssertionError e) {
				throw new AssertionError(e);
			}
		}
	}

	public static class Impl implements Stub {

	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
		s.f();
		s.f();
		clientConnection.getStatistics().dumpSummary(System.out, null);
		assertEquals(ObjectUtils.newArrayList(clientConnection.getStatistics().getMethodStatisticsClear()).size(), 2);
		assertFalse(clientConnection.getStatistics().getMethodStatistics().iterator().hasNext());

		assertException(Exception.class, s::throwing);
		assertException(Exception.class, s::throwing);
		clientConnection.getStatistics().dumpSummary(System.out, null);
		assertEquals(ObjectUtils.newArrayList(clientConnection.getStatistics().getMethodStatisticsClear()).size(), 2);
		assertFalse(clientConnection.getStatistics().getMethodStatistics().iterator().hasNext());

		for (int i = 0; i < 5; i++) {
			s.cached();
		}
		clientConnection.getStatistics().dumpSummary(System.out, null);
		assertEquals(ObjectUtils.newArrayList(clientConnection.getStatistics().getMethodStatisticsClear()).size(), 1);
		assertFalse(clientConnection.getStatistics().getMethodStatistics().iterator().hasNext());

		s.redirected();
		s.redirected();
		clientConnection.getStatistics().dumpSummary(System.out, null);
		assertEquals(ObjectUtils.newArrayList(clientConnection.getStatistics().getMethodStatisticsClear()).size(), 2);
		assertFalse(clientConnection.getStatistics().getMethodStatistics().iterator().hasNext());
	}

	@Override
	protected RMIConnection[] createConnections(int maxthreads) throws Exception {
		return RMITestUtil.createPipedConnection(new RMIOptions().collectStatistics(true)
				.classLoader(getClass().getClassLoader()).maxStreamCount(maxthreads));
	}
}
