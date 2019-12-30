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
