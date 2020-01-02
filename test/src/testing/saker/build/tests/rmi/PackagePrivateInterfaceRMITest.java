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
public class PackagePrivateInterfaceRMITest extends BaseVariablesRMITestCase {
	public interface PublicStub {
		public default String pub() {
			return "pub";
		}
	}

	private interface PrivateStub {
		public default String priv() {
			return "priv";
		}
	}

	protected interface ProtectedStub {
		public default String prot() {
			return "prot";
		}
	}

	interface PackageStub {
		public default String pack() {
			return "pack";
		}
	}

	public static class Impl implements PublicStub, PrivateStub, ProtectedStub, PackageStub {
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Object impl = clientVariables.newRemoteInstance(Impl.class);
		assertInstanceOf(impl, PublicStub.class);
		assertNotInstanceOf(impl, PrivateStub.class);
		assertNotInstanceOf(impl, ProtectedStub.class);
		assertNotInstanceOf(impl, PackageStub.class);
	}

}
