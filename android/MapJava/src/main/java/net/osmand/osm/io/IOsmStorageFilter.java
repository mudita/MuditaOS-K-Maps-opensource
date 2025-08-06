package net.osmand.osm.io;

import net.osmand.osm.edit.Entity;

public interface IOsmStorageFilter {
   boolean acceptEntityToLoad(OsmBaseStorage var1, Entity.EntityId var2, Entity var3);
}
