package testing.saker.build.tests.rmi;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Map;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMIServer;
import saker.rmi.connection.RMIVariables;
import saker.util.io.IOUtils;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class SSLSecurityRMITest extends SakerTestCase {
	//Java 11+ throws javax.net.ssl.SSLHandshakeException: No available authentication scheme if we don't set TLSv1.2
	//XXX handle java 11 too

	static final class TLSv1_2ServerSocketFactory extends ServerSocketFactory {
		private final SSLServerSocketFactory scserversocketfactory;

		TLSv1_2ServerSocketFactory(SSLServerSocketFactory scserversocketfactory) {
			this.scserversocketfactory = scserversocketfactory;
		}

		@Override
		public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
			SSLServerSocket result = (SSLServerSocket) scserversocketfactory.createServerSocket(port, backlog,
					ifAddress);
			result.setEnabledProtocols(new String[] { "TLSv1.2" });
			return result;
		}

		@Override
		public ServerSocket createServerSocket(int port, int backlog) throws IOException {
			SSLServerSocket result = (SSLServerSocket) scserversocketfactory.createServerSocket(port, backlog);
			result.setEnabledProtocols(new String[] { "TLSv1.2" });
			return result;
		}

		@Override
		public ServerSocket createServerSocket(int port) throws IOException {
			SSLServerSocket result = (SSLServerSocket) scserversocketfactory.createServerSocket(port);
			result.setEnabledProtocols(new String[] { "TLSv1.2" });
			return result;
		}
	}

	static final class TLSv1_2SocketFactory extends SocketFactory {
		private final SSLSocketFactory scsocketfactory;

		TLSv1_2SocketFactory(SSLSocketFactory scsocketfactory) {
			this.scsocketfactory = scsocketfactory;
		}

		@Override
		public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
				throws IOException {
			SSLSocket result = (SSLSocket) scsocketfactory.createSocket(address, port, localAddress, localPort);
			result.setEnabledProtocols(new String[] { "TLSv1.2" });
			return result;
		}

		@Override
		public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
				throws IOException, UnknownHostException {
			SSLSocket result = (SSLSocket) scsocketfactory.createSocket(host, port, localHost, localPort);
			result.setEnabledProtocols(new String[] { "TLSv1.2" });
			return result;
		}

		@Override
		public Socket createSocket(InetAddress host, int port) throws IOException {
			SSLSocket result = (SSLSocket) scsocketfactory.createSocket(host, port);
			result.setEnabledProtocols(new String[] { "TLSv1.2" });
			return result;
		}

		@Override
		public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
			SSLSocket result = (SSLSocket) scsocketfactory.createSocket(host, port);
			result.setEnabledProtocols(new String[] { "TLSv1.2" });
			return result;
		}

		@Override
		public Socket createSocket() throws IOException {
			SSLSocket result = (SSLSocket) scsocketfactory.createSocket();
			result.setEnabledProtocols(new String[] { "TLSv1.2" });
			return result;
		}
	}

	public interface Stub {
		public String f(String s);
	}

	public static class Impl implements Stub {
		@Override
		public String f(String s) {
			return s + s;
		}
	}

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		testHandshakeException();
		testSSL();
		testServerOnlyPrivateKeySSL();
	}

	private static void testServerOnlyPrivateKeySSL() throws Throwable {
		RMIServer server = null;
		RMIConnection connection = null;
		Path kspath = Paths.get(SSLSecurityRMITest.class.getSimpleName()).resolve("keystore");
		Path publiccerpath = Paths.get(SSLSecurityRMITest.class.getSimpleName()).resolve("public.cer");

		try {
			ServerSocketFactory serversocketfactory;
			SocketFactory socketfactory;

			{
				KeyStore ks = KeyStore.getInstance("JKS");

				char[] password = "testtest".toCharArray();
				try (InputStream is = Files.newInputStream(kspath)) {
					ks.load(is, password);
				}

				SSLContext sc = SSLContext.getInstance("SSL");
				KeyManagerFactory kmfactory = KeyManagerFactory.getInstance("SunX509");
				kmfactory.init(ks, password);

				TrustManagerFactory tmfactory = TrustManagerFactory.getInstance("SunX509");
				tmfactory.init(ks);

				KeyManager[] keymanagers = kmfactory.getKeyManagers();
				TrustManager[] trusmanagers = tmfactory.getTrustManagers();
				sc.init(keymanagers, trusmanagers, new SecureRandom());
				serversocketfactory = new TLSv1_2ServerSocketFactory(sc.getServerSocketFactory());
			}
			{
				//command to export public key:
				//    keytool -export -keystore examplestore -alias signFiles -file Example.cer
				KeyStore ks = KeyStore.getInstance("JKS");

				Certificate cer;
				try (InputStream is = Files.newInputStream(publiccerpath)) {
					CertificateFactory fact = CertificateFactory.getInstance("X.509");
					cer = fact.generateCertificate(is);
				}
				ks.load(null, null);
				ks.setCertificateEntry("test", cer);

				SSLContext sc = SSLContext.getInstance("SSL");
				KeyManagerFactory kmfactory = KeyManagerFactory.getInstance("SunX509");
				kmfactory.init(ks, null);

				TrustManagerFactory tmfactory = TrustManagerFactory.getInstance("SunX509");
				tmfactory.init(ks);

				KeyManager[] keymanagers = kmfactory.getKeyManagers();
				TrustManager[] trusmanagers = tmfactory.getTrustManagers();
				sc.init(keymanagers, trusmanagers, new SecureRandom());
				socketfactory = new TLSv1_2SocketFactory(sc.getSocketFactory());
			}
			RMIOptions options = new RMIOptions().classLoader(SSLSecurityRMITest.class.getClassLoader());
			server = new RMIServer(serversocketfactory, 0, InetAddress.getLoopbackAddress()) {
				@Override
				protected RMIOptions getRMIOptionsForAcceptedConnection(Socket acceptedsocket, int protocolversion) {
					return options;
				}
			};
			SocketAddress sockaddress = server.getLocalSocketAddress();
			server.start();
			connection = options.connect(socketfactory, sockaddress);

			try (RMIVariables vars = connection.newVariables()) {
				Stub s = (Stub) vars.newRemoteInstance(Impl.class);
				assertEquals(s.f("x"), "xx");
			}
		} finally {
			IOUtils.close(connection, server);
		}
	}

	private static void testSSL() throws Throwable {
		RMIServer server = null;
		RMIConnection connection = null;
		try {
			KeyStore ks = KeyStore.getInstance("JKS");

			Path kspath = Paths.get(SSLSecurityRMITest.class.getSimpleName()).resolve("keystore");
			char[] password = "testtest".toCharArray();
			try (InputStream is = Files.newInputStream(kspath)) {
				ks.load(is, password);
			}

			SSLContext sc = SSLContext.getInstance("SSL");
			KeyManagerFactory kmfactory = KeyManagerFactory.getInstance("SunX509");
			kmfactory.init(ks, password);

			TrustManagerFactory tmfactory = TrustManagerFactory.getInstance("SunX509");
			tmfactory.init(ks);

			KeyManager[] keymanagers = kmfactory.getKeyManagers();
			TrustManager[] trusmanagers = tmfactory.getTrustManagers();
			sc.init(keymanagers, trusmanagers, new SecureRandom());
			SSLServerSocketFactory scserversocketfactory = sc.getServerSocketFactory();
			ServerSocketFactory serversocketfactory = new TLSv1_2ServerSocketFactory(scserversocketfactory);
			SSLSocketFactory scsocketfactory = sc.getSocketFactory();
			SocketFactory socketfactory = new TLSv1_2SocketFactory(scsocketfactory);
			RMIOptions options = new RMIOptions().classLoader(SSLSecurityRMITest.class.getClassLoader());
			server = new RMIServer(serversocketfactory, 0, InetAddress.getLoopbackAddress()) {
				@Override
				protected RMIOptions getRMIOptionsForAcceptedConnection(Socket acceptedsocket, int protocolversion) {
					return options;
				}
			};
			SocketAddress sockaddress = server.getLocalSocketAddress();
			server.start();
			connection = options.connect(socketfactory, sockaddress);

			try (RMIVariables vars = connection.newVariables()) {
				Stub s = (Stub) vars.newRemoteInstance(Impl.class);
				assertEquals(s.f("x"), "xx");
			}
		} finally {
			IOUtils.close(connection, server);
		}
	}

	private static void testHandshakeException() throws Throwable {
		RMIServer server = null;
		RMIConnection connection = null;
		try {

			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, null, new SecureRandom());
			ServerSocketFactory serversocketfactory = new TLSv1_2ServerSocketFactory(sc.getServerSocketFactory());
			SocketFactory socketfactory = new TLSv1_2SocketFactory(sc.getSocketFactory());
			RMIOptions options = new RMIOptions().classLoader(SSLSecurityRMITest.class.getClassLoader());
			server = new RMIServer(serversocketfactory, 0, InetAddress.getLoopbackAddress()) {
				@Override
				protected RMIOptions getRMIOptionsForAcceptedConnection(Socket acceptedsocket, int protocolversion) {
					return options;
				}
			};
			SocketAddress sockaddress = server.getLocalSocketAddress();
			server.start();
			try {
				connection = options.connect(socketfactory, sockaddress);
				fail();
			} catch (SSLHandshakeException e) {
			}
		} finally {
			IOUtils.close(connection, server);
		}
	}

}
