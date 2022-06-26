package net.pistonmaster.pistonpost.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentStorage {
    private ObjectId id;
    private String content;
    private ObjectId author;
}
