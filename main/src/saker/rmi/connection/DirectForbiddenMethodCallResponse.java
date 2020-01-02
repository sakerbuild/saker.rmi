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

import java.lang.reflect.InvocationTargetException;

import saker.rmi.exception.RMICallFailedException;
import saker.rmi.exception.RMICallForbiddenException;

class DirectForbiddenMethodCallResponse extends MethodCallResponse {
	public static final DirectForbiddenMethodCallResponse INSTANCE = new DirectForbiddenMethodCallResponse();

	public DirectForbiddenMethodCallResponse() {
		super(false, 0, null);
	}

	@Override
	public Object getReturnValue() throws InvocationTargetException, RMICallFailedException {
		throw new RMICallForbiddenException(RMIStream.EXCEPTION_MESSAGE_DIRECT_REQUESTS_FORBIDDEN);
	}
}
