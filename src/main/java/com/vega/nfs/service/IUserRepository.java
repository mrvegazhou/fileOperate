package com.vega.nfs.service;

import com.vega.nfs.model.UserCredentials;
import org.springframework.stereotype.Component;

@Component
public interface IUserRepository {
    UserCredentials findByUsername(String username);
    UserCredentials insert(UserCredentials user);
}
