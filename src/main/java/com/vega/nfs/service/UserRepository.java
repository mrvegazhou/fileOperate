package com.vega.nfs.service;

import com.vega.nfs.config.JwtConfig;
import com.vega.nfs.model.UserCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

@Component
public class UserRepository implements IUserRepository {

    @Autowired
    private JwtConfig conf;

    @Override
    public UserCredentials findByUsername(String username) {
        // 从配置文件中读取用户的username password
        List<String> users = conf.getUser();
        if (users.contains(username)) {
            List<String> pwds = conf.getPwd();
            List<String> roles = conf.getRole();
            List<String> basepaths = conf.getBasepath();
            int idx = users.indexOf(username);
            String pwd = pwds.get(idx);
            UserCredentials userObj = new UserCredentials();
            userObj.setPassword(pwd);
            userObj.setUsername(username);
            userObj.setRoles(asList(roles.get(idx)));
            // 设置父文件夹
            userObj.setBasepath(basepaths.get(idx));
            return userObj;
        }
        return null;
    }

    @Override
    public UserCredentials insert(UserCredentials user) {
        System.out.println(user.getUsername());
        System.out.println(user.getPassword());
        System.out.println(user.getRoles());
        return user;
    }

    public List<UserCredentials> findAll() {
        List<String> users = conf.getUser();
        List<UserCredentials> res = new ArrayList<UserCredentials>();
        for (String str: users) {
            UserCredentials userObj = new UserCredentials();
            userObj.setUsername(str);
            res.add(userObj);
        }
        return res;
    }
}
