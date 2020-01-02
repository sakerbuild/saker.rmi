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

import java.util.function.Predicate;

import saker.util.function.Functionals;
import testing.saker.SakerTest;

@SakerTest
public class InterfaceEnumWriteRMITest extends BaseVariablesRMITestCase {

	public interface Stub {
		public void f(Predicate<?> pred);
	}

	public static class Impl implements Stub {
		@Override
		public void f(Predicate<?> pred) {
			assertIdentityEquals(pred, Functionals.alwaysPredicate());
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
		s.f(Functionals.alwaysPredicate());
	}
}
