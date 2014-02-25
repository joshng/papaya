package joshng.util;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import joshng.util.io.MoreFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * User: josh
 * Date: Jan 27, 2011
 * Time: 6:49:26 PM
 */
public class Gzip {
    public enum Compression {
        Faster(Deflater.BEST_SPEED),
        Smaller(Deflater.BEST_COMPRESSION);

        private final int level;

        Compression(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }
    private static Logger LOG = LoggerFactory.getLogger(Gzip.class);

    public static byte[] decompress(byte[] bytes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteStreams.copy(getDecompressionStream(bytes), outputStream);
        return outputStream.toByteArray();
    }

    public static byte[] compress(byte[] bytes) throws IOException {
        return compress(bytes, Compression.Faster);
    }

    public static Function<byte[], byte[]> COMPRESS = new Function<byte[], byte[]>() {
        public byte[] apply(byte[] input) {
            try {
                return compress(input, Compression.Faster);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    };

    public static byte[] compress(byte[] bytes, Compression compression) throws IOException {
        CompressionStream stream = getCompressionStream(compression);
        ByteStreams.copy(new ByteArrayInputStream(bytes), stream);
        return stream.toByteArray();
    }

    public static byte[] compress(String content, Charset charset) throws IOException {
        return compress(content.getBytes(charset));
    }

    public static String decompressString(byte[] bytes, Charset charset) throws IOException {
        return new String(decompress(bytes), charset);
    }

    public static String decompressUtf8String(byte[] bytes) throws IOException {
        return decompressString(bytes, Charsets.UTF_8);
    }

    public static String decompressUtf8String_unchecked(byte[] bytes) {
        try {
            return decompressUtf8String(bytes);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public static InputStream getDecompressionStream(byte[] rawBytes) throws IOException {
        return getDecompressionStream(new ByteArrayInputStream(rawBytes));
    }

    public static InputStream getDecompressionStream(File file) throws IOException {
        return getDecompressionStream(MoreFiles.newBufferedInputStream(file));
    }
    public static InputStream getDecompressionStream(InputStream byteStream) throws IOException {
        return new GZIPInputStream(byteStream);
    }

    public static CompressionStream getCompressionStream() throws IOException {
        return getCompressionStream(Compression.Faster);
    }

    public static CompressionStream getCompressionStream(Compression compression) throws IOException {
        return new CompressionStream(new ByteArrayOutputStream(), compression);
    }

    public static GZIPOutputStream newCompressionStream(File file) throws IOException {
        return new GZIPOutputStream(MoreFiles.newBufferedOutputStream(file));
    }

    public static Writer newCompressedWriter(File file, Charset charset) throws IOException {
        return new OutputStreamWriter(newCompressionStream(file), charset);
    }

    public static class CompressionStream extends GZIPOutputStream {
        private final ByteArrayOutputStream byteStream;

        private CompressionStream(ByteArrayOutputStream out, Compression compression) throws IOException {
            super(out);
            def.setLevel(compression.getLevel());
            byteStream = out;
        }

        public byte[] toByteArray() throws IOException {
            close();
            return byteStream.toByteArray();
        }

        public void writeTo(OutputStream out) throws IOException {
            close();
            byteStream.writeTo(out);
        }
    }

    private Gzip() {}
}
