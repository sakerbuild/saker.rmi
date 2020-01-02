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
import java.util.Set;
import java.util.TreeSet;

import saker.rmi.annot.transfer.RMISerialize;
import testing.saker.SakerTest;

@SakerTest
public class RMISerializeAnnotTest extends BaseVariablesRMITestCase {
	public interface SuperItf {
		@RMISerialize
		public Set<String> method(@RMISerialize Set<String> param1, Set<String> param2) throws AssertionError;
	}

	public static class Impl implements SuperItf {

		@Override
		public Set<String> method(Set<String> param1, Set<String> param2) throws AssertionError {
			assertEquals(param1.getClass(), TreeSet.class);
			assertNotEquals(param2.getClass(), TreeSet.class);
			return new TreeSet<>();
		}

	}

	@Override
	protected void runVariablesTestImpl() throws AssertionError {
		try {
			SuperItf instance = (SuperItf) clientVariables.newRemoteInstance(Impl.class);
			Set<String> res = instance.method(new TreeSet<>(), new TreeSet<>());
			assertEquals(res.getClass(), TreeSet.class);
		} catch (InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new AssertionError(e);
		}
	}

}
