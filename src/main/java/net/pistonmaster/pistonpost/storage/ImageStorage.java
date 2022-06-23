package net.pistonmaster.pistonpost.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageStorage {
    private ObjectId id;
    private int width;
    private int height;
}
