package net.pistonmaster.pistonpost.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class UserDataStorage {
    private ObjectId id;
    private String name;
    private String email;
    private SettingsStorage settings;
}
