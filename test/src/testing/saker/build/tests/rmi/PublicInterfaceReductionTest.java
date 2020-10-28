package testing.saker.build.tests.rmi;

import java.util.Map;

import saker.rmi.connection.RMITestUtil;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

/**
 * Simple test to check that public superinterface of a non accessible interface class is till present in the interface
 * set for proxy generation.
 */
@SakerTest
public class PublicInterfaceReductionTest extends SakerTestCase {

	public interface PubItf {
	}

	private interface PrivItf extends PubItf {
	}

	public interface SubPrivItf extends PrivItf {
	}

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		assertEquals(RMITestUtil.getPublicInterfaceSetOf(PrivItf.class), setOf(PubItf.class));
		assertEquals(RMITestUtil.getPublicInterfaceSetOf(SubPrivItf.class), setOf(SubPrivItf.class));
	}

}
