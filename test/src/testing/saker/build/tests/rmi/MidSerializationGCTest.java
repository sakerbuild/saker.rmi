package testing.saker.build.tests.rmi;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import saker.rmi.annot.transfer.RMIWrap;
import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIConnection.IOErrorListener;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.io.RMIObjectInput;
import saker.rmi.io.RMIObjectOutput;
import saker.rmi.io.wrap.RMIWrapper;
import saker.util.ReflectUtils;
import saker.util.io.function.IOFunction;
import saker.util.thread.ThreadUtils;
import testing.saker.SakerTest;

/**
 * Test case for the issue #4.
 * <p>
 * To test when garbage collection happens during serialization of an object, and ensure that the garbage collection is
 * not sent before a method result is sent.
 */
@SakerTest
public class MidSerializationGCTest extends BaseVariablesRMITestCase {
	public interface Stub {
		public Stub getOne();
	}

	public static class Impl implements Stub {
		public String val;
		public Impl theone;

		public Impl() {
			this.val = "impl";
			this.theone = new Impl("one");
		}

		public Impl(String val) {
			this.val = val;
		}

		@Override
		public Stub getOne() {
			return theone;
		}

	}

	private static class ComplexObj {
		protected Stub stub;

		public ComplexObj(Stub stub) {
			this.stub = stub;
		}

	}

	private static Semaphore getBackCallSemaphore;
	private static Semaphore writeDoneSemaphore;
	private static Semaphore gcSemaphore;
	private static volatile boolean gotSecondStub;

	@RMIWrap(SerializerImpl.class)
	public static ComplexObj getBack(Stub stub) {
		getBackCallSemaphore.release();

		assertTrue(RMIConnection.isRemoteObject(stub), "not remote");
		//we want to use a remote object that is not the argument to this function, as reference to the argument may be kept during invocation
		//therefore it won't be released by the GC.
		Stub stub2 = stub.getOne();
		gotSecondStub = true;
		return new ComplexObj(stub2);
	}

	public static class SerializerImpl implements RMIWrapper {

		private ComplexObj complexObj;

		public SerializerImpl() {
		}

		public SerializerImpl(ComplexObj complexObj) {
			this.complexObj = complexObj;
		}

		@Override
		public void writeWrapped(RMIObjectOutput out) throws IOException {
			System.out.println("MidSerializationGCTest.SerializerImpl.writeWrapped() in");
			out.writeObject(complexObj.stub);
			ReferenceQueue<Object> queue = new ReferenceQueue<>();
			WeakReference<?> weakref = new WeakReference<>(complexObj.stub, queue);
			this.complexObj.stub = null;
			this.complexObj = null;
			try {
				for (int i = 0; i < 50; i++) {
					System.gc();
					if (queue.remove(50) != null) {
						//the stub was garbage collected
						System.out.println(
								"MidSerializationGCTest.SerializerImpl.writeWrapped() stub was garbage collected ");
						break;
					}
				}
				if (weakref.get() != null) {
					System.out.println(
							"MidSerializationGCTest.SerializerImpl.writeWrapped() stub was NOT garbage collected");
				}
				writeDoneSemaphore.release();
				for (int i = 0; i < 50; i++) {
					System.gc();
					if (gcSemaphore.tryAcquire(50, TimeUnit.MILLISECONDS)) {
						//wait with finishing the writing, until the gc message arrives on the other side
						//(if the bug is fixed, this never arrives)
						break;
					}
				}
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}

		@Override
		public void readWrapped(RMIObjectInput in) throws IOException, ClassNotFoundException {
			complexObj = new ComplexObj((Stub) in.readObject());
		}

		@Override
		public Object resolveWrapped() {
			return complexObj;
		}

		@Override
		public Object getWrappedObject() {
			return complexObj;
		}

	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		System.out.println("MidSerializationGCTest.runVariablesTestImpl() run test");
		try {
			//in case some other faulty test left it in an invalid state
			RMITestUtil.restoreInternalHandlers();
			Semaphore getbacksemaphore = new Semaphore(0);
			Semaphore gcsemaphore = new Semaphore(0);
			Semaphore writedonesemaphore = new Semaphore(0);

			getBackCallSemaphore = getbacksemaphore;
			writeDoneSemaphore = writedonesemaphore;
			gcSemaphore = gcsemaphore;
			gotSecondStub = false;
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

			RMITestUtil.replaceCommandHandler(COMMAND_METHODRESULT,new IOFunction<Object[], Object>() {
				@Override
				public Object apply(Object[] args) throws IOException {
					try {
						System.out.println("MidSerializationGCTest.COMMAND_METHODRESULT() in " + this);
						if (gotSecondStub) {
							for (int i = 0; i < 50; ++i) {
								try {
									if (gcsemaphore.tryAcquire(50, TimeUnit.MILLISECONDS)) {
										System.out.println(
												"MidSerializationGCTest.COMMAND_METHODRESULT() reference released for remote");
										break;
									}
								} catch (InterruptedException e) {
									throw new RuntimeException(e);
								}
								System.gc();
							}
						}
					} finally {
						//need to always call this
						System.out.println("MidSerializationGCTest.COMMAND_METHODRESULT() call original handler");
						return RMITestUtil.callOriginalCommandHandler(COMMAND_METHODRESULT, args);
					}
				}
			});
			Impl impl = new Impl();
			Thread releaserthread = ThreadUtils.startDaemonThread(() -> {
				try {
					getbacksemaphore.acquire();
					writedonesemaphore.acquire();
					for (int i = 0; i < 50; i++) {
						if (!RMITestUtil.isLocalObjectKnown(clientVariables, impl.theone)) {
							System.out.println("MidSerializationGCTest.runVariablesTestImpl() release GC semaphore");
							gcsemaphore.release();
							break;
						}
						System.gc();
						Thread.sleep(50);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			ComplexObj result = (ComplexObj) clientVariables.invokeRemoteStaticMethod(
					ReflectUtils.getMethodAssert(MidSerializationGCTest.class, "getBack", Stub.class), impl);
			assertNonNull(result.stub);
			assertIdentityEquals(result.stub, impl.theone);
			assertEquals(result.stub.getClass(), Impl.class);
			assertEquals(((Impl) result.stub).val, "one");
			System.out.println("MidSerializationGCTest.runVariablesTestImpl() result: " + result);
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
