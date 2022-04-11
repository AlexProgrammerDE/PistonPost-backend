package net.pistonmaster.pistonpost.api;

import lombok.Data;

@Data
/*
 * "email":"mail@pistonmaster.net","sub":"625296b97085127de16f4ded","iat":1649690674,"exp":1652282674,"jti":"ca78f39e-943a-48a0-82af-4139a1033600"}
 */
public class JWTToken {
    private String email;
    private String sub;
    private long iat;
    private long exp;
    private String jti;
}
