package com.accenture.cordova.plugin.sso;

import android.content.ComponentName;
import android.os.Bundle;

/**
 * Created by joshualamkin on 9/8/15.
 */
public interface SSOHelperDelegate {

    void startActivityWithComponentAndBundle(final ComponentName componentName, final Bundle bundle, final int flags);
    void startActivityWithBundle(Bundle bundle, int flags, Class<?> destinationClass);
    void accessTokenUpdated(String accessToken);
    void errorGettingAccessToken(Error error);
}
