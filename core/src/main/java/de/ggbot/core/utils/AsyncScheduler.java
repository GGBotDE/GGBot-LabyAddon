package de.ggbot.core.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AsyncScheduler {
  private static final ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(2);

  /**
   * Runs the given task asynchronously after the specified delay in milliseconds.
   * @param task the task to run
   * @param delayMs the delay in milliseconds before running the task
   */
  public static void runLater(Runnable task, long delayMs) {
    scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
  }
}
