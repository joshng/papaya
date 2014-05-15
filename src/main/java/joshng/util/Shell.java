package joshng.util;

import com.google.common.base.Joiner;

import java.io.IOException;

/**
 * User: josh
 * Date: Nov 1, 2010
 * Time: 12:06:24 PM
 */
public class Shell {
  public static ShellOutput execute(String... command) throws IOException, InterruptedException {
    Process process = Runtime.getRuntime().exec(command);
    return new ShellOutput(process);
  }

  public static String getOutputOrThrow(String... command) throws IOException, InterruptedException {
    ShellOutput output = execute(command);
    if (!output.isExitCodeZero()) throw new ShellCommandException(command, output);
    return output.getOutput();
  }

  public static class ShellCommandException extends IOException {
    private final String[] command;
    private final ShellOutput output;

    public ShellCommandException(String message, String[] command, ShellOutput output) {
      super(message);
      this.command = command;
      this.output = output;
    }

    public ShellCommandException(String[] command, ShellOutput output) {
      this(String.format("Command returned exitCode %d: '%s'", output.getExitCode(), Joiner.on("' '").join(command)), command, output);
    }

    public String[] getCommand() {
      return command;
    }

    public ShellOutput getOutput() {
      return output;
    }
  }
}
