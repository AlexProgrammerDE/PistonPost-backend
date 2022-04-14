package net.pistonmaster.pistonpost.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SettingsStorage {
    private boolean emailNotifications;
    private String theme;
    private String bio;
    private String website;
    private String location;
}
