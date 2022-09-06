package net.pistonmaster.pistonpost.utils;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class WebPDimensionReader {
    public static Pair<Integer, Integer> extract(InputStream is) throws IOException {
        byte[] data = is.readNBytes(30);
        if (new String(Arrays.copyOfRange(data, 0, 4)).equals("RIFF") && data[15] == 'X') {
            int width = 1 + get24bit(data, 24);
            int height = 1 + get24bit(data, 27);

            if ((long) width * height <= 4294967296L) return Pair.of(width, height);
        }
        return null;
    }

    private static int get24bit(byte[] data, int index) {
        return data[index] & 0xFF | (data[index + 1] & 0xFF) << 8 | (data[index + 2] & 0xFF) << 16;
    }
}
