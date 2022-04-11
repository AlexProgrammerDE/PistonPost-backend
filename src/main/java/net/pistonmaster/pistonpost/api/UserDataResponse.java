package net.pistonmaster.pistonpost.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class UserDataResponse {
    private String name;
    private String avatar;
}
