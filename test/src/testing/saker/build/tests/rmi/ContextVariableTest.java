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
public class ContextVariableTest extends BaseVariablesRMITestCase {
	public interface Stub {
		public default int f() {
			return 123;
		}
	}

	public static class Impl implements Stub {
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Impl impl = new Impl();
		serverConnection.putContextVariable("stub", impl);
		serverConnection.putContextVariable("stub2", impl);

		Stub a = (Stub) clientVariables.getRemoteContextVariable("stub");
		Stub b = (Stub) clientVariables.getRemoteContextVariable("stub");
		Stub s2 = (Stub) clientVariables.getRemoteContextVariable("stub2");
		assertNonNull(a);
		assertEquals(a.f(), 123);
		assertIdentityEquals(a, b);
		assertIdentityEquals(a, s2);
	}

}
