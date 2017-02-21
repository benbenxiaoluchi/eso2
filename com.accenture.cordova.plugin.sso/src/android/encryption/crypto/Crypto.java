package com.accenture.cordova.plugin.sso;

import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.keyczar.Crypter;
import org.keyczar.Encrypter;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;
import org.keyczar.util.Base64Coder;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by joshualamkin on 9/16/15.
 */
public class Crypto {

    private static final String ALGORITHM_HMAC_SHA1 = "HmacSHA1";

    public static String decrypt(final String encryptionKey,final  String hmac,final String encryptedData) {
        try {
            final JSONObject hmacKey = new JSONObject();
            hmacKey.put("size", 256);
            hmacKey.put("hmacKeyString", hmac);
            final JSONObject key = createKeyJson(encryptionKey, hmacKey); // Key data should be part of arguments
            final JSONObject metadata = getMetadata(); // Metadata should be part of arguments
            final Crypter crypter = new Crypter(getReader(key, metadata));
            final String decryptedData = crypter.decrypt(encryptedData);
            final byte[] decodedDecryptedData = Base64Coder.decodeWebSafe(decryptedData);
            return new String(decodedDecryptedData);
        } catch (KeyczarException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    //first is encrypted data, second is hmac
    public static Pair<String,String> encrypt(String encryptionKey, String decryptedData) {
        try {
            final JSONObject key = createKeyJson(encryptionKey, generateHMACKey(encryptionKey.getBytes(), String.valueOf(System.currentTimeMillis()).getBytes())); // Key data should be part of arguments
            final JSONObject metadata = getMetadata(); // Metadata should be part of arguments
            final Encrypter encrypter = new Encrypter(getReader(key, metadata));
            final String data = Base64Coder.encodeWebSafe(decryptedData.getBytes());
            final String encryptedData = encrypter.encrypt(data);
            return new Pair<String, String>(encryptedData, ((JSONObject)key.get("hmacKey")).getString("hmacKeyString"));
        } catch (KeyczarException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static JSONObject createKeyJson(String encryptionKey, final JSONObject hmacKey) throws JSONException, NoSuchAlgorithmException, InvalidKeyException {
        final JSONObject key = new JSONObject();
        key.put("aesKeyString", encryptionKey);
        key.put("size", 256);
        key.put("hmacKey", hmacKey);
        key.put("mode", "CBC");
        return key;
    }

    private static KeyczarReader getReader(final JSONObject key, final JSONObject metadata) {
        return new KeyczarReader() {
            @Override
            public String getKey(int i) throws KeyczarException {
                return key.toString();
            }

            @Override
            public String getKey() throws KeyczarException {
                return key.toString();
            }

            @Override
            public String getMetadata() throws KeyczarException {
                return metadata.toString();
            }
        };
    }

    private static JSONObject getMetadata() throws JSONException {
        final JSONObject version = new JSONObject();
        version.put("exportable", false);
        version.put("status", "PRIMARY");
        version.put("versionNumber", 1);
        final JSONArray versions = new JSONArray();
        versions.put(version);
        final JSONObject metadata = new JSONObject();
        metadata.put("name", "key");
        metadata.put("purpose", "DECRYPT_AND_ENCRYPT");
        metadata.put("type", "AES");
        metadata.put("encrypted", false);
        metadata.put("versions", versions);
        return metadata;
    }

    private static JSONObject generateHMACKey(final byte[] key, final byte[] message) throws NoSuchAlgorithmException, InvalidKeyException, JSONException {
        final SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM_HMAC_SHA1);
        final Mac mac = Mac.getInstance(ALGORITHM_HMAC_SHA1);
        mac.init(keySpec);
        final byte[] result = mac.doFinal(message);
        final String hmacKey = Base64Coder.encodeWebSafe(result);
        final JSONObject hmacKeyJson = new JSONObject();
        hmacKeyJson.put("size", 256);
        hmacKeyJson.put("hmacKeyString", hmacKey);
        return hmacKeyJson;
    }
}
