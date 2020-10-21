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
import saker.rmi.connection.RMIVariables;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;
import testing.saker.build.tests.rmi.ssl.SSLSecurityRMITest;

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
		RMIOptions options = getOptions();

		//this is invoked multiple times as there was a bug that caused additional streams to fail
		for (int i = 0; i < 1000; i++) {
			System.err.println();
			System.err.println("Run " + (i + 1));
			try (RMIServer server = new RMIServer() {
				@Override
				protected RMIOptions getRMIOptionsForAcceptedConnection(Socket acceptedsocket, int protocolversion)
						throws IOException, RuntimeException {
					return options;
				}
			}) {
				server.start();
				try (RMIConnection conn = options.connect(server.getLocalSocketAddress())) {
					System.err.println("Opened connection: " + Integer.toHexString(System.identityHashCode(conn)));
					testConnection(conn);
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

	private static RMIOptions getOptions() {
		//use more than 1 max stream count
		return new RMIOptions().classLoader(SSLSecurityRMITest.class.getClassLoader()).maxStreamCount(10);
	}

}
