package net.pistonmaster.pistonpost.utils;

import java.util.UUID;

public class IDGenerator {
    public static String generateID() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    public static String generateLongID() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
