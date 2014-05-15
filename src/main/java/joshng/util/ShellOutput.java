package joshng.util;

import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * User: josh
 * Date: Nov 1, 2010
 * Time: 12:24:28 PM
 */
public class ShellOutput {
  private final int exitCode;
  private final String output;

  public ShellOutput(Process process) throws IOException, InterruptedException {
    exitCode = process.waitFor();
    output = CharStreams.toString(new InputStreamReader(process.getInputStream()));
  }

  public boolean isExitCodeZero() {
    return exitCode == 0;
  }

  public int getExitCode() {
    return exitCode;
  }

  public String getOutput() {
    return output;
  }
}
