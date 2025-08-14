package net.osmand;

public interface ResultMatcher<T> {
   boolean publish(T var1);

   boolean isCancelled();
}
