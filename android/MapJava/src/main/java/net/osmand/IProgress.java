package net.osmand;

public interface IProgress {
   IProgress EMPTY_PROGRESS = new IProgress() {
      @Override
      public void startWork(int work) {
      }

      @Override
      public void startTask(String taskName, int work) {
      }

      @Override
      public void remaining(int remainingWork) {
      }

      @Override
      public void progress(int deltaWork) {
      }

      @Override
      public boolean isInterrupted() {
         return false;
      }

      @Override
      public boolean isIndeterminate() {
         return true;
      }

      @Override
      public void finishTask() {
      }

      @Override
      public void setGeneralProgress(String genProgress) {
      }
   };

   void startTask(String var1, int var2);

   void startWork(int var1);

   void progress(int var1);

   void remaining(int var1);

   void finishTask();

   boolean isIndeterminate();

   boolean isInterrupted();

   void setGeneralProgress(String var1);
}
