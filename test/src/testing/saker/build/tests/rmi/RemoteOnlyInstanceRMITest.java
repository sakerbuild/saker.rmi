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
public class RemoteOnlyInstanceRMITest extends BaseVariablesRMITestCase {
	public interface Stub {

	}

	public static class Impl implements Stub {
		public Impl() {
		}

		public Impl(Object o) {
		}

		public Impl(int i) {
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		serverConnection.putContextVariable("cl", Impl.class.getClassLoader());
		Object cl = clientVariables.getRemoteContextVariable("cl");

		clientVariables.newRemoteOnlyInstance(cl, Impl.class.getName(), new String[] {}, new Object[] {});
		clientVariables.newRemoteOnlyInstance(cl, Impl.class.getName(), new String[] { Object.class.getName() },
				new Object[] { 1 });
		//make sure primitive class names are looked up accordingly
		clientVariables.newRemoteOnlyInstance(cl, Impl.class.getName(), new String[] { int.class.getName() },
				new Object[] { 1 });
	}

}
