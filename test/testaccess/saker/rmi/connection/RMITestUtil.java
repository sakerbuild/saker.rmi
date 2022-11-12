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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.Set;

import saker.rmi.connection.RMIStream.CommandHandler;
import saker.util.ArrayUtils;
import saker.util.ObjectUtils;
import saker.util.ReflectUtils;
import saker.util.io.ByteSource;
import saker.util.io.ReadWriteBufferOutputStream;
import saker.util.io.function.IOFunction;

public class RMITestUtil {
	private static final RMIStream.CommandHandler[] ORIGINAL_COMMAND_HANDLERS = RMIStream.COMMAND_HANDLERS.clone();
	private static final RMIStream.RMIObjectReaderFunction<?>[] ORIGINAL_OBJECT_READERS = RMIStream.OBJECT_READERS
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
		return createPipedConnection(firstoptions, secondoptions, RMIConnection.PROTOCOL_VERSION_LATEST);
	}

	public static RMIConnection[] createPipedConnection(RMIOptions firstoptions, RMIOptions secondoptions,
			short protocolversion) throws Exception, IOException {
		RMIConnection[] result = new RMIConnection[2];

		result[0] = new RMIConnection(firstoptions, protocolversion);
		result[1] = new RMIConnection(secondoptions, protocolversion);

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

	/**
	 * Gets the variable on the remove side for the argument client proxy.
	 * 
	 * @param connection
	 *            The server connection.
	 * @param proxy
	 *            The client proxy object.
	 */
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

	public static RMIStream.RMIObjectReaderFunction<?>[] getObjectReaders() {
		return RMIStream.OBJECT_READERS;
	}

	private interface TestCommandHandler {
		public void callOriginalHandler(Object[] args);
	}

	private static final ThreadLocal<Method> CURRENTLY_FORWARDED_COMMAND_HANDLER_METHOD = new ThreadLocal<>();

	public static void replaceCommandHandler(short command, IOFunction<Object[], ?> handler) {
		CommandHandler originalcommand = ORIGINAL_COMMAND_HANDLERS[command];
		if (originalcommand == null) {
			throw new IllegalArgumentException("No original command handler: " + command);
		}
		//forward the calls via a proxy, as the default implementation should be called in some cases of the command handler
		//and we can't do that by creating a CommandHandler subclass
		//but rather create a proxy for the interfaces that the original command handler implements
		CommandHandler proxy = (CommandHandler) Proxy.newProxyInstance(RMITestUtil.class.getClassLoader(),
				ArrayUtils.appended(originalcommand.getClass().getInterfaces(), TestCommandHandler.class),
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if (CURRENTLY_FORWARDED_COMMAND_HANDLER_METHOD.get() != null) {
							throw new IllegalStateException("test command handler recursively called");
						}
						if (method.isDefault()) {
							//the default method should be called for all default methods, 
							//as the command handlers are functional interfaces
							return ReflectUtils.invokeDefaultMethodOn(method, proxy, args);
						}
						CURRENTLY_FORWARDED_COMMAND_HANDLER_METHOD.set(method);
						try {
							return handler.apply(args);
						} finally {
							CURRENTLY_FORWARDED_COMMAND_HANDLER_METHOD.remove();
						}
					}
				});
		RMIStream.COMMAND_HANDLERS[command] = proxy;
	}

	public static Object callOriginalCommandHandler(short command, Object[] args) throws IOException {
		Method method = CURRENTLY_FORWARDED_COMMAND_HANDLER_METHOD.get();
		try {
			Objects.requireNonNull(method, "method");
			CommandHandler originalhandler = ORIGINAL_COMMAND_HANDLERS[command];
			Objects.requireNonNull(originalhandler, "originalhandler");
			return method.invoke(originalhandler, args);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw ObjectUtils.sneakyThrow(e.getCause());
		}
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
