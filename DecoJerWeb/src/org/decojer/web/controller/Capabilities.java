/*
 * $Id$
 *
 * This file is part of the DecoJer project.
 * Copyright (C) 2010-2011  André Pankraz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every Java Source Code
 * that is created using DecoJer.
 */
package org.decojer.web.controller;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.google.appengine.api.capabilities.CapabilitiesService;
import com.google.appengine.api.capabilities.CapabilitiesServiceFactory;
import com.google.appengine.api.capabilities.Capability;
import com.google.appengine.api.capabilities.CapabilityState;

/**
 * Capabilitites.
 * 
 * @author André Pankraz
 */
public class Capabilities {

	public List<CapabilityState> getCapabilities() throws IllegalArgumentException,
			IllegalAccessException {
		final ArrayList<CapabilityState> capabilityStates = new ArrayList<CapabilityState>();
		final CapabilitiesService capabilitiesService = CapabilitiesServiceFactory
				.getCapabilitiesService();
		for (final Field field : Capability.class.getFields()) {
			if (field.getType().equals(Capability.class)) {
				final Capability capability = (Capability) field.get(field);
				final CapabilityState status = capabilitiesService.getStatus(capability);
				capabilityStates.add(status);
			}
		}
		return capabilityStates;
	}

}