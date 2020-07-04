/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package testing.saker.build.tests.rmi;

import saker.rmi.annot.invoke.RMICacheResult;
import saker.util.thread.ThreadUtils;
import testing.saker.SakerTest;
import testing.saker.build.tests.ExecutionOrderer;

@SakerTest
public class CacheResultRMITest extends BaseVariablesRMITestCase {
	private static int method1CallCount = 0;
	private static int methodParamsCallCount = 0;
	private static int methodLongCallCount = 0;

	private static int methodFailingCallCount = 0;
	private static int methodOrderedCallCount = 0;
	private static int methodRecursiveCallCount = 0;

	private static ExecutionOrderer orderer;

	public interface Stub {
		@RMICacheResult
		public Object f();

		@RMICacheResult
		public Object f(Object param1, Object param2);

		//to test if primitives can be cached
		@RMICacheResult
		public long l();

		@RMICacheResult
		public int failing();

		@RMICacheResult
		public int ordered();

		@RMICacheResult
		public int recursive();
	}

	public static class Impl implements Stub {

		@Override
		public Object f() {
			method1CallCount++;
			return new Object();
		}

		@Override
		public Object f(Object param1, Object param2) {
			methodParamsCallCount++;
			return new Object();
		}

		@Override
		public long l() {
			++methodLongCallCount;
			return 99;
		}

		@Override
		public int failing() {
			++methodFailingCallCount;
			throw new UnsupportedOperationException();
		}

		@Override
		public int ordered() {
			++methodOrderedCallCount;
			try {
				orderer.enter("in");
				orderer.enter("got");
				Thread.sleep(500);
			} catch (InterruptedException e) {
				fail(e);
			}
			return 0;
		}

		@Override
		public int recursive() {
			if (++methodRecursiveCallCount == 3) {
				return 10;
			}
			return recursive();
		}

	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		method1CallCount = 0;
		methodParamsCallCount = 0;
		methodLongCallCount = 0;
		methodFailingCallCount = 0;
		methodOrderedCallCount = 0;
		methodRecursiveCallCount = 0;

		orderer = new ExecutionOrderer();
		orderer.addSection("in");
		orderer.addSection("started");
		orderer.addSection("got");

		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
		Object c1res = s.f();
		assertEquals(method1CallCount, 1);
		Object c2res = s.f();
		assertEquals(method1CallCount, 1);
		assertIdentityEquals(c1res, c2res);

		Object param1 = s.f(null, "hello");
		assertEquals(methodParamsCallCount, 1);
		Object param2 = s.f(null, "world");
		assertEquals(methodParamsCallCount, 1);
		assertIdentityEquals(param1, param2);

		assertEquals(methodLongCallCount, 0);
		assertEquals(s.l(), 99L);
		assertEquals(methodLongCallCount, 1);
		assertEquals(s.l(), 99L);
		assertEquals(methodLongCallCount, 1);

		assertException(UnsupportedOperationException.class, () -> {
			s.failing();
		});
		assertEquals(methodFailingCallCount, 1);
		assertException(UnsupportedOperationException.class, s::failing);
		assertEquals(methodFailingCallCount, 2);

		Thread dt = ThreadUtils.startDaemonThread(() -> {
			try {
				orderer.enter("started");
				s.ordered();
			} catch (InterruptedException e) {
				fail(e);
			}

		});
		s.ordered();
		dt.join();
		assertEquals(methodOrderedCallCount, 1);

		s.recursive();
		assertEquals(methodRecursiveCallCount, 3);
	}

}
