package net.pistonmaster.pistonpost.api;

import java.util.Set;

public record UserPageResponse(String id, String name, String avatar, Set<String> roles, String bio, String website,
                               String location, Set<PostResponse> posts) {
}
