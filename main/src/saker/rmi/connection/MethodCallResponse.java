package saker.rmi.connection;

import java.lang.reflect.InvocationTargetException;

import saker.rmi.exception.RMICallFailedException;

class MethodCallResponse extends InterruptStatusTrackingRequestResponse {
	private final Object returnValue;

	public MethodCallResponse(boolean invokerThreadInterrupted, int deliveredInterruptRequestCount,
			Object returnValue) {
		super(invokerThreadInterrupted, deliveredInterruptRequestCount);
		this.returnValue = returnValue;
	}

	public Object getReturnValue() throws InvocationTargetException, RMICallFailedException {
		return returnValue;
	}
}