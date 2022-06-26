package net.pistonmaster.pistonpost.api;

public record CommentResponse(String id, String content, UserDataResponse author) {
}
