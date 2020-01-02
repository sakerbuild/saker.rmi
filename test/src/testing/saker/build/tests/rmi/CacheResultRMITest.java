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
import testing.saker.SakerTest;

@SakerTest
public class CacheResultRMITest extends BaseVariablesRMITestCase {
	private static int method1CallCount = 0;
	private static int methodParamsCallCount = 0;
	private static int methodLongCallCount = 0;

	public interface Stub {
		@RMICacheResult
		public Object f();

		@RMICacheResult
		public Object f(Object param1, Object param2);

		//to test if primitives can be cached
		@RMICacheResult
		public long l();
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

	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		method1CallCount = 0;
		methodParamsCallCount = 0;
		methodLongCallCount = 0;

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
	}

}
