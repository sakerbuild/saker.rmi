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
import java.lang.reflect.Field;
import java.lang.reflect.Method;

class RMICommState {
	//preload primitives and common classes
	private static final Class<?>[] DEFAULT_CLASSES = { void.class, byte.class, short.class, int.class, long.class,
			char.class, float.class, double.class, boolean.class, Void.class, Byte.class, Short.class, Integer.class,
			Long.class, Character.class, Float.class, Double.class, Boolean.class, Object.class, String.class,
			Class.class, Constructor.class, Method.class, Field.class };

	private final RMIWeakCommCache<Class<?>> classes;
	private final RMIWeakCommCache<ClassLoader> classLoaders;

	private final RMIGeneratingCommCache<Method> methods;
	private final RMIGeneratingCommCache<Constructor<?>> constructors;
	private final RMIGeneratingCommCache<Field> fields;

	public RMICommState() {
		classes = new RMIWeakCommCache<>();
		classLoaders = new RMIWeakCommCache<>();

		constructors = new RMIGeneratingCommCache<>();
		methods = new RMIGeneratingCommCache<>();
		fields = new RMIGeneratingCommCache<>();
		for (int i = 0; i < DEFAULT_CLASSES.length; i++) {
			Class<?> c = DEFAULT_CLASSES[i];
			classes.putWrite(c, i);
			classes.putReadInternal(i, c);
		}
	}

	public RMIWeakCommCache<Class<?>> getClasses() {
		return classes;
	}

	public RMIWeakCommCache<ClassLoader> getClassLoaders() {
		return classLoaders;
	}

	public RMIGeneratingCommCache<Constructor<?>> getConstructors() {
		return constructors;
	}

	public RMIGeneratingCommCache<Method> getMethods() {
		return methods;
	}

	public RMIGeneratingCommCache<Field> getFields() {
		return fields;
	}

	@Override
	public String toString() {
		return "RMICommState[" + (classes != null ? "classes=" + classes + ", " : "")
				+ (constructors != null ? "constructors=" + constructors + ", " : "")
				+ (methods != null ? "methods=" + methods + ", " : "")
				+ (classLoaders != null ? "classLoaders=" + classLoaders + ", " : "")
				+ (fields != null ? "fields=" + fields : "") + "]";
	}

}
