package com.androidvoting.murati.smartdkg.communication;

import com.androidvoting.murati.smartdkg.util.SDKGKeyFactory;
import com.androidvoting.murati.smartdkg.util.SDKGXmlEncModule;
import com.androidvoting.murati.smartdkg.util.SDKGXmlSigModule;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.params.RSAKeyParameters;
import org.spongycastle.util.encoders.Base64;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;

import java.io.IOException;
import java.io.StringReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class SDKGProvider implements PacketExtensionProvider {

	private SDKGExtension extension;

	private Context context;

	public SDKGProvider(Context context) {
		this.context = context;
		SDKGKeyFactory.init(this.context);
	}

	/*
	 * first step of the signature verification
	 */
	public boolean verifyReferences(String receivedDigest, String myMessage)
			throws NoSuchAlgorithmException, NoSuchProviderException {

		byte[] myDigest = SDKGXmlSigModule.getDigest(myMessage.getBytes());
		String myDigestEncoded = new String(Base64.encode(myDigest));
		return myDigestEncoded.equals(receivedDigest);
	}

	/*
	 * second step of signature verifcation. if everything matches the signature is valid
	 */
	public boolean verifySignature(String signerKeyName, String message, String encodedSignature)
			throws IOException {

		SDKGKeyFactory.init(context);

		byte[] signatureBytes = encodedSignature.getBytes();
		RSAKeyParameters pubkey = SDKGKeyFactory.loadRSAPublicKey(signerKeyName);
		boolean verified = SDKGXmlSigModule.verify(message.getBytes(), signatureBytes, pubkey);
		return verified;
	}

	private StringBuilder readAttributes(XmlPullParser parser) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			sb.append(parser.getAttributeName(i));
			sb.append("=").append("\"").append(parser.getAttributeValue(i)).append("\"");
		}
		return sb;
	}

	private String proceedSignedInfo(XmlPullParser parser)
			throws XmlPullParserException, IOException {

		StringBuilder sb = new StringBuilder();
		int eventType = parser.getEventType();
		boolean emptyTag = false;
		boolean done = false;

		while (!done) {
			eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				if (parser.isEmptyElementTag()) {
					sb.append("<").append(parser.getName());
					if (parser.getAttributeCount() > 0)
						sb.append(" ").append(readAttributes(parser).toString());
					sb.append("/>");
					emptyTag = true;
				} else {
					sb.append("<").append(parser.getName());
					if (parser.getAttributeCount() > 0)
						sb.append(readAttributes(parser).toString());
					sb.append(">");
					emptyTag = false;
				}
			} else if (eventType == XmlPullParser.TEXT) {
				sb.append(parser.getText());
			} else if (eventType == XmlPullParser.END_TAG) {
				if (!emptyTag)
					sb.append("</").append(parser.getName()).append(">");
				if (parser.getName().equals("SignedInfo")) {
					done = true;
				}
			}
		}
		return sb.toString();
	}

	public String decryptValues(String tag, SecretKey key)
			throws InvalidCipherTextException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {

		byte[] decrypted = SDKGXmlEncModule.decrypt(key.getEncoded(), tag.getBytes());
		return new String(decrypted);
	}

	private void parseValues(XmlPullParser parser) throws XmlPullParserException, IOException {
		ArrayList<String> itemList = new ArrayList<String>();
		int eventType = parser.getEventType();
		boolean done = false;

		while (!done) {
			eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				if (parser.getName().equals("item")) {
					itemList.add(parser.nextText());
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				if (parser.getName().equals("values")) {
					done = true;
				}
			}
		}
		String[] val = new String[itemList.size()];
		extension.setValues(itemList.toArray(val));
	}

	@Override
	public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
		extension = new SDKGExtension();
		String signedInfo = "";
		boolean done = false;

		while (!done) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				if (parser.getName().equals("sessionId")) {
					extension.setSessionId(parser.nextText());
				}
				if (parser.getName().equals("type")) {
					extension.setType(parser.nextText());
				}
				if (parser.getName().equals("values")) {
					parseValues(parser);
				}
				if (parser.getName().equals("EncryptedData")) {
					parser.next();
					parser.next();
					String encrypted = parser.nextText();
					SecretKey key = SDKGKeyFactory.loadAESKey("aeskey");
					String decrypted = decryptValues(encrypted, key);
					XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
					XmlPullParser xmlParser = xmlFactory.newPullParser();
					xmlParser.setInput(new StringReader(decrypted));
					parseValues(xmlParser);
				}
				// no javax.xml.crypto for Android. Have to do the stuff on
				// myself
				if (parser.getName().equals("Signature")) {
					signedInfo = proceedSignedInfo(parser);
					int startIndex = signedInfo.indexOf("<DigestValue>");
					int endIndex = signedInfo.indexOf("</DigestValue>");
					String receivedDigest = signedInfo.substring(startIndex + 13, endIndex);
					String message = extension.toXML();
					verifyReferences(receivedDigest, message);
				}
				if (parser.getName().equals("SignatureValue")) {
					String signatureValue = parser.nextText();
					parser.next();
					parser.next();
					String signerKeyName = parser.nextText();
					verifySignature(signerKeyName, signedInfo, signatureValue);
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				if (parser.getName().equals(SDKGExtension.ELEMENT_NAME)) {
					done = true;
				}
			}
		}
		return extension;
	}
}
