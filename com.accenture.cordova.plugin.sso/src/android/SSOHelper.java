package com.accenture.cordova.plugin.sso;

import android.accounts.Account;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Pair;

import org.apache.cordova.BuildConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.HttpRetryException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by joshualamkin on 9/3/15.
 */
public class SSOHelper {

    private final static String responseType = "code";
    private final static String grantType = "authorization_code";

    private final static String authorizationCodeRequestUriStaging = "https://federation-sts-stage.accenture.com/oauth/ls/connect/authorize";
    private final static String tokenRequestUriStaging = "https://federation-sts-stage.accenture.com/oauth/ls/connect/token";
    private final static String tokenValidationUriStaging = "https://federation-sts-stage.accenture.com/oauth/ls/connect/accesstokenvalidation";
    private final static String ssoPackageNameStaging = "com.accenture.accenturesso.staging";
    private final static String ssoActivityNameStaging = "com.accenture.accenturesso.LoginActivity";

    private final static String authorizationCodeRequestUriProduction = "https://federation-sts.accenture.com/oauth/ls/connect/authorize";
    private final static String tokenRequestUriProduction = "https://federation-sts.accenture.com/oauth/ls/connect/token";
    private final static String tokenValidationUriProduction = "https://federation-sts.accenture.com/oauth/ls/connect/accesstokenvalidation";
    private final static String ssoPackageNameProduction = "com.accenture.accenturesso.production";
    private final static String ssoActivityNameProduction = "com.accenture.accenturesso.LoginActivity";

    private final static String tokenLookupKey = "com.accenture.sso.tokentype";
    private final static String hmacLookupKey = "com.accenture.sso.hmac";
    private final static String refreshTokenLookupKey = "refreshToken";
    private final static String accessTokenLookupKey = "accessToken";
    private final static String symmetricTokenLookupKey = "symmetricToken";
    private final static String accountName = "com.accenture.sso";

    public static void getToken(SSOHelperDelegate delegate, Context context, String clientId, String clientSecret, ArrayList<String> clientScopes, String clientRedirectUri, boolean production) {

        if (!NetworkReachable.isNetworkOnline(context)) {
            if (delegate != null) {
                delegate.errorGettingAccessToken(new Error("No Network Connection"));
            }
        } else {
            try {
                String decryptedAccessToken = readAndDecryptAccessToken(context, clientId, clientSecret, production);
                if (decryptedAccessToken != null) {
                    boolean validToken = validateAccessToken(decryptedAccessToken, production);
                    if (validToken) {
                        if (delegate != null) {
                            delegate.accessTokenUpdated(decryptedAccessToken);
                        }
                    } else {
                        validateRefreshToken(delegate, context, clientId, clientSecret, clientScopes, clientRedirectUri, production);
                    }
                } else {
                    validateRefreshToken(delegate, context, clientId, clientSecret, clientScopes, clientRedirectUri, production);
                }
            } catch (NetworkException e) {
                if (delegate != null) {
                    delegate.errorGettingAccessToken(new Error(e.getLocalizedMessage()));
                }
            }
        }
    }

    public static void validateRefreshToken(SSOHelperDelegate delegate, Context context, String clientId, String clientSecret, ArrayList<String> clientScopes, String clientRedirectUri, boolean production) throws NetworkException{
        Pair<String, String> newTokens = readRefreshTokenAndRefreshAccessToken(context, clientId, clientSecret, clientScopes, production);
        if (newTokens != null) {
            if (saveToken(context, newTokens.first, newTokens.second, clientId, clientSecret, production)) {
                if (delegate != null) {
                    delegate.accessTokenUpdated(newTokens.first);
                }
            } else {
                if (delegate != null) {
                    delegate.errorGettingAccessToken(new Error("Failed to Save token"));
                }
            }
        } else {
            deleteAll(context, clientId, clientSecret, production);
            signIn(delegate, context, clientId, clientSecret, clientScopes, clientRedirectUri, production);
        }
    }

    private static void createAccount(Context context) {
        AccountManger accountManger = new AccountManger(context);
        Account[] accounts = accountManger.getAccountsByType(accountName);
        if (accounts.length == 0) {
            accountManger.addAccountExplicitly("sso", "-", accountName);
        }
    }

