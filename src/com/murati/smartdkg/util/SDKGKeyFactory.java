package com.murati.smartdkg.util;

import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.generators.RSAKeyPairGenerator;
import org.spongycastle.crypto.params.RSAKeyGenerationParameters;
import org.spongycastle.crypto.params.RSAKeyParameters;
import org.spongycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.spongycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateCrtKey;
import org.spongycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openssl.PEMReader;
import org.spongycastle.openssl.PEMWriter;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SDKGKeyFactory {

	private static final BigInteger PUB_EXPONENT = new BigInteger("1234567890123456789");

	private static final int STRENGTH = 2048;

	private static final int CERTAINTY = 80;

	//////////////////////////////////////////////////

	private static final String PUBKEY_DIRNAME = "public_keys";

	private static final String PRIVKEY_DIRNAME = "private_keys";

	private static final String AES_KEY_DIRNAME = "aes_keys";

	//////////////////////////////////////////////////

	private static final int AES_KEY_STRENGTH = 128; // bit

	private static final int AES_KEY_STRENGTH_BYTE = AES_KEY_STRENGTH >> 3;

	//////////////////////////////////////////////////

	private static Context appContext;

	private static final String TAG = "SDKGKeyPairFactory";

	public static void init(Context context) {
		Security.addProvider(new BouncyCastleProvider());
		appContext = context;
	}

	public static void storeRSAPublicKey(String filename, PublicKey publicKey) {
		File dir = appContext.getDir(PUBKEY_DIRNAME, Context.MODE_PRIVATE);
		File file = new File(dir, filename + ".pem");
		BufferedWriter bufWriter = null;
		PEMWriter pemWriter = null;

		try {
			bufWriter = new BufferedWriter(new FileWriter(file));
			pemWriter = new PEMWriter(bufWriter);
			pemWriter.flush();
			pemWriter.writeObject(publicKey);
		} catch (IOException e) {
			Log.e(TAG, "could not create new bufferedwriter, maybe the file with the specified filename could not be found");
		} finally {
			try {
				pemWriter.close();
				bufWriter.close();
			} catch (IOException e) {
				Log.e(TAG, "could not close bufferedwriter");
			}
		}
	}

	public static void storeRSAPublicKey(String filename, byte[] pubkeyBytes) {
		KeyFactory keyFactory = null;
		X509EncodedKeySpec spec = null;
		PublicKey pubkey = null;

		try {
			keyFactory = KeyFactory.getInstance("RSA", "SC");
			spec = new X509EncodedKeySpec(pubkeyBytes);
			pubkey = keyFactory.generatePublic(spec);
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "the requested algorithm does not exist");
		} catch (NoSuchProviderException e) {
			Log.e(TAG, "the requested security provider is invalid");
		} catch (InvalidKeySpecException e) {
			Log.e(TAG, "the used key is invalid (maybe invalid encoding, wrong length, uninitialized, etc)");
		}

		storeRSAPublicKey(filename, pubkey);
	}

	public static void storeRSAPrivateKey(String filename, PrivateKey privateKey) {
		File dir = appContext.getDir(PRIVKEY_DIRNAME, Context.MODE_PRIVATE);
		File file = new File(dir, filename + ".pem");
		BufferedWriter bufWriter = null;
		PEMWriter pemWriter = null;

		try {
			bufWriter = new BufferedWriter(new FileWriter(file));
			pemWriter = new PEMWriter(bufWriter);
			pemWriter.flush();
			pemWriter.writeObject(privateKey);
		} catch (IOException e) {
			Log.e(TAG, "could not create new bufferedwriter");
		} finally {
			try {
				pemWriter.close();
				bufWriter.close();
			} catch (IOException e) {
				Log.e(TAG, "could not close bufferedwriter");
			}
		}
	}

	public static void storeRSAPrivateKey(String filename, byte[] privkeyBytes) {
		KeyFactory keyFactory = null;
		PKCS8EncodedKeySpec spec = null;
		PrivateKey privateKey = null;

		try {
			keyFactory = KeyFactory.getInstance("RSA", "SC");
			spec = new PKCS8EncodedKeySpec(privkeyBytes);
			privateKey = keyFactory.generatePrivate(spec);
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "the requested algorithm does not exist");
		} catch (NoSuchProviderException e) {
			Log.e(TAG, "the requested security provider is invalid");
		} catch (InvalidKeySpecException e) {
			Log.e(TAG, "the used key is invalid (maybe invalid encoding, wrong length, uninitialized, etc)");
		}

		storeRSAPrivateKey(filename, privateKey);
	}

	public static RSAKeyParameters loadRSAPublicKey(String filename)
			throws IOException {

		File dir = appContext.getDir(PUBKEY_DIRNAME, Context.MODE_PRIVATE);
		File file = new File(dir, filename + ".pem");
		BufferedReader bufReader = null;
		PEMReader pemReader = null;
		BCRSAPublicKey publicKey = null;
		RSAKeyParameters myPublicKey = null;

		try {
			bufReader = new BufferedReader(new FileReader(file));
			pemReader = new PEMReader(bufReader);
			publicKey = (BCRSAPublicKey)pemReader.readObject();
			myPublicKey = new RSAKeyParameters(false, publicKey.getModulus(),
					publicKey.getPublicExponent());
		} catch (FileNotFoundException e) {
			Log.e(TAG, "could not find a file with the specified filename");
		} catch (IOException e) {
			Log.e(TAG, "could not create new bufferedreader");
		} finally {
			try {
				pemReader.close();
				bufReader.close();
			} catch (IOException e) {
				Log.e(TAG, "could not close bufferedreader");
			}
		}

		return myPublicKey;
	}

	public static PublicKey loadRSAPublicKeyJCE(String filename) {
		File dir = appContext.getDir(PUBKEY_DIRNAME, Context.MODE_PRIVATE);
		File file = new File(dir, filename + ".pem");
		BufferedReader bufReader = null;
		PEMReader pemReader = null;
		BCRSAPublicKey publicKey = null;

		try {
			bufReader = new BufferedReader(new FileReader(file));
			pemReader = new PEMReader(bufReader);
			publicKey = (BCRSAPublicKey) pemReader.readObject();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "could not find a file with the specified filename");
		} catch (IOException e) {
			Log.e(TAG, "could not create new bufferedreader");
		}

		return publicKey;
	}

	public static RSAPrivateCrtKeyParameters loadRSAPrivateKey(String filename)
			throws IOException {

		File dir = appContext.getDir(PRIVKEY_DIRNAME, Context.MODE_PRIVATE);
		File file = new File(dir, filename + ".pem");
		BufferedReader bufReader = null;
		PEMReader pemReader = null;
		KeyPair keyPair = null;
		BCRSAPrivateCrtKey privateKey = null;
		RSAPrivateCrtKeyParameters myPrivateKey = null;

		try {
			bufReader = new BufferedReader(new FileReader(file));
			pemReader = new PEMReader(bufReader);
			keyPair = (KeyPair)pemReader.readObject();
			privateKey = (BCRSAPrivateCrtKey)keyPair.getPrivate();
			myPrivateKey = new RSAPrivateCrtKeyParameters(
					privateKey.getModulus(),
					privateKey.getPublicExponent(),
					privateKey.getPrivateExponent(),
					privateKey.getPrimeP(),
					privateKey.getPrimeQ(),
					privateKey.getPrimeExponentP(),
					privateKey.getPrimeExponentQ(),
					privateKey.getCrtCoefficient());
		} catch (FileNotFoundException e) {
			Log.e(TAG, "could not find a file with the specified filename");
		} catch (IOException e) {
			Log.e(TAG, "could not create a new bufferedreader");
		} finally {
			try {
				pemReader.close();
				bufReader.close();
			} catch (IOException e) {
				Log.e(TAG, "could not close bufferedreader");
			}
		}

		return myPrivateKey;
	}

	public static PrivateKey loadRSAPrivateKeyJCE(String filename) {
		File dir = appContext.getDir(PRIVKEY_DIRNAME, Context.MODE_PRIVATE);
		File file = new File(dir, filename + ".pem");
		BufferedReader bufReader = null;
		PEMReader pemReader = null;
		KeyPair keyPair = null;
		PrivateKey privateKey = null;

		try {
			bufReader = new BufferedReader(new FileReader(file));
			pemReader = new PEMReader(bufReader);
			keyPair = (KeyPair)pemReader.readObject();
			privateKey = keyPair.getPrivate();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "could not find a file with the specified filename");
		} catch (IOException e) {
			Log.e(TAG, "could not create a new bufferedreader");
		}

		return privateKey;
	}

	public static KeyPair generateKeyPair()
			throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {

		KeyPairGenerator keyPair = KeyPairGenerator.getInstance("RSA", "SC");
		RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(STRENGTH, PUB_EXPONENT);
		keyPair.initialize(spec);
		KeyPair myKeyPair = keyPair.generateKeyPair();
		return myKeyPair;
	}

	public static AsymmetricCipherKeyPair generateKeyPairBouncy() {
		RSAKeyPairGenerator keyGen = new RSAKeyPairGenerator();
		keyGen.init(new RSAKeyGenerationParameters(PUB_EXPONENT, new SecureRandom(), STRENGTH,
				CERTAINTY));
		AsymmetricCipherKeyPair keyPair = keyGen.generateKeyPair();
		return keyPair;
	}

	///////////////////////////////////////////////////////////////

	public static SecretKey generateAESKey() throws NoSuchAlgorithmException, NoSuchProviderException, UnsupportedEncodingException {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES", "SC");
		keyGen.init(AES_KEY_STRENGTH, new SecureRandom());
		SecretKey key = keyGen.generateKey();
		return new SecretKeySpec(key.getEncoded(), 0, AES_KEY_STRENGTH_BYTE, "AES");
	}

	public static void storeAESKey(String filename, SecretKey key) {
		byte[] keyBytes = key.getEncoded();
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		File dir = appContext.getDir(AES_KEY_DIRNAME, Context.MODE_PRIVATE);

		try {
			fos = new FileOutputStream(new File(dir, filename));
			bos = new BufferedOutputStream(fos);
			bos.write(keyBytes, 0, keyBytes.length);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "could not find a file with the specified filename");
		} catch (IOException e) {
			Log.e(TAG, "could not create new fileoutputstream");
		} finally {
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					Log.e(TAG, "could not close bufferedoutputstream");
				}
			}
		}
	}

	public static SecretKey loadAESKey(String filename) {
		byte[] keyBytes = new byte[16];
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		File dir = appContext.getDir(AES_KEY_DIRNAME, Context.MODE_PRIVATE);

		try {
			fis = new FileInputStream(new File(dir, filename));
			bis = new BufferedInputStream(fis);
			bis.read(keyBytes, 0, keyBytes.length);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "could not find a file with the specified filename");
		} catch (IOException e) {
			Log.e(TAG, "could not create new fileinputstream");
		} finally {
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					Log.e(TAG, "could not close bufferedinputstream");
				}
			}
		}
		return new SecretKeySpec(keyBytes, 0, AES_KEY_STRENGTH_BYTE, "AES");
	}

}
