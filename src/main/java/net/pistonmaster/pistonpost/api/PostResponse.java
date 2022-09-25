package net.pistonmaster.pistonpost.api;

import net.pistonmaster.pistonpost.utils.PostType;

import java.util.List;

public record PostResponse(String postId, String title, PostType type, String content, List<ImageResponse> images,
                           List<ImageResponse> montages, VideoResponse video, List<String> tags,
                           List<CommentResponse> comments, long timestamp, boolean unlisted,
                           UserDataResponse authorData, VoteResponse likes, VoteResponse dislikes,
                           VoteResponse hearts) {
}
