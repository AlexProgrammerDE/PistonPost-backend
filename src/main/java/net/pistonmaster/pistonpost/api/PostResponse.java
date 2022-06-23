package net.pistonmaster.pistonpost.api;

import net.pistonmaster.pistonpost.utils.PostType;

import java.util.List;

public record PostResponse(String postId, String title, PostType type, String content, List<ImageResponse> images, VideoResponse video, List<String> tags,
                           long timestamp, boolean unlisted, UserDataResponse authorData) {
}
