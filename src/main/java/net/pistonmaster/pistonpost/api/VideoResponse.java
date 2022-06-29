package net.pistonmaster.pistonpost.api;

public record VideoResponse(String id, String extension, ImageResponse thumbnail, int width, int height) {
}
