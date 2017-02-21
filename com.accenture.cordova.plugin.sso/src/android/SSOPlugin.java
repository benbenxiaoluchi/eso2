/*
 * Copyright 2015 Accenture
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.accenture.cordova.plugin.sso;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public final class SSOPlugin extends CordovaPlugin implements SSOHelperDelegate {

    private static final String GET_TOKEN_ACTION = "getToken";
    private static final String LOGOUT_ACTION = "logout";
    private CallbackContext callbackContext;
    private String clientId;
    private String clientSecret;
    private String clientRedirectUri;
    private ArrayList<String> clientScopes;
    private boolean production;

    @Override
    public boolean execute(String action,final JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (GET_TOKEN_ACTION.equals(action)) {
            this.cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    getToken(args);
                }
            });
            return true;
        } else if (LOGOUT_ACTION.equals(action)) {
            this.cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    logout(args);
                }
            });
            return true;
        }
        return false;
    }

    private void getToken(JSONArray args) {
        if (args == null || args.length() == 0) {
            return;
        }

        try {
            if (args.length() >= 5) {
                JSONArray jsonArray = args.getJSONArray(2);
                ArrayList<String> clientScopes = new ArrayList<String>();
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        clientScopes.add(jsonArray.get(i).toString());
                    }
                }
                this.clientId = args.getString(0);
                this.clientSecret = args.getString(1);
                this.clientScopes = clientScopes;
                this.clientRedirectUri = args.getString(3);
                this.production = args.getBoolean(4);
                
                //todo send to background
                SSOHelper.getToken(this, this.cordova.getActivity(), clientId, clientSecret, clientScopes, clientRedirectUri, production);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
        }
    }

    private void logout(JSONArray args) {
        if (args == null || args.length() == 0) {
            return;
        }

        try {
            if (args.length() >= 5) {
                JSONArray jsonArray = args.getJSONArray(2);
                ArrayList<String> clientScopes = new ArrayList<String>();
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        clientScopes.add(jsonArray.get(i).toString());
                    }
                }
                this.clientId = args.getString(0);
                this.clientSecret = args.getString(1);
                this.clientScopes = clientScopes;
                this.clientRedirectUri = args.getString(3);
                this.production = args.getBoolean(4);

                //todo send to background
                SSOHelper.logout(this.cordova.getActivity(), clientId, clientSecret, production);
                this.callbackContext.success();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (SSOHelper.isSSOAppInstalled(this.cordova.getActivity(), production)) {
          this.cordova.getThreadPool().execute(new Runnable() {
              @Override
              public void run() {
                  SSOHelper.getToken(SSOPlugin.this, SSOPlugin.this.cordova.getActivity(), clientId, clientSecret, clientScopes, clientRedirectUri, production);
              }
          });
        } else {
          SSOHelper.processActivityResult(this, this.cordova.getActivity(), clientId, clientSecret, requestCode, resultCode, intent, production);
        }
    }

    @Override
    public void startActivityWithComponentAndBundle(final ComponentName componentName, final Bundle bundle, final int flags) {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.setComponent(componentName);
                intent.setFlags(flags);
                intent.putExtras(bundle);
                SSOPlugin.this.cordova.startActivityForResult(SSOPlugin.this, intent, OAuth2.REQUEST_CODE_OAUTH_REQUEST);
            }
        });
    }

    @Override
    public void startActivityWithBundle(final Bundle bundle, final int flags, final Class<?> destinationClass) {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Intent intent = new Intent(SSOPlugin.this.cordova.getActivity(), destinationClass);
                intent.setFlags(flags);
                intent.putExtras(bundle);
                SSOPlugin.this.cordova.startActivityForResult(SSOPlugin.this, intent, OAuth2.REQUEST_CODE_OAUTH_REQUEST);
            }
        });
    }

    @Override
    public void accessTokenUpdated(final String accessToken) {
        this.callbackContext.success(accessToken);
    }

    @Override
    public void errorGettingAccessToken(final Error error) {
        this.callbackContext.error(error.getLocalizedMessage());
    }

}
