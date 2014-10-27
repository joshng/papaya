package joshng.util.blocks;

import joshng.util.collect.Nothing;

import java.util.concurrent.Callable;

/**
 * User: josh
 * Date: 10/22/14
 * Time: 9:42 PM
 */
public interface ThrowingRunnable extends Callable<Nothing> {
  void run() throws Exception;

  default Nothing call() throws Exception {
      run();
      return Nothing.NOTHING;
  }
}
