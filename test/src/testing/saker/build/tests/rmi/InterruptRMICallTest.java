package testing.saker.build.tests.rmi;

import java.util.concurrent.Semaphore;

import testing.saker.SakerTest;

@SakerTest
public class InterruptRMICallTest extends BaseVariablesRMITestCase {
	private static final Semaphore INTERRUPT_CLEARER_SEMAPHORE = new Semaphore(1);
	private static final Semaphore INTERRUPT_NONCONSUMING_SEMAPHORE = new Semaphore(1);

	public interface Stub {
		public void waitInterrupting() throws InterruptedException;

		public void interruptSetting();

		public void interruptConsuming();

		public void interruptNonConsuming();

		public void throwingInterruptSetting();
	}

	public static class Impl implements Stub {
		@Override
		public void waitInterrupting() throws InterruptedException {
			synchronized (this) {
				try {
					this.wait(1000);
					fail();
				} catch (InterruptedException e) {
					throw e;
				}
			}
		}

		@Override
		public void interruptSetting() {
			assertFalse(Thread.currentThread().isInterrupted());
			Thread.currentThread().interrupt();
		}

		@Override
		public void interruptConsuming() {
			INTERRUPT_CLEARER_SEMAPHORE.acquireUninterruptibly();
			assertTrue(Thread.interrupted());
			INTERRUPT_CLEARER_SEMAPHORE.release();
		}

		@Override
		public void interruptNonConsuming() {
			INTERRUPT_NONCONSUMING_SEMAPHORE.acquireUninterruptibly();
			assertTrue(Thread.currentThread().isInterrupted());
			INTERRUPT_NONCONSUMING_SEMAPHORE.release();
		}

		@Override
		public void throwingInterruptSetting() {
			assertFalse(Thread.currentThread().isInterrupted());
			Thread.currentThread().interrupt();
			throw new RuntimeException();
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
		Thread currentthread = Thread.currentThread();
		currentthread.interrupt();
		assertException(InterruptedException.class, s::waitInterrupting);

		//clear current thread interrupt status
		Thread.interrupted();
		s.interruptSetting();
		assertTrue(currentthread.isInterrupted());

		//clear current thread interrupt status
		Thread.interrupted();

		INTERRUPT_CLEARER_SEMAPHORE.acquire();
		new Thread(() -> {
			currentthread.interrupt();
			try {
				//wait a bit to allow the interrupt RMI request to arrive
				Thread.sleep(500);
			} catch (InterruptedException e) {
				throw fail();
			} finally {
				INTERRUPT_CLEARER_SEMAPHORE.release();
			}
		}).start();
		s.interruptConsuming();
		assertFalse(currentthread.isInterrupted());

		//clear current thread interrupt status
		Thread.interrupted();

		INTERRUPT_NONCONSUMING_SEMAPHORE.acquire();
		new Thread(() -> {
			currentthread.interrupt();
			try {
				//wait a bit to allow the interrupt RMI request to arrive
				Thread.sleep(500);
			} catch (InterruptedException e) {
				throw fail();
			} finally {
				INTERRUPT_NONCONSUMING_SEMAPHORE.release();
			}
		}).start();
		s.interruptNonConsuming();
		assertTrue(currentthread.isInterrupted());

		//clear current thread interrupt status
		Thread.interrupted();
		assertException(RuntimeException.class, s::throwingInterruptSetting);
		assertTrue(currentthread.isInterrupted());

		//clear the flag after the test
		Thread.interrupted();
	}
}
