package com.vega.nfs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "jwt.credential")
public class JwtConfig {
    private List<String> user = new ArrayList<String>();
    public List<String> getUser() { return user; }

    private List<String> pwd = new ArrayList<String>();
    public List<String> getPwd() { return pwd; }

    private List<String> role = new ArrayList<String>();
    public List<String> getRole() { return role; };

    private List<String> basepath = new ArrayList<String>();
    public List<String> getBasepath() { return basepath; };
}
