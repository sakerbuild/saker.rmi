package saker.rmi.connection;

import saker.rmi.exception.RMICallFailedException;

class GetContextVariableFailedResponse extends GetContextVariableResponse {
	private Throwable exc;

	public GetContextVariableFailedResponse(Throwable exc) {
		super(null);
		this.exc = exc;
	}

	@Override
	public Object getVariable() {
		throw new RMICallFailedException("Failed to retrieve context variable.", exc);
	}
}
