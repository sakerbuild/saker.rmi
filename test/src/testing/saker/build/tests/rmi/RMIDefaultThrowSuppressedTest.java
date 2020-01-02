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
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;

import saker.rmi.annot.invoke.RMIDefaultOnFailure;
import saker.rmi.exception.RMICallFailedException;
import saker.rmi.exception.RMIObjectTransferFailureException;
import testing.saker.SakerTest;

@SakerTest
@SuppressWarnings("unused")
public class RMIDefaultThrowSuppressedTest extends BaseVariablesRMITestCase {

	public interface Stub {
		@RMIDefaultOnFailure
		public default Object function(int i, long l, double d, Object o, Object param) {
			//some parameters to verify proxy generation
			throw new UnsupportedOperationException();
		}
	}

	public static class StubImpl implements Stub {

	}

	public static class Unwritable implements Externalizable {
		private static final long serialVersionUID = 1L;

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			throw new IOException("Write failed.");
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			throw new IOException("Read failed.");
		}

	}

	@Override
	protected void runVariablesTestImpl() throws AssertionError {
		try {
			Stub s = (Stub) clientVariables.newRemoteInstance(StubImpl.class);
			UnsupportedOperationException e = assertException(UnsupportedOperationException.class,
					() -> s.function(0, 0, 0, null, new Unwritable()));
			assertEquals(RMIObjectTransferFailureException.class, e.getSuppressed()[0].getClass());
		} catch (RMICallFailedException | InvocationTargetException | NoSuchMethodException e) {
			throw new AssertionError(e);
		}
	}

}
