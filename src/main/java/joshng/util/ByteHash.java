package joshng.util;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import joshng.util.blocks.F;
import joshng.util.concurrent.LazyReference;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * User: josh
 * Date: Jun 18, 2011
 * Time: 11:23:57 AM
 */
public enum ByteHash {
  MD5("MD5", Hashing.md5()),
  SHA1("SHA1", Hashing.sha1()),
  SHA256("SHA-256", Hashing.sha256());

  private static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

  private String algorithm;
  private final HashFunction hashFunction;

  ByteHash(String algorithm, HashFunction hashFunction) {
    this.algorithm = algorithm;
    this.hashFunction = hashFunction;
  }

  public String digestHex(String of) {
    return StringUtils.toHexString(digestBytes(of));
  }

  public String digestHex(String of, Charset charset) {
    return StringUtils.toHexString(digestBytes(of, charset));
  }

  public String digestHex(byte[] bytes) {
    return StringUtils.toHexString(digestBytes(bytes));
  }

  public String digestHex(byte[] input, int offset, int len) {
    return StringUtils.toHexString(digestBytes(input, offset, len));
  }

  public String digestHex(File file) throws IOException {
    return StringUtils.toHexString(Files.hash(file, hashFunction).asBytes());
  }

  public byte[] digestBytes(String of) {
    return digestBytes(of, DEFAULT_CHARSET);
  }

  public byte[] digestBytes(String of, Charset charset) {
    return digestBytes(of.getBytes(charset));
  }

  public byte[] digestBytes(byte[] input) {
    return getJDKDigest().digest(input);
  }

  public byte[] digestBytes(byte[] input, int offset, int len) {
    return getDigest().append(input, offset, len).digest();
  }

  public MessageDigest getJDKDigest() {
    try {
      return MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      throw Throwables.propagate(e);
    }
  }

  public Digest getDigest() {
    return getDigest(DEFAULT_CHARSET);
  }

  public Digest getDigest(Charset defaultCharset) {
    return new Digest(getJDKDigest(), defaultCharset);
  }

  public Digest start(String string) {
    return getDigest().append(string);
  }

  public Digest start(String string, Charset charset) {
    return getDigest().append(string, charset);
  }

  public Digest start(byte input) {
    return getDigest().append(input);
  }

  public Digest start(byte[] input, int offset, int len) {
    return getDigest().append(input, offset, len);
  }

  public Digest start(byte[] input) {
    return getDigest().append(input);
  }

  public Digest start(ByteBuffer input) {
    return getDigest().append(input);
  }

  public Digest join(String separator, Object first, Object second, Object... rest) {
    return getDigest().join(separator, first, second, rest);
  }

  public Digest join(String separator, Iterable<Object> values) {
    return getDigest().join(separator, values);
  }

  public HashingOutputStream newHashingOutputStream(OutputStream out) {
    return new HashingOutputStream(hashFunction.newHasher(), out);
  }

  public F<String, String> digestHexFunction() {
    return new F<String, String>() {
      @Override
      public String apply(String input) {
        return digestHex(input);
      }
    };
  }

  /**
   * a simple wrapper around an underlying JDK MessageDigest with extra utility:
   * append() is a chainable form of MessageDigest.update()
   * append() takes strings/encodings (with a default)
   * join(...) concatenates values with a separator, based on their {@link String#valueOf(Object)} representation
   */
  public static class Digest implements Appendable {
    private final MessageDigest delegate;
    private final Charset charset;

    private Digest(MessageDigest delegate, Charset charset) {
      this.delegate = delegate;
      this.charset = charset;
    }

    public Digest join(String separator, Iterable<Object> values) {
      try {
        return Joiner.on(separator).appendTo(this, values);
      } catch (IOException impossible) {
        throw Throwables.propagate(impossible);
      }
    }

    public Digest join(String separator, Object first, Object second, Object... rest) {
      try {
        return Joiner.on(separator).appendTo(this, first, second, rest);
      } catch (IOException impossible) {
        throw Throwables.propagate(impossible);
      }
    }

    public Digest append(String string) {
      return append(string, charset);
    }

    public Digest append(String string, Charset charset) {
      return append(string.getBytes(charset));
    }

    public Digest append(byte input) {
      delegate.update(input);
      return this;
    }

    public Digest append(byte[] input, int offset, int len) {
      delegate.update(input, offset, len);
      return this;
    }

    public Digest append(byte[] input) {
      delegate.update(input);
      return this;
    }

    public Digest append(ByteBuffer input) {
      delegate.update(input);
      return this;
    }

    public byte[] digest() {
      return delegate.digest();
    }

    public int digest(byte[] buf, int offset, int len) throws DigestException {
      return delegate.digest(buf, offset, len);
    }

    public byte[] digest(byte[] input) {
      return delegate.digest(input);
    }

    public String getAlgorithm() {
      return delegate.getAlgorithm();
    }

    public int getDigestLength() {
      return delegate.getDigestLength();
    }

    public void reset() {
      delegate.reset();
    }

    public String digestHex() {
      return StringUtils.toHexString(digest());
    }

    public Digest append(CharSequence csq) throws IOException {
      if (csq instanceof String) return append((String) csq);
      delegate.update(charset.encode(CharBuffer.wrap(csq)));
      return this;
    }

    public Digest append(CharSequence csq, int start, int end) throws IOException {
      delegate.update(charset.encode(CharBuffer.wrap(csq, start, end)));
      return this;
    }

    public Digest append(char c) throws IOException {
      delegate.update((byte) c);
      return this;
    }
  }

  public static class HashingOutputStream extends FilterOutputStream {
    private final Hasher hasher;
    private final LazyReference<HashCode> hashCode = new LazyReference<HashCode>() {
      @Override
      protected HashCode supplyValue() {
        return hasher.hash();
      }
    };

    public HashingOutputStream(Hasher hasher, OutputStream out) {
      super(out);
      this.hasher = hasher;
    }

    @Override
    public void write(int b) throws IOException {
      hasher.putByte((byte) b);
      super.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
      hasher.putBytes(b);
      super.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      hasher.putBytes(b, off, len);
      super.write(b, off, len);
    }

    public HashCode hash() {
      return hashCode.get();
    }

    public byte[] digestBytes() {
      return hash().asBytes();
    }

    public String digestHex() {
      return StringUtils.toHexString(digestBytes());
    }
  }
}
