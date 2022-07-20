package com.google.protobuf;

import java.nio.ByteBuffer;

/**
 * A helper class to get access to the package-private methods of {@link ByteString}
 */
public class ByteStringHelper {

  public static ByteString wrap(byte[] array) {
    return ByteString.wrap(array);
  }

  public static ByteString wrap(ByteBuffer buffer) {
    return ByteString.wrap(buffer);
  }

  public static ByteString wrap(byte[] array, int offset, int length) {
    return ByteString.wrap(array, offset, length);
  }

}