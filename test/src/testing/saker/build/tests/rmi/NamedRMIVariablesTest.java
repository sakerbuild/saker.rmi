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

import saker.rmi.connection.RMIVariables;
import testing.saker.SakerTest;

@SakerTest
public class NamedRMIVariablesTest extends BaseRMITestCase {
	@SuppressWarnings("resource")
	@Override
	protected void runTestImpl() {
		RMIVariables v1;
		RMIVariables v2;
		try (RMIVariables vars1 = clientConnection.getVariables("vars")) {
			v1 = vars1;
			try (RMIVariables vars2 = clientConnection.getVariables("vars")) {
				v2 = vars2;
				assertTrue(!v1.isClosed());
				assertTrue(!v2.isClosed());
				assertIdentityEquals(vars1, vars2);
			}
			assertTrue(!v1.isClosed());
			assertTrue(!v2.isClosed());
		}
		assertTrue(v1.isClosed());
		assertTrue(v2.isClosed());
		try (RMIVariables vars1 = clientConnection.getVariables("vars")) {
			//as it was closed, we now need to get a new instance
			assertNotIdentityEquals(v1, vars1);
			v1 = vars1;
		}
		assertTrue(v1.isClosed());
	}
}
