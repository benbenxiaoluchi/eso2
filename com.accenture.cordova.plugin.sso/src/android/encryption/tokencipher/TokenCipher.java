/*Copyright 2015 Accenture

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

import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.util.*;
import android.util.Base64;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class TokenCipher {

	private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
	private static final String ALGORITHM = "RSA";
	private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";// Cryptographic algorithm/ Feedback mode/ Padding scheme

	Context context;

	public TokenCipher(Context context) {
		this.context = context;
	}

	public byte[] encryptToken(byte[] token, AlgorithmParameterSpec spec) throws InvalidAlgorithmParameterException, NoSuchProviderException, NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnrecoverableEntryException {
		PublicKey publicKey = getPublicKey(spec);
		Cipher cipher = Cipher.getInstance(TRANSFORMATION);
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return cipher.doFinal(token);
	}

	public byte[] decryptToken(byte[] token, String alias) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableEntryException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
		PrivateKey pk = getPrivateKey(alias);
		Cipher cipher = Cipher.getInstance(TRANSFORMATION);
		cipher.init(Cipher.DECRYPT_MODE, pk);
		byte[] decryptedToken = cipher.doFinal(token);
		return Arrays.copyOfRange(decryptedToken, decryptedToken.length - 32, decryptedToken.length);
	}

	public void deleteAlias(String alias) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
		ks.load(null);
		ks.deleteEntry(alias);
	}

	private KeyPair generateKeyPair(AlgorithmParameterSpec spec) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM, ANDROID_KEY_STORE);
		kpg.initialize(spec);
		return kpg.generateKeyPair();
	}

	private PublicKey getPublicKey(AlgorithmParameterSpec spec) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException, UnrecoverableEntryException, IOException, CertificateException {
		KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
		ks.load(null);
		KeyPairGeneratorSpec kpSpec = (KeyPairGeneratorSpec) spec;
		Certificate certificate = ks.getCertificate(kpSpec.getKeystoreAlias());
		PublicKey publicKey = certificate == null ? null : certificate.getPublicKey();
		if (publicKey == null) {
			publicKey = generateKeyPair(spec).getPublic();
		}
		return publicKey;
	}

	private PrivateKey getPrivateKey(String alias) throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, UnrecoverableEntryException {
		KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
		ks.load(null);
		KeyStore.Entry entry = ks.getEntry(alias, null);
		if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
			Log.w(this.getClass().getSimpleName(), "Not an instance of a PrivateKeyEntry");
			return null;
		}
		return ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
	}

}