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

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;

import testing.saker.SakerTest;

@SakerTest
public class ExternalizableConstructorRMITest extends BaseVariablesRMITestCase {

	public static class MyClass implements Externalizable {
		private static final long serialVersionUID = 1L;
		private String val;

		public MyClass() {
		}

		public MyClass(String val) {
			this.val = val;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeUTF(val);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			val = in.readUTF();
		}

		@Override
		public String toString() {
			return getClass().getName() + "[" + val + "]";
		}
	}

	public static void call(MyClass c) {
		System.out.println("ExternalizableConstructorRMITest.call() " + c);
	}

	@Override
	protected void runVariablesTestImpl() throws AssertionError {
		try {
			clientVariables.invokeRemoteStaticMethod(
					ExternalizableConstructorRMITest.class.getDeclaredMethod("call", MyClass.class),
					new MyClass("Hello"));
		} catch (NoSuchMethodException | SecurityException e) {
			throw new AssertionError(e);
		} catch (InvocationTargetException e) {
			assertEquals(e.getTargetException().getClass(), InvalidClassException.class);
		}
	}

}
