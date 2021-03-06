/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2013-2016 tarent solutions GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.osiam.security.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.TokenGranter;
import org.springframework.security.oauth2.provider.endpoint.TokenEndpoint;
import org.springframework.security.oauth2.provider.error.DefaultWebResponseExceptionTranslator;
import org.springframework.security.oauth2.provider.error.WebResponseExceptionTranslator;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Map;

@Controller
/**
 * This is the endpoint for facebook connect. Instead of json Facebook uses key&value pairs:
 * "access_token=xxxx&expires=0000"
 * However the normal controller does deliver the access_token in json so we needed to create a second Controller for
 * that use case.
 *
 */
public class FbTokenEndpoint {

    private final TokenGranter tokenGranter;
    private final ClientDetailsService clientDetailsService;

    private WebResponseExceptionTranslator providerExceptionHandler = new DefaultWebResponseExceptionTranslator();
    private TokenEndpoint tokenEndpoint = new TokenEndpoint();

    @Autowired
    public FbTokenEndpoint(TokenGranter tokenGranter, ClientDetailsService clientDetailsService) {
        this.tokenGranter = tokenGranter;
        this.clientDetailsService = clientDetailsService;
    }

    @RequestMapping(value = "/fb/oauth/access_token")
    @ResponseBody
    public String accessToken(Principal principal, @RequestParam Map<String, String> parameters)
            throws HttpRequestMethodNotSupportedException {

        tokenEndpoint.setClientDetailsService(clientDetailsService);
        tokenEndpoint.setProviderExceptionHandler(providerExceptionHandler);
        tokenEndpoint.setTokenGranter(tokenGranter);

        OAuth2AccessToken accessToken = tokenEndpoint.getAccessToken(principal, parameters).getBody();
        return "access_token=" + accessToken.getValue() + "&expires=" + accessToken.getExpiresIn();
    }
}
