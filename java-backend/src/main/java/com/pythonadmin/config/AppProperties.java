package com.pythonadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "python-admin")
public record AppProperties(Jwt jwt, Mysql mysql, Weixin weixin) {
    public record Jwt(String secret, long expireMinutes) {}
    public record Mysql(String host, int port, String database, String user, String password) {}
    public record Weixin(String appId, String appSecret) {}
}
