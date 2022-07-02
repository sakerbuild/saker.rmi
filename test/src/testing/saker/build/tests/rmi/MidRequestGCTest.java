package testing.saker.build.tests.rmi;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import saker.rmi.connection.RMIConnection.IOErrorListener;
import saker.rmi.connection.RMITestUtil;
import saker.util.ReflectUtils;
import saker.util.io.function.IOConsumer;
import saker.util.thread.ThreadUtils;
import testing.saker.SakerTest;

/**
 * Test case for the issue #4.
 * <p>
 * It reliably reproduces the bug reported in the issue on a local dev machine. Due to its dependence on garbage
 * collection, its outcome may differ in a false positive. (Meaning it may occasionally succeed if garbage collection is
 * not performed.)
 */
@SakerTest
public class MidRequestGCTest extends BaseVariablesRMITestCase {
//	When multiple streams are used by an RMI variables context, 
//	it can cause garbage collection issues, 
//	in case when a garbage collection notification arrives earlier than some method result message.
//
//	E.g.: Connection with clients A and B.
//
//	A calls a method on B with a local object L as an argument, and causes a remote object R to be created on B.
//	The method returns R directly, so the method call result should be L.
//	After the method call result is written from B, R is immediately garbage collected.
//	This garbage collection causes a reference release message back to A. This message arrives earlier than the method result in 2.
//	The references of L decreases to 0, therefore L is no longer known by the RMI connection.
//	When reading the method result, L can no longer be found by the RMI connection, causing an exception.

	public interface Stub {
	}

	public static class Impl implements Stub {
	}

	private static Semaphore getBackCallSemaphore;

	public static Stub getBack(Stub stub) {
		getBackCallSemaphore.release();
		return stub;
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		System.out.println("MidRequestGCTest.runVariablesTestImpl() run test");
		try {
			//in case some other faulty test left it in an invalid state
			RMITestUtil.restoreInternalHandlers();
			Semaphore getbacksemaphore = new Semaphore(0);
			getBackCallSemaphore = getbacksemaphore;
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

			Semaphore gcsemaphore = new Semaphore(0);

			RMITestUtil.replaceCommandHandler(COMMAND_METHODRESULT, new IOConsumer<Object[]>() {
				@Override
				public void accept(Object[] args) throws IOException {
					try {
						System.out.println("MidRequestGCTest.COMMAND_METHODRESULT() in " + this);
						for (int i = 0; i < 50; ++i) {
							try {
								if (gcsemaphore.tryAcquire(50, TimeUnit.MILLISECONDS)) {
									System.out.println(
											"MidRequestGCTest.COMMAND_METHODRESULT() reference released for remote");
									break;
								}
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
							System.gc();
						}
					} finally {
						//need to always call this
						System.out.println("MidRequestGCTest.COMMAND_METHODRESULT() call original handler");
						RMITestUtil.callOriginalCommandHandler(COMMAND_METHODRESULT, args);
					}
				}
			});
			Impl impl = new Impl();
			Thread releaserthread = ThreadUtils.startDaemonThread(() -> {
				try {
					getbacksemaphore.acquire();
					for (int i = 0; i < 50; i++) {
						if (!RMITestUtil.isLocalObjectKnown(clientVariables, impl)) {
							gcsemaphore.release();
							System.out.println("MidRequestGCTest.runVariablesTestImpl() release GC semaphore");
							break;
						}
						System.gc();
						Thread.sleep(50);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			Object result = clientVariables.invokeRemoteStaticMethod(
					ReflectUtils.getMethodAssert(MidRequestGCTest.class, "getBack", Stub.class), impl);
			assertIdentityEquals(impl, result);
			System.out.println("MidRequestGCTest.runVariablesTestImpl() result: " + result);
			releaserthread.join();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			RMITestUtil.restoreInternalHandlers();
		}
	}

	@Override
	protected BaseRMITestSettings getTestSettings() {
		BaseRMITestSettings result = super.getTestSettings();
		//enough to run this test once, with a single executor
		result.maxStreamCount = 1;
		result.executors = new Executor[] { null };
		return result;
	}

}
