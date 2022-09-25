package net.pistonmaster.pistonpost.utils;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Code taken from <a href="https://github.com/facebook/fresco/blob/main/fbcore/src/main/java/com/facebook/common/webp/WebpSupportStatus.java">Fresco</a>
 */
public class WebPDetection {
    private static final int EXTENDED_WEBP_HEADER_LENGTH = 21;
    private static final int SIMPLE_WEBP_HEADER_LENGTH = 20;
    final static int MAX_HEADER_LENGTH = Ints.max(
            EXTENDED_WEBP_HEADER_LENGTH,
            SIMPLE_WEBP_HEADER_LENGTH);
    private static final byte[] WEBP_RIFF_BYTES = asciiBytes("RIFF");
    private static final byte[] WEBP_NAME_BYTES = asciiBytes("WEBP");

    public static boolean determineWebP(final InputStream is) throws IOException {
        final byte[] imageHeaderBytes = new byte[MAX_HEADER_LENGTH];
        final int headerSize = readHeaderFromStream(MAX_HEADER_LENGTH, is, imageHeaderBytes);

        return isWebpHeader(imageHeaderBytes, 0, headerSize);
    }

    private static int readHeaderFromStream(
            int maxHeaderLength,
            final InputStream is,
            final byte[] imageHeaderBytes)
            throws IOException {
        if (is.markSupported()) {
            try {
                is.mark(maxHeaderLength);
                return ByteStreams.read(is, imageHeaderBytes, 0, maxHeaderLength);
            } finally {
                is.reset();
            }
        } else {
            return ByteStreams.read(is, imageHeaderBytes, 0, maxHeaderLength);
        }
    }

    public static boolean isWebpHeader(
            final byte[] imageHeaderBytes,
            final int offset,
            final int headerSize) {
        return headerSize >= SIMPLE_WEBP_HEADER_LENGTH &&
                matchBytePattern(imageHeaderBytes, offset, WEBP_RIFF_BYTES) &&
                matchBytePattern(imageHeaderBytes, offset + 8, WEBP_NAME_BYTES);
    }

    private static boolean matchBytePattern(
            final byte[] byteArray,
            final int offset,
            final byte[] pattern) {
        if (pattern == null || byteArray == null) {
            return false;
        }
        if (pattern.length + offset > byteArray.length) {
            return false;
        }

        for (int i = 0; i < pattern.length; ++i) {
            if (byteArray[i + offset] != pattern[i]) {
                return false;
            }
        }

        return true;
    }

    public static byte[] asciiBytes(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }
}
