package net.pistonmaster.pistonpost.api;

import java.util.List;

public record PostResponse(String postId, String title, String content, List<String> tags,
                           long timestamp, boolean unlisted, UserDataResponse authorData) {
}
