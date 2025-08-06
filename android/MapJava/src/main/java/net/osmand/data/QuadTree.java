package net.osmand.data;

import java.util.ArrayList;
import java.util.List;

public class QuadTree<T> {
   private float ratio;
   private int maxDepth;
   private QuadTree.Node<T> root;

   public QuadTree(QuadRect r, int depth, float ratio) {
      this.ratio = ratio;
      this.root = new QuadTree.Node<>(r);
      this.maxDepth = depth;
   }

   public void insert(T data, QuadRect box) {
      int depth = 0;
      this.doInsertData(data, box, this.root, depth);
   }

   public void clear() {
      this.clear(this.root);
   }

   private void clear(QuadTree.Node<T> rt) {
      if (rt != null) {
         if (rt.data != null) {
            rt.data.clear();
         }

         if (rt.children != null) {
            for(QuadTree.Node<T> c : rt.children) {
               this.clear(c);
            }
         }
      }
   }

   public void insert(T data, float x, float y) {
      this.insert(data, new QuadRect((double)x, (double)y, (double)x, (double)y));
   }

   public List<T> queryInBox(QuadRect box, List<T> result) {
      result.clear();
      this.queryNode(box, result, this.root);
      return result;
   }

   private void queryNode(QuadRect box, List<T> result, QuadTree.Node<T> node) {
      if (node != null && QuadRect.intersects(box, node.bounds)) {
         if (node.data != null) {
            result.addAll(node.data);
         }

         for(int k = 0; k < 4; ++k) {
            this.queryNode(box, result, node.children[k]);
         }
      }
   }

   private void doInsertData(T data, QuadRect box, QuadTree.Node<T> n, int depth) {
      if (++depth >= this.maxDepth) {
         if (n.data == null) {
            n.data = new ArrayList<>();
         }

         n.data.add(data);
      } else {
         QuadRect[] ext = new QuadRect[4];
         this.splitBox(n.bounds, ext);

         for(int i = 0; i < 4; ++i) {
            if (ext[i].contains(box)) {
               if (n.children[i] == null) {
                  n.children[i] = new QuadTree.Node<>(ext[i]);
               }

               this.doInsertData(data, box, n.children[i], depth);
               return;
            }
         }

         if (n.data == null) {
            n.data = new ArrayList<>();
         }

         n.data.add(data);
      }
   }

   void splitBox(QuadRect node_extent, QuadRect[] n) {
      double lx = node_extent.left;
      double ly = node_extent.top;
      double hx = node_extent.right;
      double hy = node_extent.bottom;
      n[0] = new QuadRect(lx, ly, lx + (hx - lx) * (double)this.ratio, ly + (hy - ly) * (double)this.ratio);
      n[1] = new QuadRect(lx + (hx - lx) * (double)(1.0F - this.ratio), ly, hx, ly + (hy - ly) * (double)this.ratio);
      n[2] = new QuadRect(lx, ly + (hy - ly) * (double)(1.0F - this.ratio), lx + (hx - lx) * (double)this.ratio, hy);
      n[3] = new QuadRect(lx + (hx - lx) * (double)(1.0F - this.ratio), ly + (hy - ly) * (double)(1.0F - this.ratio), hx, hy);
   }

   private static class Node<T> {
      List<T> data = null;
      QuadTree.Node<T>[] children = null;
      QuadRect bounds;

      private Node(QuadRect b) {
         this.bounds = new QuadRect(b.left, b.top, b.right, b.bottom);
         this.children = new QuadTree.Node[4];
      }
   }
}
