package net.pistonmaster.pistonpost.api;

import net.pistonmaster.pistonpost.storage.UserDataStorage;

public record SettingsResponse(UserDataStorage userData) {
}
