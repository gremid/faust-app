/*
 * Copyright (c) 2014 Faust Edition development team.
 *
 * This file is part of the Faust Edition.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.faustedition;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.resource.Finder;
import org.restlet.resource.ServerResource;
import org.springframework.context.ApplicationContext;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class ApplicationContextFinder<T extends ServerResource> extends Finder {

	private final ApplicationContext applicationContext;
	private final Class<T> beanType;

	public ApplicationContextFinder(ApplicationContext applicationContext, Class<T> beanType) {
		this.applicationContext = applicationContext;
		this.beanType = beanType;
	}

	@Override
	public ServerResource find(Request request, Response response) {
		return applicationContext.getBean(beanType);
	}
}
