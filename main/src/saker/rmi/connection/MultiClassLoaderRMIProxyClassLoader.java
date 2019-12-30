package saker.rmi.connection;

import java.util.Collection;

import saker.util.classloader.MultiClassLoader;

class MultiClassLoaderRMIProxyClassLoader extends MultiClassLoader implements RMIClassDefiner {
	public MultiClassLoaderRMIProxyClassLoader(ClassLoader parent, Collection<? extends ClassLoader> classLoaders) {
		super(parent, classLoaders);
	}

	@Override
	public Class<?> defineClass(String name, byte[] bytes) {
		return defineClass(name, bytes, 0, bytes.length);
	}
}