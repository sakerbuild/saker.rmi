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
package saker.rmi.connection;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;

import saker.rmi.connection.AutoCreatingRMITransferProperties;
import saker.rmi.connection.MethodTransferProperties;
import saker.rmi.connection.ProxyGenerator;
import saker.rmi.connection.RMIConnection;
import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMIStream;
import saker.rmi.connection.RMITransferProperties;
import saker.rmi.connection.RMIVariables;
import saker.rmi.connection.RemoteProxyObject;
import saker.util.ReflectUtils;
import saker.util.io.ByteSource;
import saker.util.io.ReadWriteBufferOutputStream;

public class RMITestUtil {
	public static RMIConnection[] createPipedConnection() throws Exception {
		ClassLoader cl = RMITestUtil.class.getClassLoader();
		return createPipedConnection(cl);
	}

	public static RMIConnection[] createPipedConnection(ClassLoader cl) throws Exception {
		RMITransferProperties properties = new RMITransferProperties();

		return createPipedConnection(cl, properties);
	}

	public static RMIConnection[] createPipedConnection(ClassLoader cl, RMITransferProperties properties)
			throws Exception {
		RMIOptions options = new RMIOptions().classLoader(cl).transferProperties(properties);

		return createPipedConnection(options);
	}

	public static RMIConnection[] createPipedConnection(RMIOptions options) throws Exception {
		return createPipedConnection(options, options);
	}

	public static RMIConnection[] createPipedConnection(RMIOptions firstoptions, RMIOptions secondoptions)
			throws Exception {
		RMIConnection[] result = new RMIConnection[2];

		result[0] = new RMIConnection(firstoptions, RMIConnection.PROTOCOL_VERSION_LATEST);
		result[1] = new RMIConnection(secondoptions, RMIConnection.PROTOCOL_VERSION_LATEST);

		RMIStream[] initstreams = createPipedStreams(result);

		result[0].finishNewConnectionSetup(initstreams[0]);
		result[1].finishNewConnectionSetup(initstreams[1]);

		int sc = Math.max(1, firstoptions.getDefaultedMaxStreamCount());
		for (int i = 1; i < sc; i++) {
			addPipedStreams(result);
		}

		return result;
	}

	private static RMIStream[] createPipedStreams(RMIConnection[] connections) throws Exception {
		RMIStream[] result = { null, null };

		ReadWriteBufferOutputStream s1 = new ReadWriteBufferOutputStream();
		ReadWriteBufferOutputStream s2 = new ReadWriteBufferOutputStream();

		InputStream s1is = ByteSource.toInputStream(s1);
		InputStream s2is = ByteSource.toInputStream(s2);

		OutputStream s1os = s2;
		OutputStream s2os = s1;

		result[0] = new RMIStream(connections[0], s1is, s1os);
		result[1] = new RMIStream(connections[1], s2is, s2os);

		return result;
	}

	private static void addPipedStreams(RMIConnection[] connections) throws Exception {
		RMIStream[] streams = createPipedStreams(connections);

		connections[0].addStream(streams[0]);
		connections[1].addStream(streams[1]);
	}

	public static RMIVariables getConnectionVariablesByLocalId(RMIConnection connection, int localid) {
		return connection.getVariablesByLocalId(localid);
	}

	public static RMIVariables getCorrespondingConnectionVariables(RMIConnection connection,
			RMIVariables localvariables) {
		return getConnectionVariablesByLocalId(connection, localvariables.getRemoteIdentifier());
	}

	public static int getLiveLocalObjectCount(RMIVariables vars) {
		return vars.getLiveLocalObjectCount();
	}

	public static Object getRemoteVariablesVariable(RMIConnection connection, Object proxy) {
		RemoteProxyObject proxyobj = (RemoteProxyObject) proxy;
		RMIVariables vars = proxyobj.variables.get();
		RMIVariables remotevars = connection.getVariablesByLocalId(vars.getRemoteIdentifier());
		return remotevars.getObjectWithLocalId(proxyobj.remoteId);
	}

	public static byte[] testProxyCreation(Set<Class<?>> interfaces) {
		return testProxyCreation("noname", interfaces);
	}

	public static byte[] testProxyCreation(String name, Set<Class<?>> interfaces) {
		return ProxyGenerator.generateProxy(name, interfaces, null, AutoCreatingRMITransferProperties.create(), false);
	}

	public static void testProxyCreation(Class<?> c) {
		testProxyCreation(ReflectUtils.getInterfaces(c));
	}

	public static void validateMethodProperties(Class<?> c) {
		if (c == null) {
			return;
		}
		for (Method m : c.getDeclaredMethods()) {
			new MethodTransferProperties(m);
		}
		for (Class<?> itf : c.getInterfaces()) {
			validateMethodProperties(itf);
		}
		validateMethodProperties(c.getSuperclass());
	}

}
