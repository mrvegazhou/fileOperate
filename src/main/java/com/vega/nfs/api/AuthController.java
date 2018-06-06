package com.vega.nfs.api;

import com.vega.nfs.service.IUserService;
import com.vega.nfs.service.UserRepository;
import com.vega.nfs.utils.JSONResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    UserRepository userRep;

    @Autowired
    private IUserService userService;

    @RequestMapping(value = "/token", method = RequestMethod.POST)
    public String createAuthenticationToken(
            @RequestParam(value = "username", required = true) String username,
            @RequestParam(value = "password", required = true) String password) throws AuthenticationException {
        final String token = userService.login(username, password);
        String res;
        if (!token.isEmpty() && token!=null) {
            res = JSONResult.reResultString("200", null, token);
        } else {
            res = JSONResult.reResultString("400", "Failed to generate token.", null);
        }
        return res;
    }

    @RequestMapping(value = "/refreshToken", method = RequestMethod.POST)
    public String refreshToken(@RequestHeader String authorization) throws AuthenticationException {
        if (!authorization.isEmpty()) {
            String token = userService.refreshToken(authorization);
            if (token==null) {
                return JSONResult.reResultString("400", "Failed to refresh token.", null);
            } else {
                return JSONResult.reResultString("200", null, token);
            }

        } else {
            return JSONResult.reResultString("400", "Failed to refresh token.", null);
        }
    }
}