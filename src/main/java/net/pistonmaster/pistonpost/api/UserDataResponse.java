package net.pistonmaster.pistonpost.api;

import java.util.Set;

public record UserDataResponse(String id, String name, String avatar, Set<String> roles) {
}
