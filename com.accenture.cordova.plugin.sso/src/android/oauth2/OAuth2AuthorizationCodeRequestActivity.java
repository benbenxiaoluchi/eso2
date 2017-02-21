package com.accenture.cordova.plugin.sso;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.UUID;


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
final public class OAuth2AuthorizationCodeRequestActivity extends Activity {

    static final int RESULT_CODE_OAUTH_RESPONSE_OK = 2000;
    static final int RESULT_CODE_OAUTH_RESPONSE_ERROR = 3000;
    static final int RESULT_CODE_OAUTH_RESPONSE_USE_EXISTING = 4000;

    private static String CHARSET = "UTF-8";
    private AuthorizationCodeRequestUrl authorizationCodeRequestUrl;
    private ProgressBar progressBar;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        clearCookies();

        final String url = buildAuthorizationCodeRequestUrl();

        FrameLayout layout = new FrameLayout(this);
        progressBar = new ProgressBar(OAuth2AuthorizationCodeRequestActivity.this, null, android.R.attr.progressBarStyleLarge);
        progressBar.setVisibility(View.VISIBLE);

        webView = new WebView(this.getApplicationContext());
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                final String redirectUri = authorizationCodeRequestUrl.getRedirectUri();
                if (redirectUri != null && url != null && url.contains(redirectUri)) {
                    if (url.contains("error=")) {
                         int p1 = url.indexOf("error=")+"error=".length();
                         setActivityResultToError(url.substring(p1,url.indexOf("&",p1)));
                    } else {
                         runOnUiThread(new Runnable() {
                             @Override
                             public void run() {
                                 progressBar.setVisibility(View.VISIBLE);
                             }
                         });
                         requestToken(url);
                    }
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.INVISIBLE);
            }
        });

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        layout.addView(webView, params);
        params = new FrameLayout.LayoutParams(100, 100, Gravity.CENTER);
        layout.addView(progressBar, params);

        setContentView(layout);

        webView.loadUrl(url);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SSOHelper.doCredentialsExist(this, getIntent().getExtras().getString(OAuth2Keys.CLIENT_ID), getIntent().getExtras().getString(OAuth2Keys.CLIENT_SECRET), getIntent().getExtras().getBoolean(OAuth2Keys.PRODUCTION))) {
            useExistingAndFinish();
        } else {
            final String url = buildAuthorizationCodeRequestUrl();
            webView.loadUrl(url);
        }
    }

    private void requestToken(final String url) {
        if (checkAuthorizationRequestData()) {
            final AuthorizationCodeResponseUrl authResponse = new AuthorizationCodeResponseUrl(url);
            if (authResponse.getError() != null) {
                setActivityResultToError("Authorization denied...");
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Bundle bundle = getIntent().getExtras();

                            AuthorizationCodeTokenRequest request =
                                    new AuthorizationCodeTokenRequest(new NetHttpTransport(), new JacksonFactory(),
                                            new GenericUrl(bundle.getString(OAuth2Keys.TOKEN_REQUEST_URL)), authResponse.getCode())
                                            .setRedirectUri(authorizationCodeRequestUrl.getRedirectUri())
                                            .setClientAuthentication(new BasicAuthentication(URLEncoder.encode(bundle.getString(OAuth2Keys.CLIENT_ID), CHARSET), URLEncoder.encode(bundle.getString(OAuth2Keys.CLIENT_SECRET), CHARSET)));

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.VISIBLE);
                                }
                            });
                            TokenResponse response = request.execute();
                            if (response == null) {
                                setActivityResultToError("There was an error. Unable to get token.");
                            } else {
                                saveResponseAndFinish(response);
                            }
                        } catch (IOException e) {
                            setActivityResultToError(e.getMessage());
                        }
                    }
                }).start();
            }
        }
    }

    private String buildAuthorizationCodeRequestUrl() {
        String authorizationCodeRequestUrl = "";

        if (checkAuthorizationRequestData()) {
            Bundle bundle = getIntent().getExtras();

            String redirectUrl = bundle.getString(OAuth2Keys.REDIRECT_URL);
            this.authorizationCodeRequestUrl = new AuthorizationCodeRequestUrl(bundle.getString(OAuth2Keys.AUTHORIZATION_CODE_URL), bundle.getString(OAuth2Keys.CLIENT_ID))
                    .setScopes(bundle.getStringArrayList(OAuth2Keys.SCOPE))
                    .setResponseTypes(Collections.singleton(bundle.getString(OAuth2Keys.RESPONSE_TYPE)))
                    .setState(UUID.randomUUID().toString())
                    .setRedirectUri(redirectUrl);

            authorizationCodeRequestUrl = this.authorizationCodeRequestUrl.build();
        }

        return authorizationCodeRequestUrl;
    }

    private void clearCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
    }


    private boolean checkAuthorizationRequestData() {
        final Intent intent = getIntent();
        final Bundle bundle = intent == null ? null : intent.getExtras();
        if (intent == null || bundle == null
                || !bundle.containsKey(OAuth2Keys.AUTHORIZATION_CODE_URL)
                || !bundle.containsKey(OAuth2Keys.TOKEN_REQUEST_URL)
                || !bundle.containsKey(OAuth2Keys.RESPONSE_TYPE)
                || !bundle.containsKey(OAuth2Keys.GRANT_TYPE)
                || !bundle.containsKey(OAuth2Keys.CLIENT_ID)
                || !bundle.containsKey(OAuth2Keys.CLIENT_SECRET)
                || !bundle.containsKey(OAuth2Keys.SCOPE)
                || !bundle.containsKey(OAuth2Keys.REDIRECT_URL)
                || !bundle.containsKey(OAuth2Keys.PRODUCTION)) {
            setActivityResultToError("Error building client authorization request. There is no data available to build the request.");
            return false;
        }
        return true;
    }

    private void setActivityResultToError(final String message) {
        clearCookies();

        final Intent intent = new Intent();
        intent.putExtra(OAuth2Keys.ERROR, message);
        setResult(RESULT_CODE_OAUTH_RESPONSE_ERROR, intent);
        finish();
    }

    private void saveResponseAndFinish(final TokenResponse response) {
        clearCookies();

        final Intent intent = new Intent();
        intent.putExtra(OAuth2Keys.ACCESS_TOKEN, response.getAccessToken());
        intent.putExtra(OAuth2Keys.REFRESH_TOKEN, response.getRefreshToken());
        setResult(RESULT_CODE_OAUTH_RESPONSE_OK, intent);
        finish();
    }

    private void useExistingAndFinish() {
        clearCookies();

        final Intent intent = new Intent();
        setResult(RESULT_CODE_OAUTH_RESPONSE_USE_EXISTING, intent);
        finish();
    }

}
