package com.murati.smartdkg.communication;

import com.murati.smartdkg.util.SDKGKeyFactory;
import com.murati.smartdkg.util.SDKGXmlEncModule;
import com.murati.smartdkg.util.SDKGXmlSigModule;

import org.jivesoftware.smack.packet.PacketExtension;
import org.spongycastle.crypto.CryptoException;
import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.spongycastle.util.encoders.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.StringWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class SDKGExtension implements PacketExtension {

	private String from; // sender

	private String type; // message type e.g. complaint

	private String sessionId; // current session id of an protocol instance

	private String[] values; // the payload of a message e.g. the share values

	//////////////////////////////////////////////////

	private Document document; // the enveloping xml document

	private Element rootElement; // the root element of a protocol specific message (smartdkg)

	private Context context; // current context (needed for key loading/storing operations)

	private String signerKeyName; // the name of the player who signed the current message data

	private String publicKeyName; // name of opposite player (needed for signature verification)

	private RSAPrivateCrtKeyParameters privateKey; // my rsa private key

	private boolean useSignature = false;

	private boolean useEncryption = false;

	private SecretKey key; // my aes key for encryption/decryption

	//////////////////////////////////////////////////

	public static final String ELEMENT_NAME = "smartdkg";

	public static final String NAMESPACE = "dkg:xmpp:dkgmessageext:0";

	private static final String SIGNATURE_NS = "http://www.w3.org/2000/09/xmldsig#";

	private static final String CANONICALIZATION_METHOD_ALG = "http://www.w3.org/2006/12/xml-c14n11";

	private static final String SIGNATURE_METHOD_ALG = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";

	private static final String DIGEST_METHOD_ALG = "http://www.w3.org/2000/09/xmldsig#sha1";

	private static final String ENCRYPTION_NS = "http://www.w3.org/2001/04/xmlenc#";

	private static final String ENCRYPTION_MODE = "http://www.w3.org/2001/04/xmlenc#Element";

	private static final String TAG = "SDKGExtension";

	/*
	 * although this seems useless it is needed for parsing purposes
	 */
	public SDKGExtension() {

	}

	public SDKGExtension(String type, String sessionId, String[] values, Context context) {
		this.type = type;
		this.sessionId = sessionId;
		this.setValues(values);
		this.context = context;
	}

	public SDKGExtension(RSAPrivateCrtKeyParameters privateKey, String type, String sessionId,
			String[] values, Context context) {

		this(type, sessionId, values, context);
		this.privateKey = privateKey;
	}

	public SDKGExtension(SecretKey key, RSAPrivateCrtKeyParameters privateKey, String type,
			String sessionId, String[] values, Context context) {

		this(privateKey, type, sessionId, values, context);
		this.key = key;
	}

	/*
	 * creates a xml document node
	 */
	private Document createNewDocument() {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		docFactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = null;

		try {
			docBuilder = docFactory.newDocumentBuilder();
			document = docBuilder.newDocument();
		} catch (ParserConfigurationException e) {
			Log.e(TAG, "could not create new document element");
		}

		return document;
	}

	/*
	 * root element - nameley smartdkg
	 */
	private Element createRootElement() {
		rootElement = document.createElement(ELEMENT_NAME);
		rootElement.setAttribute("xmlns", NAMESPACE);
		return rootElement;
	}

	/*
	 * type tag with the specified value
	 */
	private Element createTypeElement(String value) {
		Element typeElement = document.createElement("type");
		typeElement.appendChild(document.createTextNode(value));
		return typeElement;
	}

	/*
	 * represents a tag for the given session id with the specified value
	 */
	private Element createSessionIdElement(String value) {
		Element sessionIdElement = document.createElement("sessionId");
		sessionIdElement.appendChild(document.createTextNode(value));
		return sessionIdElement;
	}

	/*
	 * represents a values tag with item sub tags
	 */
	private Element createValuesElement(String[] values) {
		Element valueElement = null;
		if (values != null) {
			valueElement = document.createElement("values");
			for (String value : values) {
				if (value != null) {
					Element itemTag = document.createElement("item");
					itemTag.appendChild(document.createTextNode(value));
					valueElement.appendChild(itemTag);
				}
			}
		}
		return valueElement;
	}

	/*
	 * creates a string representation of a xml node. this is important if we want to encrypt/decrypt data also for parsing purposes
	 * 
	 * see URL
	 */
	protected String nodeToString(Node node) throws TransformerException {
		TransformerFactory xmlFactory = TransformerFactory.newInstance();
		Transformer xmlTransformer = xmlFactory.newTransformer();
		xmlTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringWriter strWriter = new StringWriter();
		xmlTransformer.transform(new DOMSource(node), new StreamResult(strWriter));
		String xmlOutput = strWriter.getBuffer().toString();
		return xmlOutput;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getFrom() {
		return from;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String[] getValues() {
		return values;
	}

	public void setValues(String[] values) {
		if (values == null) {
			this.values = null;
		} else {
			this.values = new String[values.length];
			for (int i = 0; i < values.length; i++) {
				this.values[i] = values[i];
			}
		}
	}

	@Override
	public String getElementName() {
		return ELEMENT_NAME;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public Document getDocument() {
		return document;
	}

	public void setRootElement(Element root) {
		rootElement = root;
	}

	public Element getRootElement() {
		return rootElement;
	}

	public Context getContext() {
		return context;
	}

	///////////////////// SIGNATURE /////////////////////

	// <Signature>
	private Element createSignatureElement() {
		Element signatureElement = document.createElement("Signature");
		signatureElement.setAttribute("xmlns", SIGNATURE_NS);
		return signatureElement;
	}

	// <SignedInfo>
	private Element createSignedInfoElement() {
		Element signedInfoElement = document.createElement("SignedInfo");
		return signedInfoElement;
	}

	// <CanonicalizationMethod>
	private Element createCanonicalizationMethodElement() {
		Element canonicalizationMethodElement = document
				.createElement("CanonicalizationMethod");
		canonicalizationMethodElement.setAttribute("Algorithm", CANONICALIZATION_METHOD_ALG);
		return canonicalizationMethodElement;
	}

	// <SignatureMethod>
	private Element createSignatureMethodElement() {
		Element signatureMethodElement = document.createElement("SignatureMethod");
		signatureMethodElement.setAttribute("Algorithm", SIGNATURE_METHOD_ALG);
		return signatureMethodElement;
	}

	// <Reference>
	private Element createReferenceElement(String uri) {
		Element referenceElement = document.createElement("Reference");
		if (!uri.equals(""))
			referenceElement.setAttribute("URI", uri);
		return referenceElement;
	}

	// <DigestMethod>
	private Element createDigestMethodElement() {
		Element digestMethodElement = document.createElement("DigestMethod");
		digestMethodElement.setAttribute("Algorithm", DIGEST_METHOD_ALG);
		return digestMethodElement;
	}

	// <DigestValue>
	private Element createDigestValueElement(String value) {
		Element digestValueElement = document.createElement("DigestValue");
		digestValueElement.appendChild(document.createTextNode(value));
		return digestValueElement;
	}

	// <SignatureValue>
	private Element createSignatureValueElement(String value) {
		Element signatureValueElement = document.createElement("SignatureValue");
		signatureValueElement.appendChild(document.createTextNode(value));
		return signatureValueElement;
	}

	// <KeyInfo>
	private Element createKeyInfoElement() {
		Element keyInfoElement = document.createElement("KeyInfo");
		return keyInfoElement;
	}

	// <KeyName>
	private Element createKeyNameElement(String value) {
		Element keyNameElement = document.createElement("KeyName");
		keyNameElement.appendChild(document.createTextNode(value));
		return keyNameElement;
	}

	public String getSignerKeyName() {
		return signerKeyName;
	}

	public Element createSignedDocument(RSAPrivateCrtKeyParameters privkey)
			throws TransformerException, DataLengthException, CryptoException,
			NoSuchAlgorithmException, NoSuchProviderException {

		Element signature = createSignatureElement();
		Element signedInfo = createSignedInfoElement();
		Element canonicalMethod = createCanonicalizationMethodElement();
		Element signatureMethod = createSignatureMethodElement();
		Element reference = createReferenceElement("");
		Element digestMethod = createDigestMethodElement();

		// 1. create digest of the data
		// 2. encode with base64

		// create digest of the data
		String dataToDigest = nodeToString(document);
		byte[] dataToDigestBytes = SDKGXmlSigModule.getDigest(dataToDigest.getBytes());

		// encode with base64
		String digVal = new String(Base64.encode(dataToDigestBytes));

		Element digestValue = createDigestValueElement(digVal);

		reference.appendChild(digestMethod);
		reference.appendChild(digestValue);
		signedInfo.appendChild(canonicalMethod);
		signedInfo.appendChild(signatureMethod);
		signedInfo.appendChild(reference);

		// ---------------------------------------------------

		// 1. create digest of signedInfo element
		// 2. sign the digest from 1
		// 3. encode with base64

		// create digest of signedInfo
		String signedInfoToString = nodeToString(signedInfo);
		byte[] signedInfoDigest = SDKGXmlSigModule.getDigest(signedInfoToString.getBytes());
		byte[] signedInfoSignature = SDKGXmlSigModule.sign(signedInfoDigest, privkey);
		String signedInfoSignatureEncoded = new String(signedInfoSignature); // base64 encoded

		Element signatureValue = createSignatureValueElement(signedInfoSignatureEncoded);
		Element keyInfo = createKeyInfoElement();
		Element keyName = createKeyNameElement(from.split("@")[0]);
		keyInfo.appendChild(keyName);
		signature.appendChild(signedInfo);
		signature.appendChild(signatureValue);
		signature.appendChild(keyInfo);
		return signature;
	}

	public void useSignature() {
		useSignature = true;
	}

	//////////////////// ENCRYPTION //////////////////////

	// <EncryptedData>
	private Element createEncryptedDataElement() {
		Element encryptedDataElement = document.createElement("EncryptedData");
		encryptedDataElement.setAttribute("xmlns", ENCRYPTION_NS);
		encryptedDataElement.setAttribute("Type", ENCRYPTION_MODE);
		return encryptedDataElement;
	}

	// <CipherData>
	private Element createCipherDataElement() {
		Element cipherData = document.createElement("CipherData");
		return cipherData;
	}

	// <CipherValue>
	private Element createCipherValueElement(String value) {
		Element cipherValue = document.createElement("CipherValue");
		cipherValue.appendChild(document.createTextNode(value));
		return cipherValue;
	}

	public String getPublicKeyName() {
		return publicKeyName;
	}

	public void useEncryption() {
		useEncryption = true;
	}

	/*
	 * the player who created this document (= the sender of the enveloping message) - this information is needed to append the key information to the message
	 */
	public void setIssuer(String issuer) {
		signerKeyName = issuer;
	}

	/*
	 * returns xml encryption elements
	 * 
	 * <EncryptedData>
	 * 	<CipherData>
	 * 		<CipherValue>CIPHER</CipherValue>
	 * 	</CipherData>
	 * </EncryptedData>
	 */
	public Element encryptedValuesElement(Element tag, SecretKey key)
			throws InvalidCipherTextException, TransformerException, DataLengthException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, IllegalStateException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

		String toEncrypt = nodeToString(tag);
		byte[] toEncryptBytes = toEncrypt.getBytes();
		String encrypted = new String(SDKGXmlEncModule.encrypt(key.getEncoded(), toEncryptBytes));
		Element encData = createEncryptedDataElement();
		Element cipherData = createCipherDataElement();
		Element cipherValue = createCipherValueElement(encrypted);
		cipherData.appendChild(cipherValue);
		encData.appendChild(cipherData);
		return encData;
	}

	@Override
	public String toXML() {
		String xmlString = "";
		try {
			if (document == null) {
				document = createNewDocument();
				Element smartdkgTag = createRootElement();
				Element typeTag = createTypeElement(getType());
				Element sessionIdTag = createSessionIdElement(getSessionId());
				Element valueTag = createValuesElement(getValues());

				SDKGKeyFactory.init(getContext());

				smartdkgTag.appendChild(typeTag);
				smartdkgTag.appendChild(sessionIdTag);

				if (getValues() != null) {
					smartdkgTag.appendChild(valueTag);
				}

				if (useSignature) {
					privateKey = SDKGKeyFactory.loadRSAPrivateKey("privkey");
					Element signatureElement = createSignedDocument(privateKey);
					rootElement.appendChild(signatureElement);
				}

				if (useEncryption) {
					NodeList allValues = rootElement.getElementsByTagName("values");

					if (allValues != null) {
						Element valuesElement = (Element)allValues.item(0);
						Element encData = encryptedValuesElement(valuesElement, key);
						rootElement.appendChild(encData);
						rootElement.replaceChild(encData, valuesElement);
					}
				}

				document.appendChild(smartdkgTag);
			}

			xmlString = nodeToString(document);

		} catch (TransformerException e) {
			Log.e(TAG, "could not transform xml object to string");
		} catch (IOException e) {
			Log.e(TAG, "could not load or store rsa private key from or to a file");
		} catch (InvalidCipherTextException e) {
			Log.e(TAG, "could not create cipher");
		} catch (DataLengthException e) {
			Log.e(TAG, "invalid data length");
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "the requested algorithm does not exist");
		} catch (NoSuchProviderException e) {
			Log.e(TAG, "the requested security provider is invalid");
		} catch (CryptoException e) {
			Log.e(TAG, "caused error by using cryptographic operation");
		} catch (NoSuchPaddingException e) {
			Log.e(TAG, "the requested padding scheme is invalid");
		} catch (IllegalStateException e) {
			Log.e(TAG, "illegal state");
		} catch (InvalidKeyException e) {
			Log.e(TAG, "the used key is invalid (maybe invalid encoding, wrong length, uninitialized, etc)");
		} catch (IllegalBlockSizeException e) {
			Log.e(TAG, "the length of data provided to a block cipher is incorrect");
		} catch (BadPaddingException e) {
			Log.e(TAG, "input data is not padded properly");
		}
		return xmlString;
	}

}
