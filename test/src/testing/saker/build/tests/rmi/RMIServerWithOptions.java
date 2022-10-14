package testing.saker.build.tests.rmi;

import java.io.IOException;
import java.net.Socket;

import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMIServer;

public class RMIServerWithOptions extends RMIServer {
	private final RMIOptions options;

	public RMIServerWithOptions(RMIOptions options) throws IOException {
		this.options = options;
	}

	@Override
	protected RMIOptions getRMIOptionsForAcceptedConnection(Socket acceptedsocket, int protocolversion)
			throws IOException, RuntimeException {
		return options;
	}
}