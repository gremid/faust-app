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

package de.faustedition.document;

import de.faustedition.graph.NodeWrapperCollection;
import org.neo4j.graphdb.Node;

public class ArchiveCollection extends NodeWrapperCollection<Archive> {
	public ArchiveCollection(Node node) {
		super(node, Archive.class);
	}

	public Archive findById(String id) {
		for (Archive a : this) {
			if (id.equals(a.getId())) {
				return a;
			}
		}
		return null;
	}
}
