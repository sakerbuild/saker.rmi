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
package saker.rmi.connection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import saker.rmi.io.writer.RMIObjectWriteHandler;
import saker.util.ObjectUtils;

class AutoCreatingRMITransferProperties extends RMITransferPropertiesHolder {
	private static final ClassTransferProperties<?> NO_CLASS_PROPERTIES_DEFINED_MARKER_INSTANCE = new ClassTransferProperties<>(
			RMITransferPropertiesHolder.class, RMIObjectWriteHandler.defaultWriter());

	protected Map<Executable, ExecutableTransferProperties<?>> executableProperties = new ConcurrentHashMap<>();
	protected Map<Class<?>, ClassTransferProperties<?>> classProperties = new ConcurrentHashMap<>();

	AutoCreatingRMITransferProperties() {
	}

	AutoCreatingRMITransferProperties(RMITransferProperties base) {
		this.executableProperties = new ConcurrentHashMap<>(base.executableProperties);
		this.classProperties = new ConcurrentHashMap<>(base.classProperties);
	}

	static AutoCreatingRMITransferProperties create() {
		return new AutoCreatingRMITransferProperties();
	}

	static AutoCreatingRMITransferProperties create(RMITransferProperties properties) {
		return properties == null ? new AutoCreatingRMITransferProperties()
				: new AutoCreatingRMITransferProperties(properties);
	}

	@Override
	public MethodTransferProperties getExecutableProperties(Method method) {
		return (MethodTransferProperties) executableProperties.computeIfAbsent(method,
				m -> new MethodTransferProperties((Method) m));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <C> ConstructorTransferProperties<C> getExecutableProperties(Constructor<C> constructor) {
		return (ConstructorTransferProperties<C>) executableProperties.computeIfAbsent(constructor,
				m -> new ConstructorTransferProperties<>((Constructor<?>) m));
	}

	@Override
	public <C> ClassTransferProperties<C> getClassProperties(Class<C> clazz) {
		@SuppressWarnings("unchecked")
		ClassTransferProperties<C> result = (ClassTransferProperties<C>) classProperties.computeIfAbsent(clazz, c -> {
			return ObjectUtils.nullDefault(ClassTransferProperties.createForAnnotations(c),
					NO_CLASS_PROPERTIES_DEFINED_MARKER_INSTANCE);
		});
		if (result == NO_CLASS_PROPERTIES_DEFINED_MARKER_INSTANCE) {
			return null;
		}
		return result;
	}
}
