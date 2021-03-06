package net.pistonmaster.pistonpost.utils;

import lombok.Data;

@Data
public class JWTToken {
    private String email;
    private String sub;
    private long iat;
    private long exp;
    private String jti;
}
