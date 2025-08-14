package net.osmand.search.core;

import net.osmand.data.LatLon;

public class SearchWord {
   private String word;
   private SearchResult result;

   public SearchWord(String word, SearchResult res) {
      this.word = word.trim();
      this.result = res;
   }

   public ObjectType getType() {
      return this.result == null ? ObjectType.UNKNOWN_NAME_FILTER : this.result.objectType;
   }

   public String getWord() {
      return this.word;
   }

   public SearchResult getResult() {
      return this.result;
   }

   public void syncWordWithResult() {
      this.word = this.result.wordsSpan != null ? this.result.wordsSpan : this.result.localeName.trim();
   }

   public LatLon getLocation() {
      System.out.println("result " + this.result);
      System.out.println("result location " + this.result.location);
      return this.result == null ? null : this.result.location;
   }

   @Override
   public String toString() {
      return this.word;
   }
}
