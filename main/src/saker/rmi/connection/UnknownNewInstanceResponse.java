package saker.rmi.connection;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import saker.rmi.exception.RMICallFailedException;

class UnknownNewInstanceResponse extends NewInstanceResponse {
	private final Set<Class<?>> interfaces;

	public UnknownNewInstanceResponse(boolean invokerThreadInterrupted, int deliveredInterruptRequestCount,
			int remoteId, Set<Class<?>> interfaces) {
		super(invokerThreadInterrupted, deliveredInterruptRequestCount, remoteId);
		this.interfaces = interfaces;
	}

	public Set<Class<?>> getInterfaces() throws InvocationTargetException, RMICallFailedException {
		return interfaces;
	}
}