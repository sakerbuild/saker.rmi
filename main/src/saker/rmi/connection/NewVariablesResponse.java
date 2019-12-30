package saker.rmi.connection;

import saker.rmi.exception.RMIRuntimeException;

class NewVariablesResponse implements RequestResponse {
	private int remoteIdentifier;

	public NewVariablesResponse(int remoteIdentifier) {
		this.remoteIdentifier = remoteIdentifier;
	}

	public int getRemoteIdentifier() throws RMIRuntimeException {
		return remoteIdentifier;
	}

	@Override
	public String toString() {
		return "NewVariablesResponse [remoteIdentifier=" + remoteIdentifier + "]";
	}

}
