package joshng.util;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * User: josh
 * Date: 11/14/11
 * Time: 11:33 PM
 */
public class ByteArrayWriter extends OutputStreamWriter {
    private final ByteArrayOutputStream output;

    public ByteArrayWriter() {
        this(new ByteArrayOutputStream(), Charsets.UTF_8);
    }

    public ByteArrayWriter(Charset charset) throws UnsupportedEncodingException {
        this(new ByteArrayOutputStream(), charset);
    }

    public ByteArrayWriter(ByteArrayOutputStream out, Charset cs) {
        super(out, cs);
        output = out;
    }

    public ByteArrayWriter(ByteArrayOutputStream output) {
        super(output);
        this.output = output;
    }

    public byte[] toByteArray() {
        try {
            flush();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return output.toByteArray();
    }
}
