package saker.rmi.connection;

interface RMIClassDefiner {
	public Class<?> defineClass(String name, byte[] bytes);
}
