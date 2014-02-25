package joshng.util.io;

import joshng.util.collect.Maybe;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import static com.google.common.base.Preconditions.checkArgument;
import static joshng.util.collect.Maybe.definitely;

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

    public static Maybe<File> findExecutableOnPath(String executableName)   {
        String path = System.getenv("PATH");

        for (String pathDir : path.split(File.pathSeparator)) {
            File file = new File(pathDir, executableName);
            if (file.isFile()) return definitely(file);
        }
        return Maybe.not();
    }
}
