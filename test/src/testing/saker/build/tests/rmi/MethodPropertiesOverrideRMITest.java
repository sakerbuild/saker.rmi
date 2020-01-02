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

import java.io.Serializable;

import saker.rmi.annot.transfer.RMIRemote;
import saker.rmi.annot.transfer.RMISerialize;
import saker.rmi.connection.RMIConnection;
import testing.saker.SakerTest;

/**
 * Test to make sure that when different method properties are defined in an interface hierarchy, then the appropriate
 * invocation happens.
 */
@SakerTest
public class MethodPropertiesOverrideRMITest extends BaseVariablesRMITestCase {

	public interface SuperStub {
		@RMIRemote
		public Object function();
	}

	public interface Stub extends SuperStub {
		@RMISerialize
		@Override
		public Object function();
	}

	public static class Impl implements Stub {
		@Override
		public Object function() {
			return new MyRunnable();
		}
	}

	public static class SuperImpl implements SuperStub {

		@Override
		public Object function() {
			return new MyRunnable();
		}

	}

	public static class MyRunnable implements Runnable, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public void run() {
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
		assertTrue(!RMIConnection.isRemoteObject(s.function()));

		SuperStub ss = (SuperStub) clientVariables.newRemoteInstance(SuperImpl.class);
		assertTrue(RMIConnection.isRemoteObject(ss.function()));
	}

}
