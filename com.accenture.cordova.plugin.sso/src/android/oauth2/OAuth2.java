package com.accenture.cordova.plugin.sso;

import android.content.Intent;
import android.os.Bundle;

import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import org.json.JSONException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Copyright 2015 Accenture
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class OAuth2 {

    public static final int REQUEST_CODE_OAUTH_REQUEST = 1000;
    private static String CHARSET = "UTF-8";

    public static boolean validateAccessToken(String tokenValidationUri, String accessToken) throws IOException, JSONException {
        tokenValidationUri += "?token=" + accessToken;

        URL url = new URL(tokenValidationUri);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);

        // Starts the query
        conn.connect();
        int response = conn.getResponseCode();

        return response == 200;
    }

    public static String refreshAccessToken(ArrayList<String> scopes, String tokenRefreshUrl, String clientId, String clientSecret, String refreshToken) throws IOException, JSONException {
        RefreshTokenRequest request =
                new RefreshTokenRequest(new NetHttpTransport(), new JacksonFactory(),
                        new GenericUrl(tokenRefreshUrl), refreshToken)
                        .setScopes(scopes)
                        .setClientAuthentication(new BasicAuthentication(URLEncoder.encode(clientId, CHARSET), URLEncoder.encode(clientSecret, CHARSET)));

        TokenResponse response = request.execute();
        if (response == null) {
            return null;
        } else {
            return response.getAccessToken();
        }
    }

    public static Bundle getRequestAccessBundle(String responseType, String grantType, ArrayList<String> scopes, String clientId, String clientSecret, String authorizationCodeUri, String tokenRequestUri, String redirectUri, boolean production) {
        final Bundle bundle = new Bundle();
        bundle.putString(OAuth2Keys.RESPONSE_TYPE, responseType);
        bundle.putString(OAuth2Keys.GRANT_TYPE, grantType);
        bundle.putStringArrayList(OAuth2Keys.SCOPE, scopes);
        bundle.putString(OAuth2Keys.CLIENT_ID, clientId);
        bundle.putString(OAuth2Keys.CLIENT_SECRET, clientSecret);
        bundle.putString(OAuth2Keys.AUTHORIZATION_CODE_URL, authorizationCodeUri);
        bundle.putString(OAuth2Keys.TOKEN_REQUEST_URL, tokenRequestUri);
        bundle.putString(OAuth2Keys.REDIRECT_URL, redirectUri);
        bundle.putBoolean(OAuth2Keys.PRODUCTION, production);

        return bundle;
    }

    public static void requestAccess(SSOHelperDelegate delegate, String responseType, String grantType, ArrayList<String> scopes, String clientId, String clientSecret, String authorizationCodeUri, String tokenRequestUri, String redirectUri, boolean production) throws JSONException {
        Bundle bundle = getRequestAccessBundle(responseType, grantType, scopes, clientId, clientSecret, authorizationCodeUri, tokenRequestUri, redirectUri, production);

        if (delegate != null) {
            delegate.startActivityWithBundle(bundle, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT, OAuth2AuthorizationCodeRequestActivity.class);
        }
    }

    public static OAuth2TokenResponse processActivityResult(SSOHelperDelegate delegate, int requestCode, int resultCode, Intent intent) {
        if (OAuth2AuthorizationCodeRequestActivity.RESULT_CODE_OAUTH_RESPONSE_OK == resultCode) {
            return OAuth2TokenResponse.createTokenResponseFromBundle(intent != null ? intent.getExtras() : null);
        } else if (OAuth2AuthorizationCodeRequestActivity.RESULT_CODE_OAUTH_RESPONSE_ERROR == resultCode) {
            if (delegate != null) {
                delegate.errorGettingAccessToken(new Error(intent != null ? intent.getExtras().getString(OAuth2Keys.ERROR) : "Error Signing In"));
            }
        } else if (OAuth2AuthorizationCodeRequestActivity.RESULT_CODE_OAUTH_RESPONSE_USE_EXISTING == resultCode) {
            //Purposeful no op
            //Plugin and SSO App already call getToken so this will work without needing to do anything
        }

        return null;
    }
}
