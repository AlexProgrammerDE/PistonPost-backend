package net.pistonmaster.pistonpost.utils;

import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;

public class MongoConstants {
    public static final Collation CASE_INSENSITIVE = Collation.builder().locale("en").collationStrength(CollationStrength.SECONDARY).build();
}
