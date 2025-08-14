package net.osmand.search.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.Street;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class SearchResult {
   public static final String DELIMITER = " ";
   private static final String HYPHEN = "-";
   private static final int NEAREST_METERS_LIMIT = 30000;
   public static final double MAX_TYPES_BASE_10 = 10.0;
   public static final double MAX_PHRASE_WEIGHT_TOTAL = 100.0;
   public SearchPhrase requiredSearchPhrase;
   public SearchResult parentSearchResult;
   String wordsSpan;
   boolean firstUnknownWordMatches;
   Collection<String> otherWordsMatch = null;
   public Object object;
   public ObjectType objectType;
   public BinaryMapIndexReader file;
   public double priority;
   public double priorityDistance;
   public LatLon location;
   public int preferredZoom = 15;
   public String localeName;
   public String alternateName;
   public Collection<String> otherNames;
   public String localeRelatedObjectName;
   public Object relatedObject;
   public double distRelatedObjectName;
   private double unknownPhraseMatchWeight = 0.0;

   public SearchResult() {
      this.requiredSearchPhrase = SearchPhrase.emptyPhrase();
   }

   public SearchResult(SearchPhrase sp) {
      this.requiredSearchPhrase = sp;
   }

   public double getUnknownPhraseMatchWeight() {
      if (this.unknownPhraseMatchWeight != 0.0) {
         return this.unknownPhraseMatchWeight;
      } else {
         this.unknownPhraseMatchWeight = this.getSumPhraseMatchWeight() / Math.pow(100.0, (double)(this.getDepth() - 1));
         return this.unknownPhraseMatchWeight;
      }
   }

   private double getSumPhraseMatchWeight() {
      double res = (double)ObjectType.getTypeWeight(this.objectType);
      if (this.requiredSearchPhrase.getUnselectedPoiType() == null && this.objectType != ObjectType.POI_TYPE) {
         SearchResult.CheckWordsMatchCount completeMatchRes = new SearchResult.CheckWordsMatchCount();
         if (!this.allWordsMatched(this.localeName, completeMatchRes) && this.otherNames != null) {
            for(String otherName : this.otherNames) {
               if (this.allWordsMatched(otherName, completeMatchRes)) {
                  break;
               }
            }
         }

         if (completeMatchRes.allWordsInPhraseAreInResult) {
            res = this.getPhraseWeightForCompleteMatch(completeMatchRes);
         }
      }

      if (this.parentSearchResult != null) {
         res += this.parentSearchResult.getSumPhraseMatchWeight() / 100.0;
      }

      return res;
   }

   private double getPhraseWeightForCompleteMatch(SearchResult.CheckWordsMatchCount completeMatchRes) {
      double res = (double)ObjectType.getTypeWeight(this.objectType) * 10.0;
      if (completeMatchRes.allWordsEqual && this.requiredSearchPhrase.getLastTokenLocation() != null && this.location != null) {
         boolean closeDistance = MapUtils.getDistance(this.requiredSearchPhrase.getLastTokenLocation(), this.location) <= 30000.0;
         if (this.objectType == ObjectType.CITY || this.objectType == ObjectType.VILLAGE || closeDistance) {
            res = (double)ObjectType.getTypeWeight(this.objectType) * 10.0 + 50.0;
         }
      }

      return res;
   }

   public int getDepth() {
      return this.parentSearchResult != null ? 1 + this.parentSearchResult.getDepth() : 1;
   }

   public int getFoundWordCount() {
      int inc = this.getSelfWordCount();
      if (this.parentSearchResult != null) {
         inc += this.parentSearchResult.getFoundWordCount();
      }

      return inc;
   }

   private boolean allWordsMatched(String name, SearchResult.CheckWordsMatchCount cnt) {
      List<String> searchPhraseNames = this.getSearchPhraseNames();
      List<String> localResultNames;
      if (!this.requiredSearchPhrase.getFullSearchPhrase().contains("-")) {
         localResultNames = SearchPhrase.splitWords(name, new ArrayList(), "\\s|,|-");
      } else {
         localResultNames = SearchPhrase.splitWords(name, new ArrayList(), "\\s|,");
      }

      if (searchPhraseNames.isEmpty()) {
         return false;
      } else {
         int idxMatchedWord = -1;

         for(String searchPhraseName : searchPhraseNames) {
            boolean wordMatched = false;

            for(int i = idxMatchedWord + 1; i < localResultNames.size(); ++i) {
               int r = this.requiredSearchPhrase.getCollator().compare(searchPhraseName, localResultNames.get(i));
               if (r == 0) {
                  wordMatched = true;
                  idxMatchedWord = i;
                  break;
               }
            }

            if (!wordMatched) {
               return false;
            }
         }

         if (searchPhraseNames.size() == localResultNames.size()) {
            cnt.allWordsEqual = true;
         }

         cnt.allWordsInPhraseAreInResult = true;
         return true;
      }
   }

   private List<String> getSearchPhraseNames() {
      List<String> searchPhraseNames = new ArrayList<>();
      String fw = this.requiredSearchPhrase.getFirstUnknownSearchWord();
      List<String> ow = this.requiredSearchPhrase.getUnknownSearchWords();
      if (fw != null && fw.length() > 0) {
         searchPhraseNames.add(fw);
      }

      if (ow != null) {
         searchPhraseNames.addAll(ow);
      }

      return searchPhraseNames;
   }

   private int getSelfWordCount() {
      int inc = 0;
      if (this.firstUnknownWordMatches) {
         inc = 1;
      }

      if (this.otherWordsMatch != null) {
         inc += this.otherWordsMatch.size();
      }

      return inc;
   }

   public double getSearchDistance(LatLon location) {
      double distance = 0.0;
      if (location != null && this.location != null) {
         distance = MapUtils.getDistance(location, this.location);
      }

      return this.priority - 1.0 / (1.0 + this.priorityDistance * distance);
   }

   public double getSearchDistance(LatLon location, double pd) {
      double distance = 0.0;
      if (location != null && this.location != null) {
         distance = MapUtils.getDistance(location, this.location);
      }

      return this.priority - 1.0 / (1.0 + pd * distance);
   }

   @Override
   public String toString() {
      StringBuilder b = new StringBuilder();
      if (!Algorithms.isEmpty(this.localeName)) {
         b.append(this.localeName);
      }

      if (!Algorithms.isEmpty(this.localeRelatedObjectName)) {
         if (b.length() > 0) {
            b.append(", ");
         }

         b.append(this.localeRelatedObjectName);
         if (this.relatedObject instanceof Street) {
            Street street = (Street)this.relatedObject;
            City city = street.getCity();
            if (city != null) {
               b.append(", ")
                  .append(city.getName(this.requiredSearchPhrase.getSettings().getLang(), this.requiredSearchPhrase.getSettings().isTransliterate()));
            }
         }
      } else if (this.object instanceof AbstractPoiType) {
         if (b.length() > 0) {
            b.append(" ");
         }

         AbstractPoiType poiType = (AbstractPoiType)this.object;
         if (poiType instanceof PoiCategory) {
            b.append("(Category)");
         } else if (poiType instanceof PoiFilter) {
            b.append("(Filter)");
         } else if (poiType instanceof PoiType) {
            PoiType p = (PoiType)poiType;
            AbstractPoiType parentType = p.getParentType();
            if (parentType != null) {
               String translation = parentType.getTranslation();
               b.append("(").append(translation);
               if (parentType instanceof PoiCategory) {
                  b.append(" / Category)");
               } else if (parentType instanceof PoiFilter) {
                  b.append(" / Filter)");
               } else if (parentType instanceof PoiType) {
                  PoiType pp = (PoiType)poiType;
                  PoiFilter filter = pp.getFilter();
                  PoiCategory category = pp.getCategory();
                  if (filter != null && !filter.getTranslation().equals(translation)) {
                     b.append(" / ").append(filter.getTranslation()).append(")");
                  } else if (category != null && !category.getTranslation().equals(translation)) {
                     b.append(" / ").append(category.getTranslation()).append(")");
                  } else {
                     b.append(")");
                  }
               }
            } else if (p.getFilter() != null) {
               b.append("(").append(p.getFilter().getTranslation()).append(")");
            } else if (p.getCategory() != null) {
               b.append("(").append(p.getCategory().getTranslation()).append(")");
            }
         }
      }

      return b.toString();
   }

   static class CheckWordsMatchCount {
      boolean allWordsEqual;
      boolean allWordsInPhraseAreInResult;
   }
}
