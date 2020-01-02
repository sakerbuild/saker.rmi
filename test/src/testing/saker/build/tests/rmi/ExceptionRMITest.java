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

import testing.saker.SakerTest;

@SakerTest
public class ExceptionRMITest extends BaseVariablesRMITestCase {

	public static class MyException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	public static void throwRuntime() throws MyException {
		throw new MyException();
	}

	public static class InstantiationThrower {
		public InstantiationThrower() {
			throw new MyException();
		}
	}

	public static class CallThrower implements Runnable {
		@Override
		public void run() {
			throw new MyException();
		}
	}

	@Override
	protected void runVariablesTestImpl() {
		assertException(MyException.class, unwrapInvocationTargetException(
				() -> clientVariables.invokeRemoteStaticMethod(ExceptionRMITest.class.getMethod("throwRuntime")))::run);
		assertException(MyException.class, unwrapInvocationTargetException(
				() -> clientVariables.newRemoteInstance(InstantiationThrower.class))::run);
		assertException(MyException.class,
				() -> ((Runnable) clientVariables.newRemoteInstance(CallThrower.class)).run());
	}

}
