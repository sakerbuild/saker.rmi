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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Set;

import saker.rmi.connection.RMIStream.CommandHandler;
import saker.rmi.connection.RMIStream.ReferencesReleasedAction;
import saker.util.ReflectUtils;
import saker.util.io.ByteSource;
import saker.util.io.DataInputUnsyncByteArrayInputStream;
import saker.util.io.ReadWriteBufferOutputStream;
import saker.util.io.function.IOConsumer;
import saker.util.io.function.IOTriFunction;

public class RMITestUtil {
	private static final RMIStream.CommandHandler[] ORIGINAL_COMMAND_HANDLERS = RMIStream.COMMAND_HANDLERS.clone();
	private static final IOTriFunction<RMIStream, RMIVariables, DataInputUnsyncByteArrayInputStream, Object>[] ORIGINAL_OBJECT_READERS = RMIStream.OBJECT_READERS
			.clone();

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

	public static int getLiveRemoteObjectCount(RMIVariables vars) {
		return vars.getLiveRemoteObjectCount();
	}

	public static int getConnectionStreamCount(RMIConnection connection) {
		return connection.getStreamCount();
	}

	public static Object getRemoteVariablesVariable(RMIConnection connection, Object proxy) {
		RemoteProxyObject proxyobj = (RemoteProxyObject) proxy;
		RMIVariables vars = proxyobj.variables.get();
		RMIVariables remotevars = connection.getVariablesByLocalId(vars.getRemoteIdentifier());
		return remotevars.getObjectWithLocalId(proxyobj.remoteId);
	}

	public static boolean isLocalObjectKnown(RMIVariables vars, Object localobject) {
		return vars.isLocalObjectKnown(localobject);
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

	public static Set<Class<?>> getPublicInterfaceSetOf(Class<?> c) {
		return RMIStream.getPublicNonAssignableInterfaces(c, MethodHandles.lookup(), null);
	}

	public static IOTriFunction<?, RMIVariables, DataInputUnsyncByteArrayInputStream, Object>[] getObjectReaders() {
		return RMIStream.OBJECT_READERS;
	}

	public static void replaceCommandHandler(short command, IOConsumer<Object[]> handler) {
		CommandHandler originalcommand = ORIGINAL_COMMAND_HANDLERS[command];
		if (originalcommand == null) {
			throw new IllegalArgumentException("No original command handler: " + command);
		}
		RMIStream.COMMAND_HANDLERS[command] = new RMIStream.CommandHandler() {
			@Override
			public void accept(RMIStream stream, DataInputUnsyncByteArrayInputStream in,
					ReferencesReleasedAction gcaction) throws IOException {
				handler.accept(new Object[] { stream, in, gcaction });
			}

			@Override
			public boolean isPreventGarbageCollection() {
				return originalcommand.isPreventGarbageCollection();
			}

			@Override
			public boolean isPendingResponse() {
				return originalcommand.isPendingResponse();
			}
		};
	}

	public static boolean isCommandPreventGarbageCollection(short command) {
		return RMIStream.COMMAND_HANDLERS[command].isPreventGarbageCollection();
	}

	public static void callOriginalCommandHandler(short command, Object[] args) throws IOException {
		ORIGINAL_COMMAND_HANDLERS[command].accept((RMIStream) args[0], (DataInputUnsyncByteArrayInputStream) args[1],
				(ReferencesReleasedAction) args[2]);
	}

	public static void restoreInternalHandlers() {
		System.arraycopy(ORIGINAL_OBJECT_READERS, 0, RMIStream.OBJECT_READERS, 0, ORIGINAL_OBJECT_READERS.length);
		System.arraycopy(ORIGINAL_COMMAND_HANDLERS, 0, RMIStream.COMMAND_HANDLERS, 0, ORIGINAL_COMMAND_HANDLERS.length);
	}

	public static void closeRMIStreamInput(RMIVariables vars) throws IOException {
		vars.getStream().blockIn.close();
	}

	public static void closeRMIOutputInput(RMIVariables vars) throws IOException {
		vars.getStream().blockOut.close();
	}

}
