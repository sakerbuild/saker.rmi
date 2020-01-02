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

import java.util.Map;

import saker.rmi.connection.RMITestUtil;
import saker.util.classloader.ClassLoaderDataFinder;
import saker.util.classloader.CloseProtectedClassLoaderDataFinder;
import saker.util.classloader.JarClassLoaderDataFinder;
import saker.util.classloader.PathClassLoaderDataFinder;
import saker.util.classloader.SubDirectoryClassLoaderDataFinder;
import saker.util.io.ByteArrayRegion;
import saker.util.io.DataInputUnsyncByteArrayInputStream;
import saker.util.io.DataOutputUnsyncByteArrayOutputStream;
import saker.util.io.StreamUtils;
import saker.util.io.UnsyncByteArrayInputStream;
import saker.util.io.UnsyncByteArrayOutputStream;
import testing.saker.SakerTest;
import testing.saker.SakerTestCase;

@SakerTest
public class CommonClassesValidationTest extends SakerTestCase {

	@Override
	public void runTest(Map<String, String> parameters) throws Throwable {
		RMITestUtil.testProxyCreation(setOf(UnsyncByteArrayOutputStream.class.getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(DataOutputUnsyncByteArrayOutputStream.class.getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(UnsyncByteArrayInputStream.class.getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(DataInputUnsyncByteArrayInputStream.class.getInterfaces()));

		RMITestUtil.testProxyCreation(setOf(StreamUtils.nullAppendable().getClass().getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(StreamUtils.nullByteSink().getClass().getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(StreamUtils.nullByteSource().getClass().getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(StreamUtils.nullDataInput().getClass().getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(StreamUtils.nullDataOutput().getClass().getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(StreamUtils.nullInputStream().getClass().getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(StreamUtils.nullObjectInput().getClass().getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(StreamUtils.nullObjectOutput().getClass().getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(StreamUtils.nullOutputStream().getClass().getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(StreamUtils.nullPrintStream().getClass().getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(StreamUtils.nullPrintWriter().getClass().getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(StreamUtils.nullReadable().getClass().getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(StreamUtils.nullReader().getClass().getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(StreamUtils.nullWriter().getClass().getInterfaces()));

		RMITestUtil.testProxyCreation(setOf(ByteArrayRegion.class.getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(ClassLoaderDataFinder.class.getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(CloseProtectedClassLoaderDataFinder.class.getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(JarClassLoaderDataFinder.class.getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(PathClassLoaderDataFinder.class.getInterfaces()));
		RMITestUtil.testProxyCreation(setOf(SubDirectoryClassLoaderDataFinder.class.getInterfaces()));
	}

}
