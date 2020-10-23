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
package testing.saker.build.tests.rmi.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIConnection.IOErrorListener;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMIServer;
import saker.rmi.connection.RMIVariables;
import saker.util.ObjectUtils;
import saker.util.StringUtils;
import saker.util.io.IOUtils;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class SSLSecurityRMITest extends SakerTestCase {
	//Java 11 may throw javax.net.ssl.SSLHandshakeException: No available authentication scheme if we don't set TLSv1.2
	//  Use RSA for the generated key alg
	//  based on: https://stackoverflow.com/questions/55854904/javax-net-ssl-sslhandshakeexception-no-available-authentication-scheme

	//key generated using
	//  keytool -genkey -alias test_alias -keystore k1.jks -keyalg RSA -storepass testtest
	//k1_public.cer exported using
	//  keytool -export -keystore k1.jks -alias test_alias -file k1_public.cer -storepass testtest

	//  keytool -genkey -alias test_alias -keystore k2.jks -keyalg RSA -storepass testtest
	//  keytool -export -keystore k2.jks -alias test_alias -file k2_public.cer -storepass testtest

	private static final char[] PASSWORD = "testtest".toCharArray();

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
//		System.setProperty("javax.net.debug", "all");

		testNoCertsHandshakeException();
		testSameSSLContext();

		testServerPrivateKeySSL(null);
		testServerPrivateKeySSL(false);
		testServerPrivateKeySSL(true);

		testServerPublicKeySSL(null);
		testServerPublicKeySSL(false);
		testServerPublicKeySSL(true);

		testCASignedAuthentication(null);
		testCASignedAuthentication(false);
		testCASignedAuthentication(true);
	}

	/**
	 * This authentication method is based on server signed certificates.
	 * <p>
	 * Two keypair are generated. One for the server (CA) and one for the client. The CA keypair is used to sign the
	 * client keys, and the client uses the signed certificate to connect to the server.
	 * <p>
	 * The server can only accept certificates that it has signed, if authentication is needed. It must be turned on
	 * using the {@link SSLServerSocket#setNeedClientAuth(boolean)} method.
	 * <p>
	 * The following commands were used (on Windows) to generate and sign the certificates:
	 * 
	 * <pre>
	 * keytool -genkey -alias server_alias -keystore ca.jks -keyalg RSA -ext bc:c -dname "CN=server" -keypass testtest -storepass testtest
	 * keytool -keystore ca.jks -alias server_alias -exportcert -rfc -storepass testtest > ca.pem
	 * keytool -genkey -alias client_alias -keystore client.jks -keyalg RSA -dname "CN=client" -storepass testtest -keypass testtest
	 * keytool -keystore client.jks -alias client_alias -exportcert -rfc -storepass testtest > client.pem
	 * keytool -keystore client.jks -certreq -alias client_alias -keyalg rsa -file client.csr -storepass testtest
	 * keytool -gencert -keystore ca.jks -alias server_alias -storepass testtest -infile client.csr -ext ku:c=dig,keyEncipherment -rfc -outfile signed.pem
	 * type ca.pem signed.pem > signed_chain.pem
	 * copy client.jks client_signed.jks
	 * keytool -keystore client_signed.jks -importcert -alias client_alias -file signed_chain.pem -storepass testtest -noprompt
	 * </pre>
	 * 
	 * Replace <code>type</code> with <code>cat</code> and <code>copy</code> with <code>cp</code> for Unix based
	 * systems. Change the "testtest" passwords for your use case appropriately. The commands will ask no prompts.
	 */
	private static void testCASignedAuthentication(Boolean needsclientauth) throws Throwable {
		System.err.println("SSLSecurityRMITest.testCASignedAuthentication() needs client auth: " + needsclientauth);
		RMIServer server = null;

		try {
			SetupSSLServerSocketFactory serversocketfactory = new SetupSSLServerSocketFactory(
					getKeystoreSSLContext("ca.jks").getServerSocketFactory());
			serversocketfactory.needsClientAuth = needsclientauth;

			RMIOptions options = getOptions();
			server = new RMIServer(serversocketfactory, 0, InetAddress.getLoopbackAddress()) {
				@Override
				protected RMIOptions getRMIOptionsForAcceptedConnection(Socket acceptedsocket, int protocolversion) {
					return options;
				}
			};
			SocketAddress sockaddress = server.getLocalSocketAddress();
			server.start();

			System.err.println("SSLSecurityRMITest.testCASignedAuthentication() client signed");
			IOUtils.close(testConnection(
					options.connect(getKeystoreSSLContext("client_signed.jks").getSocketFactory(), sockaddress)));

			//we're connecting to the server with the same certificate it uses for verification
			//this is acceptable
			System.err.println("SSLSecurityRMITest.testCASignedAuthentication() ca");
			IOUtils.close(
					testConnection(options.connect(getKeystoreSSLContext("ca.jks").getSocketFactory(), sockaddress)));

			System.err.println("SSLSecurityRMITest.testCASignedAuthentication() accept all");
			if (Boolean.TRUE.equals(needsclientauth)) {
				try {
					IOUtils.close(testConnection(options.connect(getAcceptAllSocketFactory(), sockaddress)));
					fail();
				} catch (SSLException | SocketException e) {
					//both may be thrown depending on who aborts the handshake first?
				}
			} else {
				//if no auth is required, this can succeed
				IOUtils.close(testConnection(options.connect(getAcceptAllSocketFactory(), sockaddress)));
			}

			System.err.println("SSLSecurityRMITest.testCASignedAuthentication() keystore");
			//this should always fail as its not signed, and the server is not trusted
			try {
				IOUtils.close(testConnection(
						options.connect(getKeystoreSSLContext("k1.jks").getSocketFactory(), sockaddress)));
				fail();
			} catch (SSLException | SocketException e) {
				//both may be thrown depending on who aborts the handshake first?
			}

			System.err.println("SSLSecurityRMITest.testCASignedAuthentication() keystore - ca");
			if (Boolean.TRUE.equals(needsclientauth)) {
				//this should fail because the server rejects it
				try {
					SSLContext sc = SSLContext.getInstance("SSL");
					sc.init(getKeyManagers(getKeyStore("k1.jks")), getTrustManagers(getKeyStore("ca.jks")), null);
					IOUtils.close(testConnection(options.connect(sc.getSocketFactory(), sockaddress)));
					fail();
				} catch (SSLException | SocketException e) {
					//both may be thrown depending on who aborts the handshake first?
				}
			} else {
				//this should not fail, because the server doesn't need authentication, and we trust the server
				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(getKeyManagers(getKeyStore("k1.jks")), getTrustManagers(getKeyStore("ca.jks")), null);
				IOUtils.close(testConnection(options.connect(sc.getSocketFactory(), sockaddress)));
			}

			//this should be rejected by the client, as we don't trust the server
			try {
				IOUtils.close(testConnection(
						options.connect(getKeystoreSSLContext("k1.jks").getSocketFactory(), sockaddress)));
				fail();
			} catch (SSLException | SocketException e) {
				//both may be thrown depending on who aborts the handshake first?
			}
		} finally {
			IOUtils.close(server);
		}
	}

	private static RMIOptions getOptions() {
		return new RMIOptions().classLoader(SSLSecurityRMITest.class.getClassLoader()).maxStreamCount(10);
	}

	/**
	 * THIS AUTHENTICATION METHOD DOESNT WORK AT ALL.
	 */
	private static void testServerPublicKeySSL(Boolean needsclientauth) throws Throwable {
		System.err.println("SSLSecurityRMITest.testServerPublicKeySSL() needs client auth: " + needsclientauth);
		RMIServer server = null;

		try {
			SetupSSLServerSocketFactory serversocketfactory = new SetupSSLServerSocketFactory(
					getPublicKeySSLContext("k1_public.cer").getServerSocketFactory());
			serversocketfactory.needsClientAuth = needsclientauth;

			RMIOptions options = getOptions();
			server = new RMIServer(serversocketfactory, 0, InetAddress.getLoopbackAddress()) {
				@Override
				protected RMIOptions getRMIOptionsForAcceptedConnection(Socket acceptedsocket, int protocolversion) {
					return options;
				}
			};
			SocketAddress sockaddress = server.getLocalSocketAddress();
			server.start();

			//this is just fails all the way

			System.err.println("SSLSecurityRMITest.testServerPublicKeySSL() keystore");
			try {
				IOUtils.close(testConnection(
						options.connect(getKeystoreSSLContext("k1.jks").getSocketFactory(), sockaddress)));
				fail();
			} catch (SSLException | SocketException e) {
				//both may be thrown depending on who aborts the handshake first?
			}
			System.err.println("SSLSecurityRMITest.testServerPublicKeySSL() publickey");
			try {
				IOUtils.close(testConnection(options.connect(getPublicKeySocketFactory(), sockaddress)));
				fail();
			} catch (SSLException | SocketException e) {
				//both may be thrown depending on who aborts the handshake first?
			}
			System.err.println("SSLSecurityRMITest.testServerPublicKeySSL() accept all");
			try {
				IOUtils.close(testConnection(options.connect(getAcceptAllSocketFactory(), sockaddress)));
				fail();
			} catch (SSLException | SocketException e) {
				//both may be thrown depending on who aborts the handshake first?
			}
			System.err.println("SSLSecurityRMITest.testServerPublicKeySSL() accept all k1_public.cer");
			try {
				IOUtils.close(testConnection(
						options.connect(getAcceptAllSocketFactory(getX509Certificate("k1_public.cer")), sockaddress)));
				fail();
			} catch (SSLException | SocketException e) {
				//both may be thrown depending on who aborts the handshake first?
			}
		} finally {
			IOUtils.close(server);
		}
	}

	/**
	 * THIS IS NOT A SECURE AUTHENTICATION METHOD.
	 */
	private static void testServerPrivateKeySSL(Boolean needsclientauth) throws Throwable {
		System.err.println("SSLSecurityRMITest.testServerPrivateKeySSL() needs client auth: " + needsclientauth);
		RMIServer server = null;

		try {
			SetupSSLServerSocketFactory serversocketfactory = new SetupSSLServerSocketFactory(
					getKeystoreServerSocketFactory());
			serversocketfactory.needsClientAuth = needsclientauth;

			RMIOptions options = getOptions();
			server = new RMIServer(serversocketfactory, 0, InetAddress.getLoopbackAddress()) {
				@Override
				protected RMIOptions getRMIOptionsForAcceptedConnection(Socket acceptedsocket, int protocolversion) {
					return options;
				}
			};
			SocketAddress sockaddress = server.getLocalSocketAddress();
			server.start();

			System.err.println("SSLSecurityRMITest.testServerPrivateKeySSL() public key");
			if (Boolean.TRUE.equals(needsclientauth)) {
				try {
					IOUtils.close(testConnection(options.connect(getPublicKeySocketFactory(), sockaddress)));
					fail();
				} catch (SSLException | SocketException e) {
					//both may be thrown depending on who aborts the handshake first?
				}
			} else {
				//this will succeed
				IOUtils.close(testConnection(options.connect(getPublicKeySocketFactory(), sockaddress)));
			}

			System.err.println("SSLSecurityRMITest.testServerPrivateKeySSL() default");
			//this should always fail
			assertException(SSLException.class,
					() -> IOUtils.close(testConnection(options.connect(SSLSocketFactory.getDefault(), sockaddress))));

			System.err.println("SSLSecurityRMITest.testServerPrivateKeySSL() k2_public.cer");
			//this should always fail
			assertException(SSLException.class, () -> IOUtils
					.close(testConnection(options.connect(getPublicKeySocketFactory("k2_public.cer"), sockaddress))));

			System.err.println("SSLSecurityRMITest.testServerPrivateKeySSL() accept all");
			if (Boolean.TRUE.equals(needsclientauth)) {
				try {
					IOUtils.close(testConnection(options.connect(getAcceptAllSocketFactory(), sockaddress)));
					fail();
				} catch (SSLException | SocketException e) {
					//both may be thrown depending on who aborts the handshake first?
				}
			} else {
				//this will succeed
				IOUtils.close(testConnection(options.connect(getAcceptAllSocketFactory(), sockaddress)));
			}

			System.err.println("SSLSecurityRMITest.testServerPrivateKeySSL() accept all k1_public.cer");
			if (Boolean.TRUE.equals(needsclientauth)) {
				try {
					IOUtils.close(testConnection(options
							.connect(getAcceptAllSocketFactory(getX509Certificate("k1_public.cer")), sockaddress)));
					fail();
				} catch (SSLException | SocketException e) {
					//both may be thrown depending on who aborts the handshake first?
				}
			} else {
				//this will succeed
				IOUtils.close(testConnection(
						options.connect(getAcceptAllSocketFactory(getX509Certificate("k1_public.cer")), sockaddress)));
			}
		} finally {
			IOUtils.close(server);
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

	private static SocketFactory getAcceptAllSocketFactory(X509Certificate... acceptedissuers) throws Exception {
		SSLContext sc = getAcceptAllSSLContext(acceptedissuers);
		return sc.getSocketFactory();
	}

	private static SSLContext getAcceptAllSSLContext(X509Certificate... acceptedissuers)
			throws NoSuchAlgorithmException, KeyManagementException {
		KeyManager[] keymanagers = null;
		return getAcceptAllSSLContext(keymanagers, acceptedissuers);
	}

	private static SSLContext getAcceptAllSSLContext(KeyManager[] keymanagers, X509Certificate... acceptedissuers)
			throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(keymanagers, new TrustManager[] { new X509TrustManager() {

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return acceptedissuers;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				//trust
			}

			@Override
			public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				//trust
			}
		} }, null);
		return sc;
	}

	private static SSLSocketFactory getPublicKeySocketFactory() throws KeyStoreException, CertificateException,
			IOException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
		return getPublicKeySocketFactory("k1_public.cer");
	}

	private static SSLSocketFactory getPublicKeySocketFactory(String filename) throws CertificateException, IOException,
			KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
		return getPublicKeySSLContext(filename).getSocketFactory();
	}

	private static SSLContext getPublicKeySSLContext(String filename) throws CertificateException, IOException,
			KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
		X509Certificate cer = getX509Certificate(filename);
		KeyStore ks = KeyStore.getInstance("PKCS12");
		ks.load(null, null);
		ks.setCertificateEntry("test", cer);

		KeyManagerFactory kmfactory = KeyManagerFactory.getInstance("PKIX");
		kmfactory.init(ks, null);

		TrustManagerFactory tmfactory = TrustManagerFactory.getInstance("PKIX");
		tmfactory.init(ks);

		KeyManager[] keymanagers = kmfactory.getKeyManagers();
		TrustManager[] trusmanagers = tmfactory.getTrustManagers();

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(keymanagers, trusmanagers, null);
		return sc;
	}

	private static X509Certificate getX509Certificate(String filename) throws CertificateException, IOException {
		Path publiccerpath = Paths.get(SSLSecurityRMITest.class.getSimpleName()).resolve(filename);
		try (InputStream is = Files.newInputStream(publiccerpath)) {
			CertificateFactory fact = CertificateFactory.getInstance("X.509");
			return (X509Certificate) fact.generateCertificate(is);
		}
	}

	private static SSLServerSocketFactory getKeystoreServerSocketFactory() throws Exception {
		SSLContext sc = getKeystoreSSLContext("k1.jks");
		return sc.getServerSocketFactory();
	}

	private static SSLContext getKeystoreSSLContext(String filename) throws Exception {
		System.err.println("Open keystore: " + filename);
		KeyStore ks = getKeyStore(filename);

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(getKeyManagers(ks), getTrustManagers(ks), null);
		return sc;
	}

	private static TrustManager[] getTrustManagers(KeyStore ks)
			throws KeyStoreException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		TrustManagerFactory tmfactory = getTrustManagerFactory(ks);
		TrustManager[] trustmanagers = tmfactory.getTrustManagers();
		return trustmanagers;
	}

	private static TrustManagerFactory getTrustManagerFactory(KeyStore ks)
			throws KeyStoreException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		X509Certificate trustedcert = null;

		Enumeration<String> aliases = ks.aliases();
		if (!aliases.hasMoreElements()) {
			throw new IllegalArgumentException("No aliases in keystore. ");
		}
		String alias = aliases.nextElement();
		if (aliases.hasMoreElements()) {
			throw new IllegalArgumentException(
					"Too many aliases in keystore: " + alias + " and " + aliases.nextElement());
		}
		System.out.println(
				"Alias: " + alias + " is cert: " + ks.isCertificateEntry(alias) + " is key: " + ks.isKeyEntry(alias));

		Certificate[] chain = ks.getCertificateChain(alias);
		if (ObjectUtils.isNullOrEmpty(chain)) {
			throw new IllegalArgumentException(
					"Certificate chain " + (chain == null ? "not found" : "is empty") + " for alias: " + alias);
		}
		Certificate cert = chain[0];
		System.out.println("Key usage: " + Arrays.toString(((X509Certificate) cert).getKeyUsage()));
		int basicconstraints = ((X509Certificate) cert).getBasicConstraints();
		System.out.println("Basic constraints: " + basicconstraints);
		System.out.println("Chain: " + Arrays.toString(chain));
		if (basicconstraints >= 0 || chain.length < 2) {
			//this certificate is a ca, or there are no signers for it
			//use this as the anchor
			trustedcert = (X509Certificate) cert;
		} else {
			//use the next certificate for the anchor
			trustedcert = (X509Certificate) chain[1];
		}

		TrustManagerFactory tmfactory = TrustManagerFactory.getInstance("PKIX");
		System.out.println("Trusted cert is: " + trustedcert.getIssuerDN());
		PKIXBuilderParameters pkixbuilderparams = new PKIXBuilderParameters(
				Collections.singleton(new TrustAnchor(trustedcert, null)), null);
		//no need for revocation checks as we're using self signed certificates
		pkixbuilderparams.setRevocationEnabled(false);
		tmfactory.init(new CertPathTrustManagerParameters(pkixbuilderparams));
		return tmfactory;
	}

	private static KeyStore getKeyStore(String filename)
			throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		Path kspath = Paths.get(SSLSecurityRMITest.class.getSimpleName()).resolve(filename);
		String kstype = getKeystoreType(filename);
		KeyStore ks = KeyStore.getInstance(kstype);

		try (InputStream is = Files.newInputStream(kspath)) {
			ks.load(is, PASSWORD);
		}
		return ks;
	}

	private static String getKeystoreType(String filename) {
		if (StringUtils.endsWithIgnoreCase(filename, ".jks")) {
			return "JKS";
		}
		if (StringUtils.endsWithIgnoreCase(filename, ".pfx") || StringUtils.endsWithIgnoreCase(filename, ".p12")) {
			return "PKCS12";
		}
		throw new IllegalArgumentException("Unknown keystore type: " + filename);
	}

	private static KeyManager[] getKeyManagers(KeyStore ks)
			throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
		KeyManagerFactory kmfactory = getKeyManagerFactory(ks);
		KeyManager[] keymanagers = kmfactory.getKeyManagers();
		return keymanagers;
	}

	private static KeyManagerFactory getKeyManagerFactory(KeyStore ks)
			throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
		KeyManagerFactory kmfactory = KeyManagerFactory.getInstance("PKIX");
		kmfactory.init(ks, PASSWORD);
		return kmfactory;
	}

	/**
	 * THIS IS NOT A SECURE AUTHENTICATION METHOD.
	 */
	private static void testSameSSLContext() throws Throwable {
		System.err.println("SSLSecurityRMITest.testSameSSLContext()");
		RMIServer server = null;
		try {
			SSLContext sc = getKeystoreSSLContext("k1.jks");
			ServerSocketFactory serversocketfactory = sc.getServerSocketFactory();
			SocketFactory socketfactory = sc.getSocketFactory();
			RMIOptions options = getOptions();
			server = new RMIServer(serversocketfactory, 0, InetAddress.getLoopbackAddress()) {
				@Override
				protected RMIOptions getRMIOptionsForAcceptedConnection(Socket acceptedsocket, int protocolversion) {
					return options;
				}
			};
			SocketAddress sockaddress = server.getLocalSocketAddress();
			server.start();

			System.err.println("SSLSecurityRMITest.testSameSSLContext() normal");
			IOUtils.close(testConnection(options.connect(socketfactory, sockaddress)));

			System.err.println("SSLSecurityRMITest.testSameSSLContext() default");
			try {
				IOUtils.close(testConnection(options.connect(SSLSocketFactory.getDefault(), sockaddress)));
				fail();
			} catch (SSLException | SocketException e) {
				//both may be thrown depending on who aborts the handshake first?
			}

			System.err.println("SSLSecurityRMITest.testSameSSLContext() accept all");
			IOUtils.close(testConnection(options.connect(getAcceptAllSocketFactory(), sockaddress)));
		} finally {
			IOUtils.close(server);
		}
	}

	private static void testNoCertsHandshakeException() throws Throwable {
		System.err.println("SSLSecurityRMITest.testNoCertsHandshakeException()");
		RMIServer server = null;
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, null, null);
			ServerSocketFactory serversocketfactory = sc.getServerSocketFactory();
			SocketFactory socketfactory = sc.getSocketFactory();
			RMIOptions options = getOptions();
			server = new RMIServer(serversocketfactory, 0, InetAddress.getLoopbackAddress()) {
				@Override
				protected RMIOptions getRMIOptionsForAcceptedConnection(Socket acceptedsocket, int protocolversion) {
					return options;
				}
			};
			SocketAddress sockaddress = server.getLocalSocketAddress();
			server.start();

			System.err.println("SSLSecurityRMITest.testNoCertsHandshakeException() normal");
			try {
				IOUtils.close(testConnection(options.connect(socketfactory, sockaddress)));
				fail();
			} catch (SSLException | SocketException e) {
				//both may be thrown depending on who aborts the handshake first?
			}
		} finally {
			IOUtils.close(server);
		}
	}

	private static class SetupSSLServerSocketFactory extends SSLServerSocketFactory {
		private SSLServerSocketFactory factory;

		protected Boolean needsClientAuth;

		public SetupSSLServerSocketFactory(SSLServerSocketFactory factory) {
			this.factory = factory;
		}

		@Override
		public String[] getDefaultCipherSuites() {
			return factory.getDefaultCipherSuites();
		}

		@Override
		public String[] getSupportedCipherSuites() {
			return factory.getSupportedCipherSuites();
		}

		@Override
		public ServerSocket createServerSocket(int port) throws IOException {
			return setupServerSocket(factory.createServerSocket(port));
		}

		@Override
		public ServerSocket createServerSocket(int port, int backlog) throws IOException {
			return setupServerSocket(factory.createServerSocket(port, backlog));
		}

		@Override
		public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
			return setupServerSocket(factory.createServerSocket(port, backlog, ifAddress));
		}

		@Override
		public ServerSocket createServerSocket() throws IOException {
			return setupServerSocket(factory.createServerSocket());
		}

		private ServerSocket setupServerSocket(ServerSocket socket) {
			SSLServerSocket ssls = (SSLServerSocket) socket;
			if (needsClientAuth != null) {
				ssls.setNeedClientAuth(needsClientAuth);
			}
			return socket;
		}

	}

}
