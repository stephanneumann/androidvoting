package com.murati.smartdkg.util;

import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Base64;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;


public class SDKGXmlEncModule {

	public static final int STRENGTH = 128;

	public static byte[] encrypt(byte[] keyBytes, byte[] input)
			throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, DataLengthException, IllegalStateException, InvalidCipherTextException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {

		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		Cipher cipher = Cipher.getInstance("AES", "SC");
		cipher.init(Cipher.ENCRYPT_MODE, keySpec);
		byte[] encrypted = cipher.doFinal(input);
		return Base64.encode(encrypted);
	}

	public static byte[] decrypt(byte[] keyBytes, byte[] encodedInput)
			throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		Cipher cipher = Cipher.getInstance("AES", "SC");
		byte[] input = Base64.decode(encodedInput);
		cipher.init(Cipher.DECRYPT_MODE, keySpec);
		byte[] decrypted = cipher.doFinal(input);
		return decrypted;
	}

}
