package testing.saker.build.tests.rmi;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import saker.rmi.annot.transfer.RMIWrap;
import saker.util.io.ByteArrayRegion;
import saker.util.io.ByteRegion;
import saker.util.io.ByteSource;
import saker.util.rmi.wrap.RMIByteRegionWrapper;
import testing.saker.SakerTest;

@SakerTest
public class RMIWrapAnnotTest extends BaseVariablesRMITestCase {

	public static class FillerByteSource implements ByteSource {

		@Override
		public int read(ByteRegion buffer) throws IOException {
			for (int i = 0; i < buffer.getLength(); i++) {
				buffer.put(i, (byte) 123);
			}
			return buffer.getLength();
		}

	}

	@RMIWrap(RMIByteRegionWrapper.class)
	public static ByteRegion makeRegion(byte[] bytes) {
		return ByteArrayRegion.wrap(bytes);
	}

	public static ByteRegion getBack(@RMIWrap(RMIByteRegionWrapper.class) ByteRegion region) {
		return region;
	}

	@RMIWrap(RMIByteRegionWrapper.class)
	public static ByteRegion wrapBack(ByteRegion region) {
		return region;
	}

	@RMIWrap(RMIByteRegionWrapper.class)
	public static ByteRegion wrapThrough(@RMIWrap(RMIByteRegionWrapper.class) ByteRegion region) {
		return region;
	}

	@Override
	protected void runVariablesTestImpl() throws AssertionError {
		try {
			RMIByteRegionWrapper region = (RMIByteRegionWrapper) clientVariables.invokeRemoteStaticMethod(
					RMIWrapAnnotTest.class.getMethod("makeRegion", byte[].class),
					"hello".getBytes(StandardCharsets.UTF_8));
			assertEquals(region.getClass(), RMIByteRegionWrapper.class);
			assertEquals(region.copy(), "hello".getBytes(StandardCharsets.UTF_8));

			ByteSource src = (ByteSource) clientVariables.newRemoteInstance(FillerByteSource.class);
			byte[] buf = new byte[10];
			byte[] test = buf.clone();
			Arrays.fill(test, (byte) 123);
			src.read(ByteArrayRegion.wrap(buf));

			assertEquals(buf, test, () -> Arrays.toString(buf) + " - " + Arrays.toString(test));

			Method gbmethod = RMIWrapAnnotTest.class.getMethod("getBack", ByteRegion.class);

			ByteArrayRegion localbar = ByteArrayRegion.wrap(buf);
			Object gb2 = clientVariables.invokeRemoteStaticMethod(gbmethod, localbar);
			assertIdentityEquals(gb2, localbar);

			RMIByteRegionWrapper gb1 = (RMIByteRegionWrapper) clientVariables
					.invokeRemoteStaticMethod(RMIWrapAnnotTest.class.getMethod("wrapBack", ByteRegion.class), region);
			assertIdentityEquals(gb1.getWrappedObject(), region.getWrappedObject(),
					() -> gb1.getClass() + " - " + region.getClass());

			ByteRegion wt1 = (ByteRegion) clientVariables.invokeRemoteStaticMethod(
					RMIWrapAnnotTest.class.getMethod("wrapThrough", ByteRegion.class), localbar);
			assertIdentityEquals(localbar, wt1, () -> localbar.getClass() + " - " + wt1.getClass());

			RMIByteRegionWrapper wt2 = (RMIByteRegionWrapper) clientVariables.invokeRemoteStaticMethod(
					RMIWrapAnnotTest.class.getMethod("wrapThrough", ByteRegion.class), region);
			assertIdentityEquals(region.getWrappedObject(), wt2.getWrappedObject(),
					() -> region.getClass() + " - " + wt2.getClass());

		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

}
