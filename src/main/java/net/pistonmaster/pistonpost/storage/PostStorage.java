package net.pistonmaster.pistonpost.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostStorage {
    private ObjectId id;
    private String postId;
    private String title;
    private String content;
    private ObjectId author;
    private List<String> tags;
    private long timestamp;
    private boolean unlisted;
}
