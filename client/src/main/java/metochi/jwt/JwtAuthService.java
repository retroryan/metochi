/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package metochi.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import metochi.MetochiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

import static metochi.jwt.Constant.IS_AUTHORITY;

/**
 * Created by rayt on 6/27/17.
 */
public class JwtAuthService {

    private static Logger logger = LoggerFactory.getLogger(MetochiClient.class.getName());

    private Algorithm algorithm;
    private JWTVerifier verifier;

    private static JwtAuthService authService;

    public static JwtAuthService instance() {
        if (authService == null) {
            authService = new JwtAuthService();
        }
        return authService;
    }

    private JwtAuthService() {

        try {
            this.algorithm = Algorithm.HMAC256("secret");

            this.verifier = JWT.require(algorithm)
                    .withIssuer(Constant.ISSUER)
                    .build();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    protected String generateToken(String nodeName, boolean isAuthorityNode) {
        //TODO generate jwt token with a claim for being an authority node or not
        return "";
    }

    protected DecodedJWT jwtFromToken(String token) {
        return verifier.verify(token);
    }

    public String authenticate(String nodeName, boolean isAuthorityNode) {

        //This should do a real authentication for the node, for example looking this up from a trusted data source.
        return generateToken(nodeName, isAuthorityNode);

    }
}
