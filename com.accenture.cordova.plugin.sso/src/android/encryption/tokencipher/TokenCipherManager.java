/* Copyright 2015 Accenture

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.accenture.cordova.plugin.sso;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

import org.keyczar.exceptions.Base64DecodingException;
import org.keyczar.util.Base64Coder;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Date;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

public class TokenCipherManager {

    private final static String keyAlias = "keyAlias";
    private final static String ALGORITHM = "AES";

    public static String decryptSymmetricKey(Context context, String keyAdd, String encryptedData) {
        String alias = keyAlias+keyAdd;
        byte[] decryptedBytes = TokenCipherManager.decryptToken(context, Base64.decode(encryptedData, Base64.DEFAULT), alias);
        if (decryptedBytes != null) {
            return Base64Coder.encodeWebSafe(decryptedBytes);
        }

        return null;
    }

    public static String generateEncryptedSymmetricKey(Context context, String keyAdd)
    {
        try {
            SecureRandom sr = new SecureRandom();
            KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM);
            kg.init(256, sr);
            SecretKey secretKey = kg.generateKey();

            long milliSecondsInAMinute = 60000;
            long minutesRefreshTokenIsValid = 10080; //7 days
            Date startDate = new Date();
            Date endDate = new Date(startDate.getTime() + minutesRefreshTokenIsValid * milliSecondsInAMinute);
            Double serialNumber = Math.floor(Math.random() * 1000000000);

            String alias = keyAlias+keyAdd;
            String stringKey = Base64Coder.encodeWebSafe(secretKey.getEncoded());
            byte[] encryptedBytes = TokenCipherManager.encryptToken(context,  Base64Coder.decodeWebSafe(stringKey), alias, startDate, endDate, serialNumber.longValue(), "CN=SSO");
            if (encryptedBytes != null) {
                return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
            }

            return null;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (Base64DecodingException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static boolean deleteSymmetricKey(Context context, String keyAdd) {
        String alias = keyAlias+keyAdd;
        return deleteAllTokens(context, alias);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static byte[] encryptToken(Context context, byte[] token, String alias, Date startDate, Date endDate, Long serialNumber, String subject) {
        AlgorithmParameterSpec spec = new KeyPairGeneratorSpec.Builder(context.getApplicationContext())
                .setAlias(alias)
                .setStartDate(startDate)
                .setEndDate(endDate)
                .setSerialNumber(BigInteger.valueOf(serialNumber))
                .setSubject(new X500Principal(subject))
                .build();
        TokenCipher tokenCipher = new TokenCipher(context.getApplicationContext());
        try {
            return tokenCipher.encryptToken(token, spec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean deleteAllTokens(Context context, String alias) {
        TokenCipher tokenCipher = new TokenCipher(context.getApplicationContext());

        try {
            tokenCipher.deleteAlias(alias);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static byte[] decryptToken(Context context, byte[] token, String alias) {
        TokenCipher tokenCipher = new TokenCipher(context.getApplicationContext());
        try {
            return tokenCipher.decryptToken(token, alias);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}