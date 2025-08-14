package net.osmand.search.core;

import java.io.IOException;
import net.osmand.search.SearchUICore;

public interface SearchCoreAPI {
   int getSearchPriority(SearchPhrase var1);

   boolean search(SearchPhrase var1, SearchUICore.SearchResultMatcher var2) throws IOException;

   boolean isSearchMoreAvailable(SearchPhrase var1);

   boolean isSearchAvailable(SearchPhrase var1);

   int getMinimalSearchRadius(SearchPhrase var1);

   int getNextSearchRadius(SearchPhrase var1);
}
