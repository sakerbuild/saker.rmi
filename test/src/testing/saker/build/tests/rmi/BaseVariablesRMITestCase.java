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

import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMIVariables;

public abstract class BaseVariablesRMITestCase extends BaseRMITestCase {
	protected RMIVariables clientVariables;
	protected RMIVariables serverVariables;

	@Override
	protected final void runTestImpl() throws Exception {
		try (RMIVariables cv = clientConnection.newVariables()) {
			clientVariables = cv;
			serverVariables = RMITestUtil.getCorrespondingConnectionVariables(serverConnection, cv);
			runVariablesTestImpl();
		} finally {
			clientVariables = null;
			serverVariables = null;
		}
	}

	protected abstract void runVariablesTestImpl() throws Exception;

}
