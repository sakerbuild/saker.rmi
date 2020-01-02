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

import java.lang.reflect.Method;

import saker.rmi.connection.RMIVariables;
import saker.util.ReflectUtils;
import testing.saker.SakerTest;
import testing.saker.build.tests.ExecutionOrderer;

@SakerTest
public class RMIAsyncMethodInvokeTest extends BaseVariablesRMITestCase {
	private static ExecutionOrderer orderer;

	public interface Stub {
		public void f() throws InterruptedException;
	}

	public static class Impl implements Stub {
		@Override
		public void f() throws InterruptedException {
			System.out.println("RMIAsyncMethodInvokeTest.Impl.f()");
			orderer.enter("f");
		}
	}

	@Override
	protected void runVariablesTestImpl() throws Exception {
		orderer = new ExecutionOrderer();
		orderer.addSection("call");
		orderer.addSection("endcall");
		orderer.addSection("f");

		Stub s = (Stub) clientVariables.newRemoteInstance(Impl.class);
		Method fmethod = ReflectUtils.getMethodAssert(Stub.class, "f");
		orderer.enter("call");
		RMIVariables.invokeRemoteMethodAsync(s, fmethod);
		orderer.enter("endcall");
	}

}
