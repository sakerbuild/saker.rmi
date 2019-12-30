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