    public static void logout(Context context, String clientId, String clientSecret, boolean production) {
        deleteAll(context, clientId, clientSecret, production);
    }

    private static void signIn(SSOHelperDelegate delegate, Context context, String clientId, String clientSecret, ArrayList<String> clientScopes, String clientRedirectUri, boolean production) {

        if (!NetworkReachable.isNetworkOnline(context)) {
            if (delegate != null) {
                delegate.errorGettingAccessToken(new Error("No Network Connection"));
            }
        } else {
            String codeRequestUri = production ? authorizationCodeRequestUriProduction : authorizationCodeRequestUriStaging;
            String tokenRequestUri = production ? tokenRequestUriProduction : tokenRequestUriStaging;
            if (isSSOAppInstalled(context, production)) {
                if (delegate != null) {
                    Bundle bundle = OAuth2.getRequestAccessBundle(responseType, grantType, clientScopes, clientId, clientSecret, codeRequestUri, tokenRequestUri, clientRedirectUri, production);

                    ComponentName componentName;
                    if (production) {
                        componentName = new ComponentName(ssoPackageNameProduction, ssoActivityNameProduction);
                    } else {
                        componentName = new ComponentName(ssoPackageNameStaging, ssoActivityNameStaging);
                    }

                    delegate.startActivityWithComponentAndBundle(componentName, bundle, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                }
            } else {
                try {
                    OAuth2.requestAccess(delegate, responseType, grantType, clientScopes, clientId, clientSecret, codeRequestUri, tokenRequestUri, clientRedirectUri, production);
                } catch (JSONException e) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void processActivityResult(SSOHelperDelegate delegate, Context context, String clientId, String clientSecret, int requestCode, int resultCode, Intent intent, boolean production) {

        OAuth2TokenResponse tokenResponse = OAuth2.processActivityResult(delegate, requestCode, resultCode, intent);

        if (tokenResponse != null) {
            if (saveToken(context, tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), clientId, clientSecret, production)) {
                if (delegate != null) {
                    delegate.accessTokenUpdated(tokenResponse.getAccessToken());
                }
            } else {
                if (delegate != null) {
                    delegate.errorGettingAccessToken(new Error("Failed to save token"));
                }
            }
        }
    }

    private static void deleteAll(Context context, String clientId, String clientSecret, boolean production) {
        deleteCipherAlias(context, getSymmetricTokenKey(context, clientId, clientSecret, production));
        deleteRefreshKey(context, clientId, clientSecret, production);
        deleteAccessKey(context, clientId, clientSecret, production);
        deleteSymmetricKey(context, clientId, clientSecret, production);
    }

    private static void deleteCipherAlias(Context context, String keyAdd) {
        TokenCipherManager.deleteSymmetricKey(context, keyAdd);
    }

    private static void deleteSymmetricKey(Context context, String clientId, String clientSecret, boolean production) {
        String keyAdd = getSymmetricTokenKey(context, clientId, clientSecret, production);
        deleteToken(context, keyAdd, production);
    }

    private static void deleteRefreshKey(Context context, String clientId, String clientSecret, boolean production) {
        String keyAdd = getRefreshTokenKey(context, clientId, clientSecret, production);
        deleteHMAC(context, keyAdd, production);
        deleteToken(context, keyAdd, production);
    }

    private static void deleteAccessKey(Context context, String clientId, String clientSecret, boolean production) {
        String keyAdd = getAccessTokenKey(clientId, clientSecret);
        deleteHMAC(context, keyAdd, production);
        deleteToken(context, keyAdd, production);
    }

    //Returns pair as accessToken,refreshToken
    private static Pair<String, String> readRefreshTokenAndRefreshAccessToken(Context context, String clientId, String clientSecret, ArrayList<String> clientScopes, boolean production) throws NetworkException{
        String decryptedRefreshToken = readAndDecryptRefreshToken(context, clientId, clientSecret, production);
        if (decryptedRefreshToken != null) {
            return refreshAccessToken(clientScopes, clientId, clientSecret, decryptedRefreshToken, production);
        }

        return null;
    }

    private static String getAccessTokenKey(String clientId, String clientSecret) {
        return accessTokenLookupKey + clientId + clientSecret;
    }

    private static String readAndDecryptAccessToken(Context context, String clientId, String clientSecret, boolean production) {
        String keyAdd = getAccessTokenKey(clientId, clientSecret);
        String encryptedToken = readToken(context, keyAdd, production);
        String hmac = readHMAC(context, keyAdd, production);
        if (encryptedToken != null && hmac != null) {
            return decryptToken(context, encryptedToken, hmac, clientId, clientSecret, production);
        }
        return null;
    }

    public static Boolean doCredentialsExist(Context context, String clientId, String clientSecret, boolean production) {
        return readAndDecryptRefreshToken(context, clientId, clientSecret, production) != null;
    }

    private static String getRefreshTokenKey(Context context, String clientId, String clientSecret, boolean production) {
        String keyAdd = refreshTokenLookupKey;
        if (!isSSOAppInstalled(context, production)) {
            keyAdd += clientId + clientSecret;
        }

        return keyAdd;
    }

    private static String readAndDecryptRefreshToken(Context context, String clientId, String clientSecret, boolean production) {
        String keyAdd = getRefreshTokenKey(context, clientId, clientSecret, production);
        String encryptedToken = readToken(context, keyAdd, production);
        String hmac = readHMAC(context, keyAdd, production);
        if (encryptedToken != null && hmac != null) {
            return decryptToken(context, encryptedToken, hmac, clientId, clientSecret, production);
        }
        return null;
    }

    private static boolean validateAccessToken(String accessToken, boolean production) throws NetworkException {
        try {
            String tokenValidationUri = production ? tokenValidationUriProduction : tokenValidationUriStaging;
            return OAuth2.validateAccessToken(tokenValidationUri, accessToken);
        } catch (JSONException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            return false;
        } catch (IOException e) {

            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            if (e instanceof HttpRetryException
                    || e instanceof InterruptedIOException
                    || e instanceof ConnectException
                    || e instanceof ConnectTimeoutException
                    || e instanceof NoRouteToHostException
                    || e instanceof UnknownHostException
                    || e instanceof SocketTimeoutException) {
                throw new NetworkException("Network Error: " + e.getLocalizedMessage());
            }
            return false;
        }
    }

    //Returns pair as accessToken,refreshToken
    private static Pair<String, String> refreshAccessToken(ArrayList<String> clientScopes, String clientId, String clientSecret, String refreshToken, boolean production) throws NetworkException {
        try {
            String tokenRequestUri = production ? tokenRequestUriProduction : tokenRequestUriStaging;
            return new Pair<String, String>(OAuth2.refreshAccessToken(clientScopes, tokenRequestUri, clientId, clientSecret, refreshToken), refreshToken);
        } catch (JSONException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            return null;
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            if (e instanceof HttpRetryException
                    || e instanceof InterruptedIOException
                    || e instanceof ConnectException
                    || e instanceof ConnectTimeoutException
                    || e instanceof NoRouteToHostException
                    || e instanceof UnknownHostException
                    || e instanceof SocketTimeoutException) {
                throw new NetworkException("Network Error: " + e.getLocalizedMessage());
            }
            return null;
        }
    }

    private static String decryptToken(Context context, String encryptedToken, String hmac, String clientId, String clientSecret, boolean production) {
        String symmetricKey = getSymmetricalKey(context, clientId, clientSecret, production);
        if (symmetricKey != null) {
            return Crypto.decrypt(symmetricKey, hmac, encryptedToken);
        } else {
            return null;
        }
    }

    private static String readToken(Context context, String keyAdd, boolean production) {
        return peekToken(context, tokenLookupKey + production + keyAdd);
    }

    private static String readHMAC(Context context, String keyAdd, boolean production) {
        return peekToken(context, hmacLookupKey + production + keyAdd);
    }

    private static void deleteToken(Context context, String keyAdd, boolean production) {
        invalidateToken(context, readToken(context, keyAdd, production));
    }

    private static void deleteHMAC(Context context, String keyAdd, boolean production) {
        invalidateToken(context, readHMAC(context, keyAdd, production));
    }

    private static void invalidateToken(Context context, String authToken) {
        AccountManger accountManger = new AccountManger(context);
        accountManger.invalidateAuthToken(accountName, authToken);
    }

    private static String peekToken(Context context, String authTokenType) {
        AccountManger accountManger = new AccountManger(context);
        Account[] accounts = accountManger.getAccountsByType(accountName);
        if (accounts.length > 0) {
            Account account = accounts[0];
            return accountManger.peekAuthToken(account, authTokenType);
        }

        return null;
    }

    public static boolean isSSOAppInstalled(Context context, boolean production) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo(production ? ssoPackageNameProduction : ssoPackageNameStaging, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    private static boolean saveToken(Context context, String accessToken, String refreshToken, String clientId, String clientSecret, boolean production) {
        createAccount(context);

        String refreshKeyAdd = getRefreshTokenKey(context, clientId, clientSecret, production);
        String accessKeyAdd = getAccessTokenKey(clientId, clientSecret);

        if (refreshToken == null) {
            return encryptAndSaveToken(context, clientId, clientSecret, accessToken, accessKeyAdd, production);
        } else {
            return encryptAndSaveToken(context, clientId, clientSecret, refreshToken, refreshKeyAdd, production) &&
                    encryptAndSaveToken(context, clientId, clientSecret, accessToken, accessKeyAdd, production);
        }
    }

    private static String getSymmetricTokenKey(Context context, String clientId, String clientSecret, boolean production) {
        String keyAdd = symmetricTokenLookupKey + production;
        if (!isSSOAppInstalled(context, production)) {
            keyAdd += clientId + clientSecret;
        }

        return keyAdd;
    }

    private static void generateAndSaveSymmetricalKey(Context context, String clientId, String clientSecret, boolean production) {
        String keyAdd = getSymmetricTokenKey(context, clientId, clientSecret, production);
        String encryptedToken = TokenCipherManager.generateEncryptedSymmetricKey(context, keyAdd);
        writeToken(context, encryptedToken, keyAdd, production);
    }

    private static String getSymmetricalKey(Context context, String clientId, String clientSecret, boolean production) {
        String keyAdd = getSymmetricTokenKey(context, clientId, clientSecret, production);
        String encryptedToken = readToken(context, keyAdd, production);
        if (encryptedToken != null) {
            return TokenCipherManager.decryptSymmetricKey(context, keyAdd, encryptedToken);
        }

        return null;
    }

    private static boolean encryptAndSaveToken(Context context, String clientId, String clientSecret, String token, String keyAdd, boolean production) {
        String symmetricalKey = getSymmetricalKey(context, clientId, clientSecret, production);
        if (symmetricalKey == null) {
            generateAndSaveSymmetricalKey(context, clientId, clientSecret, production);
            symmetricalKey = getSymmetricalKey(context, clientId, clientSecret, production);
        }

        if (symmetricalKey != null) {
            Pair<String, String> encryptionPair = Crypto.encrypt(symmetricalKey, token);
            if (encryptionPair != null) {
                return writeHMAC(context, encryptionPair.second, keyAdd, production) && writeToken(context, encryptionPair.first, keyAdd, production);
            }
        }

        return false;
    }

    private static boolean writeHMAC(Context context, String hmac, String keyAdd, boolean production) {
        String authTokenType = hmacLookupKey + production + keyAdd;
        return setToken(context, authTokenType, hmac);
    }

    private static boolean writeToken(Context context, String token, String keyAdd, boolean production) {
        String authTokenType = tokenLookupKey + production + keyAdd;
        return setToken(context, authTokenType, token);
    }

    private static boolean setToken(Context context, String authTokenType, String token) {
        AccountManger accountManger = new AccountManger(context);
        Account[] accounts = accountManger.getAccountsByType(accountName);
        if (accounts.length > 0) {
            Account account = accounts[0];
            return accountManger.setAuthToken(account, authTokenType, token);
        }

        return false;

    }
}
