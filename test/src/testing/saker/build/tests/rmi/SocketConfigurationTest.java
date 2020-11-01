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
package testing.saker.build.tests.rmi;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedByInterruptException;
import java.util.Map;

import saker.rmi.connection.RMIOptions;
import saker.rmi.connection.RMISocketConfiguration;
import saker.util.thread.ThreadUtils;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class SocketConfigurationTest extends SakerTestCase {

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		Thread currentthread = Thread.currentThread();
		InetSocketAddress timeoutingaddress = new InetSocketAddress(InetAddress.getByName("saker.build"), 12345);

		//this connection should time out
		RMISocketConfiguration socketconfig = new RMISocketConfiguration();
		assertException(SocketTimeoutException.class, () -> new RMIOptions().connect(timeoutingaddress, socketconfig));

		socketconfig.setConnectionTimeout(5000);
		socketconfig.setConnectionInterruptible(true);
		startInterruptThread(currentthread);
		//currently not interrupted
		assertFalse(Thread.currentThread().isInterrupted());
		assertException(ClosedByInterruptException.class,
				() -> new RMIOptions().connect(timeoutingaddress, socketconfig));
		//the interrupted flag should be set
		assertTrue(Thread.interrupted());

		//already interrupted, should fail right away
		currentthread.interrupt();
		assertException(ClosedByInterruptException.class,
				() -> new RMIOptions().connect(timeoutingaddress, socketconfig));
		assertTrue(Thread.interrupted());

		//if we set the interruptible to false, it should time out, even if we interrupt the thread
		startInterruptThread(currentthread);
		socketconfig.setConnectionTimeout(2000);
		socketconfig.setConnectionInterruptible(false);
		assertException(SocketTimeoutException.class, () -> new RMIOptions().connect(timeoutingaddress, socketconfig));
		assertTrue(Thread.interrupted());

	}

	private static void startInterruptThread(Thread currentthread) {
		ThreadUtils.startDaemonThread(() -> {
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("SocketConfigurationTest.runTest() interrupt " + currentthread);
			currentthread.interrupt();
		});
	}

}
