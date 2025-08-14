package net.osmand;

import java.util.Comparator;

class NodeComparator implements Comparator<Node> {
   public int compare(Node a, Node b) {
      return Double.compare(a.lowerBound, b.lowerBound);
   }
}
