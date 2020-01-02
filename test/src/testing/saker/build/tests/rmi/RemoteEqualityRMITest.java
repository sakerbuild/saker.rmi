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

import java.lang.reflect.InvocationTargetException;

import testing.saker.SakerTest;

@SakerTest
public class RemoteEqualityRMITest extends BaseVariablesRMITestCase {

	public static void equality(Runnable r1, Runnable r2) throws AssertionError {
		assertIdentityEquals(r1, r2);
	}

	@Override
	protected void runVariablesTestImpl() throws AssertionError {
		Runnable r = () -> {
		};
		try {
			clientVariables.invokeRemoteStaticMethod(
					RemoteEqualityRMITest.class.getMethod("equality", Runnable.class, Runnable.class), r, r);
		} catch (InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new AssertionError(e);
		}
	}

}
