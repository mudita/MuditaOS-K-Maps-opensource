package net.osmand;

import java.util.Comparator;

public interface Collator extends Comparator<Object>, Cloneable {
   boolean equals(String var1, String var2);

   int compare(String var1, String var2);
}
