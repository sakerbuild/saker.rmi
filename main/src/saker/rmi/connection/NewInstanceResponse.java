package saker.rmi.connection;

import java.lang.reflect.InvocationTargetException;

import saker.rmi.exception.RMICallFailedException;

class NewInstanceResponse extends InterruptStatusTrackingRequestResponse {
	private final int remoteId;

	public NewInstanceResponse(boolean invokerThreadInterrupted, int deliveredInterruptRequestCount, int remoteId) {
		super(invokerThreadInterrupted, deliveredInterruptRequestCount);
		this.remoteId = remoteId;
	}

	public int getRemoteId() throws InvocationTargetException, RMICallFailedException {
		return remoteId;
	}
}