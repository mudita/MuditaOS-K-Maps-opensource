package net.osmand;

import net.osmand.util.Algorithms;
import org.apache.commons.logging.Log;

public class OperationLog {
   private static final Log LOG = PlatformUtil.getLog(OperationLog.class);
   private final String operationName;
   private boolean debug = false;
   private long logThreshold = 100L;
   private long startTime = System.currentTimeMillis();
   private boolean startLogged = false;

   public OperationLog(String operationName) {
      this.operationName = operationName;
   }

   public OperationLog(String operationName, boolean debug) {
      this.operationName = operationName;
      this.debug = debug;
   }

   public OperationLog(String operationName, boolean debug, long logThreshold) {
      this.operationName = operationName;
      this.debug = debug;
      this.logThreshold = logThreshold;
   }

   public void startOperation() {
      this.startOperation(null);
   }

   public void startOperation(String message) {
      this.startTime = System.currentTimeMillis();
      this.logImpl(this.operationName + " BEGIN " + (!Algorithms.isEmpty(message) ? message : ""), this.debug);
      this.startLogged = this.debug;
   }

   public void finishOperation() {
      this.finishOperation(null);
   }

   public void finishOperation(String message) {
      long elapsedTime = System.currentTimeMillis() - this.startTime;
      if (this.startLogged || this.debug || elapsedTime > this.logThreshold) {
         this.logImpl(this.operationName + " END (" + elapsedTime + " ms)" + (!Algorithms.isEmpty(message) ? " " + message : ""), true);
      }
   }

   public void log(String message) {
      this.log(message, false);
   }

   public void log(String message, boolean forceLog) {
      if (this.debug || forceLog) {
         LOG.debug(this.operationName + (!Algorithms.isEmpty(message) ? " " + message : ""));
      }
   }

   private void logImpl(String message, boolean forceLog) {
      if (this.debug || forceLog) {
         LOG.debug(message);
      }
   }
}
