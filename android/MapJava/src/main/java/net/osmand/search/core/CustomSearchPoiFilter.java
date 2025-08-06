package net.osmand.search.core;

import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;

public interface CustomSearchPoiFilter extends BinaryMapIndexReader.SearchPoiTypeFilter {
   String getFilterId();

   String getName();

   Object getIconResource();

   ResultMatcher<Amenity> wrapResultMatcher(ResultMatcher<Amenity> var1);
}
