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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.Map;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIConnection.IOErrorListener;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMIServer;
import saker.rmi.connection.RMITestUtil;
import saker.rmi.connection.RMIVariables;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class ServerConnectRMITest extends SakerTestCase {

	public interface Stub {
		public String f(String s);
	}

	public static class Impl implements Stub {
		@Override
		public String f(String s) {
			String result = s + s;
			System.err.println("SSLSecurityRMITest.Impl.f() " + result);
			System.out.println("SSLSecurityRMITest.Impl.f() " + result);
			return result;
		}
	}

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		testMultiStream();
		testSingleStream();
	}

	private static void testSingleStream() throws Exception {
		RMIOptions options = new RMIOptions().classLoader(ServerConnectRMITest.class.getClassLoader())
				.maxStreamCount(1);
		try (RMIServer server = new RMIServerWithOptions(options)) {
			server.start();
			try (RMIConnection connection = options.connect(server.getLocalSocketAddress())) {
				System.err.println("Opened connection: " + Integer.toHexString(System.identityHashCode(connection)));
				for (int j = 0; j < 4; j++) {
					testConnection(connection);
				}
				//expect the connection to have only a single stream, as only at most one RMIVariables are alive at one point
				assertEquals(RMITestUtil.getConnectionStreamCount(connection), 1);

				//check that creating multiple RMI variables won't cause new streams to be created, as the max is 1
				try (RMIVariables vars1 = connection.newVariables()) {
					Stub s = (Stub) vars1.newRemoteInstance(Impl.class);
					assertEquals(s.f("x"), "xx");
					try (RMIVariables vars2 = connection.newVariables()) {
						Stub s2 = (Stub) vars2.newRemoteInstance(Impl.class);
						assertEquals(s2.f("y"), "yy");
						try (RMIVariables vars3 = connection.newVariables()) {
							Stub s3 = (Stub) vars3.newRemoteInstance(Impl.class);
							assertEquals(s3.f("z"), "zz");
						}
					}
				}
				//expect that we opened one more connection for the additional RMIVariables
				assertEquals(RMITestUtil.getConnectionStreamCount(connection), 1);
			}
			server.closeWait();
		}
	}

	private static void testMultiStream() throws Exception {
		RMIOptions options = new RMIOptions().classLoader(ServerConnectRMITest.class.getClassLoader())
				.maxStreamCount(10);

		//this is invoked multiple times as there was a bug that caused additional streams to fail
		for (int i = 0; i < 200; i++) {
			System.err.println();
			System.err.println("Run " + (i + 1));
			try (RMIServer server = new RMIServerWithOptions(options)) {
				server.start();
				try (RMIConnection connection = options.connect(server.getLocalSocketAddress())) {
					System.err
							.println("Opened connection: " + Integer.toHexString(System.identityHashCode(connection)));
					for (int j = 0; j < 4; j++) {
						testConnection(connection);
					}
					//expect the connection to have only a single stream, as only at most one RMIVariables is alive at one point
					assertEquals(RMITestUtil.getConnectionStreamCount(connection), 1);

					//check that creating multiple RMI variables will cause additional streams to be connected
					try (RMIVariables vars1 = connection.newVariables()) {
						Stub s = (Stub) vars1.newRemoteInstance(Impl.class);
						assertEquals(s.f("x"), "xx");
						try (RMIVariables vars2 = connection.newVariables()) {
							Stub s2 = (Stub) vars2.newRemoteInstance(Impl.class);
							assertEquals(s2.f("y"), "yy");
							try (RMIVariables vars3 = connection.newVariables()) {
								Stub s3 = (Stub) vars3.newRemoteInstance(Impl.class);
								assertEquals(s3.f("z"), "zz");
							}
						}
					}
					//expect that we opened one more connection for the additional RMIVariables
					assertEquals(RMITestUtil.getConnectionStreamCount(connection), 3);
				}
				server.closeWait();
			}
		}
	}

	private static RMIConnection testConnection(RMIConnection connection)
			throws InvocationTargetException, NoSuchMethodException, AssertionError {
		RuntimeException originexc = new RuntimeException("IO error listener called.");
		connection.addErrorListener(new IOErrorListener() {
			@Override
			public void onIOError(Throwable exc) {
				originexc.addSuppressed(new RuntimeException("IO error: " + Thread.currentThread().getName(), exc));
				originexc.printStackTrace();
			}
		});
		try (RMIVariables vars = connection.newVariables()) {
			Stub s = (Stub) vars.newRemoteInstance(Impl.class);
			assertEquals(s.f("x"), "xx");
		}
		return connection;
	}

	private static final class RMIServerWithOptions extends RMIServer {
		private final RMIOptions options;

		private RMIServerWithOptions(RMIOptions options) throws IOException {
			this.options = options;
		}

		@Override
		protected RMIOptions getRMIOptionsForAcceptedConnection(Socket acceptedsocket, int protocolversion)
				throws IOException, RuntimeException {
			return options;
		}
	}

}
