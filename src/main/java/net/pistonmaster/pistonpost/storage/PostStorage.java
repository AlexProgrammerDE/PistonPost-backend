package net.pistonmaster.pistonpost.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.pistonmaster.pistonpost.utils.PostType;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostStorage {
    private ObjectId id;
    private String postId;
    private String title;
    private PostType type;
    private String content;
    private List<ObjectId> imageIds;
    private List<ObjectId> montageIds;
    private ObjectId videoId;
    private ObjectId author;
    private List<String> tags;
    private List<ObjectId> comments;
    private Set<ObjectId> likes;
    private Set<ObjectId> dislikes;
    private Set<ObjectId> hearts;
    private long timestamp;
    private boolean unlisted;
}
