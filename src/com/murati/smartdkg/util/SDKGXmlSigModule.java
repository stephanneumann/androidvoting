package com.murati.smartdkg.util;

import org.spongycastle.crypto.CryptoException;
import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.digests.SHA1Digest;
import org.spongycastle.crypto.params.RSAKeyParameters;
import org.spongycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.spongycastle.crypto.signers.RSADigestSigner;
import org.spongycastle.util.encoders.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class SDKGXmlSigModule {

	public static byte[] getDigest(byte[] toDigest)
			throws NoSuchAlgorithmException, NoSuchProviderException {

		MessageDigest shaDigest = MessageDigest.getInstance("SHA-1", "SC");
		shaDigest.update(toDigest);
		return shaDigest.digest();
	}

	public static byte[] sign(byte[] message, RSAPrivateCrtKeyParameters privateKey)
			throws DataLengthException, CryptoException {

		SHA1Digest digest = new SHA1Digest();
		RSADigestSigner rsaSigner = new RSADigestSigner(digest);
		rsaSigner.init(true, privateKey);
		rsaSigner.update(message, 0, message.length);
		byte[] decodedSignature = rsaSigner.generateSignature();
		byte[] encodedSignature = Base64.encode(decodedSignature);
		return encodedSignature;
	}

	public static boolean verify(byte[] message, byte[] encodedSignature,
			RSAKeyParameters publicKey) {

		SHA1Digest digest = new SHA1Digest();
		byte[] decodedSignature = Base64.decode(encodedSignature);
		RSADigestSigner rsaSigner = new RSADigestSigner(digest);
		rsaSigner.init(false, publicKey);
		rsaSigner.update(message, 0, message.length);
		return rsaSigner.verifySignature(decodedSignature);
	}

}
