package testing.saker.build.tests.rmi;

import java.io.IOException;
import java.io.PrintStream;

import saker.rmi.connection.RMIConnection.IOErrorListener;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.exception.RMIIOFailureException;
import saker.util.io.function.IOConsumer;
import testing.saker.SakerTest;

@SakerTest
public class IOErrorTest extends BaseVariablesRMITestCase {
	public interface Stub {
	}

	public static class Impl implements Stub {

	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		try {
			//in case some other faulty test left it in an invalid state
			RMITestUtil.restoreInternalHandlers();
			clientConnection.addErrorListener(new IOErrorListener() {
				@Override
				public void onIOError(Throwable exc) {
					PrintStream errout = System.err;
					synchronized (errout) {
						errout.println("Client error:");
						exc.printStackTrace(errout);
					}
				}
			});
			serverConnection.addErrorListener(new IOErrorListener() {
				@Override
				public void onIOError(Throwable exc) {
					PrintStream errout = System.err;
					synchronized (errout) {
						errout.println("Server error:");
						exc.printStackTrace(errout);
					}
				}
			});

			final short COMMAND_METHODRESULT = 3;

			RMITestUtil.replaceCommandHandler(COMMAND_METHODRESULT, new IOConsumer<Object[]>() {
				@Override
				public void accept(Object[] args) throws IOException {
					throw new IOException("read fail");
				}
			});

			Stub stub = (Stub) clientVariables.newRemoteInstance(Impl.class);
			assertException(RMIIOFailureException.class, () -> stub.toString());
		} finally {
			RMITestUtil.restoreInternalHandlers();
		}
	}

}
