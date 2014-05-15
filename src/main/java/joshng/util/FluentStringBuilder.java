package joshng.util;

import com.google.common.base.Joiner;

import javax.annotation.Nullable;
import java.util.Iterator;

/**
 * User: josh
 * Date: 3/5/13
 * Time: 3:02 PM
 */
public class FluentStringBuilder implements Appendable, CharSequence {
  private final StringBuilder stringBuilder;

  public FluentStringBuilder() {
    this(new StringBuilder());
  }

  public FluentStringBuilder(int capacity) {
    this(new StringBuilder(capacity));
  }

  public FluentStringBuilder(String str) {
    this(new StringBuilder(str));
  }

  public FluentStringBuilder(CharSequence seq) {
    this(new StringBuilder(seq));
  }

  public FluentStringBuilder(StringBuilder stringBuilder) {
    this.stringBuilder = stringBuilder;
  }

  public FluentStringBuilder appendJoined(Joiner joiner, Iterable<?> parts) {
    joiner.appendTo(stringBuilder, parts);
    return this;
  }

  public FluentStringBuilder appendJoined(Joiner joiner, Iterator<?> parts) {
    joiner.appendTo(stringBuilder, parts);
    return this;
  }

  public FluentStringBuilder appendJoined(Joiner joiner, Object[] parts) {
    joiner.appendTo(stringBuilder, parts);
    return this;
  }

  public FluentStringBuilder appendJoined(Joiner joiner, @Nullable Object first, @Nullable Object second, Object... rest) {
    joiner.appendTo(stringBuilder, first, second, rest);
    return this;
  }

  public int length() {
    return stringBuilder.length();
  }

  public char charAt(int index) {
    return stringBuilder.charAt(index);
  }

  public CharSequence subSequence(int start, int end) {
    return stringBuilder.subSequence(start, end);
  }

  public FluentStringBuilder append(Object obj) {
    stringBuilder.append(obj);
    return this;
  }

  public FluentStringBuilder append(String str) {
    stringBuilder.append(str);
    return this;
  }

  public FluentStringBuilder append(StringBuffer sb) {
    stringBuilder.append(sb);
    return this;
  }

  public FluentStringBuilder append(CharSequence s) {
    stringBuilder.append(s);
    return this;
  }

  public FluentStringBuilder append(CharSequence s, int start, int end) {
    stringBuilder.append(s, start, end);
    return this;
  }

  public FluentStringBuilder append(char[] str) {
    stringBuilder.append(str);
    return this;
  }

  public FluentStringBuilder append(char[] str, int offset, int len) {
    stringBuilder.append(str, offset, len);
    return this;
  }

  public FluentStringBuilder append(boolean b) {
    stringBuilder.append(b);
    return this;
  }

  public FluentStringBuilder append(char c) {
    stringBuilder.append(c);
    return this;
  }

  public FluentStringBuilder append(int i) {
    stringBuilder.append(i);
    return this;
  }

  public FluentStringBuilder append(long lng) {
    stringBuilder.append(lng);
    return this;
  }

  public FluentStringBuilder append(float f) {
    stringBuilder.append(f);
    return this;
  }

  public FluentStringBuilder append(double d) {
    stringBuilder.append(d);
    return this;
  }

  public FluentStringBuilder appendCodePoint(int codePoint) {
    stringBuilder.appendCodePoint(codePoint);
    return this;
  }

  public FluentStringBuilder delete(int start, int end) {
    stringBuilder.delete(start, end);
    return this;
  }

  public FluentStringBuilder deleteCharAt(int index) {
    stringBuilder.deleteCharAt(index);
    return this;
  }

  public FluentStringBuilder replace(int start, int end, String str) {
    stringBuilder.replace(start, end, str);
    return this;
  }

  public FluentStringBuilder insert(int index, char[] str, int offset, int len) {
    stringBuilder.insert(index, str, offset, len);
    return this;
  }

  public FluentStringBuilder insert(int offset, Object obj) {
    stringBuilder.insert(offset, obj);
    return this;
  }

  public FluentStringBuilder insert(int offset, String str) {
    stringBuilder.insert(offset, str);
    return this;
  }

  public FluentStringBuilder insert(int offset, char[] str) {
    stringBuilder.insert(offset, str);
    return this;
  }

  public FluentStringBuilder insert(int dstOffset, CharSequence s) {
    stringBuilder.insert(dstOffset, s);
    return this;
  }

  public FluentStringBuilder insert(int dstOffset, CharSequence s, int start, int end) {
    stringBuilder.insert(dstOffset, s, start, end);
    return this;
  }

  public FluentStringBuilder insert(int offset, boolean b) {
    stringBuilder.insert(offset, b);
    return this;
  }

  public FluentStringBuilder insert(int offset, char c) {
    stringBuilder.insert(offset, c);
    return this;
  }

  public FluentStringBuilder insert(int offset, int i) {
    stringBuilder.insert(offset, i);
    return this;
  }

  public FluentStringBuilder insert(int offset, long l) {
    stringBuilder.insert(offset, l);
    return this;
  }

  public FluentStringBuilder insert(int offset, float f) {
    stringBuilder.insert(offset, f);
    return this;
  }

  public FluentStringBuilder insert(int offset, double d) {
    stringBuilder.insert(offset, d);
    return this;
  }

  public int indexOf(String str) {
    return stringBuilder.indexOf(str);
  }

  public int indexOf(String str, int fromIndex) {
    return stringBuilder.indexOf(str, fromIndex);
  }

  public int lastIndexOf(String str) {
    return stringBuilder.lastIndexOf(str);
  }

  public int lastIndexOf(String str, int fromIndex) {
    return stringBuilder.lastIndexOf(str, fromIndex);
  }

  public FluentStringBuilder reverse() {
    stringBuilder.reverse();
    return this;
  }

  public String toString() {
    return stringBuilder.toString();
  }
}
