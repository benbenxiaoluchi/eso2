package com.accenture.cordova.plugin.sso;

import android.os.Bundle;

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
public class OAuth2TokenResponse {

    final String accessToken;
    final String refreshToken;

    private static final String EMPTY = "";

    private OAuth2TokenResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public static OAuth2TokenResponse createTokenResponseFromBundle(final Bundle bundle) {
        if (bundle == null || bundle.isEmpty() || !bundle.containsKey(OAuth2Keys.ACCESS_TOKEN) || !bundle.containsKey(OAuth2Keys.REFRESH_TOKEN)) {
            return new OAuth2TokenResponse(EMPTY, EMPTY);
        } else {
            return new OAuth2TokenResponse(bundle.getString(OAuth2Keys.ACCESS_TOKEN), bundle.getString(OAuth2Keys.REFRESH_TOKEN));
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
