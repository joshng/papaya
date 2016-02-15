package com.joshng.util.io;

import com.google.common.base.Throwables;
import com.joshng.util.collect.Maybe;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * User: josh
 * Date: 9/6/13
 * Time: 3:51 PM
 */
public class MoreFiles {
  public static File ensureDirectoryExists(File dir) {
    checkArgument(dir.mkdirs() || dir.isDirectory(), "Unable to create directory: %s", dir.getPath());
    return dir;
  }

  public static BufferedOutputStream newBufferedOutputStream(File file) throws FileNotFoundException {
    return new BufferedOutputStream(new FileOutputStream(file));
  }

  public static BufferedInputStream newBufferedInputStream(File file) throws FileNotFoundException {
    return new BufferedInputStream(new FileInputStream(file));
  }

  public static File canonicalize(String path) {
    return canonicalize(new File(path));
  }

  public static File canonicalize(File file) {
    try {
      return file.getCanonicalFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Maybe<File> findExecutableOnPath(String executableName) {
    String path = System.getenv("PATH");

    for (String pathDir : path.split(File.pathSeparator)) {
      File file = new File(pathDir, executableName);
      if (file.isFile()) return Maybe.definitely(file);
    }
    return Maybe.not();
  }

  public static Path createTempFile(
          @Nullable String prefix,
          @Nullable String suffix,
          FileAttribute<?>... attributes
  ) {
    try {
      return Files.createTempFile(prefix, suffix, attributes);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
