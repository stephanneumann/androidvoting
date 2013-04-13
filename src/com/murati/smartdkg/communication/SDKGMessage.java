package com.murati.smartdkg.communication;


import com.murati.smartdkg.dkg.SDKGPlayer;

import org.jivesoftware.smack.packet.Message;
import org.spongycastle.crypto.params.RSAPrivateCrtKeyParameters;

import android.content.Context;

import javax.crypto.SecretKey;

public class SDKGMessage extends Message {
	/*
	 * Message := flag : sessionId : values
	 */

	private SDKGExtension extension;

	/*
	 * represents the type of a message - the processing of the data depends on this information
	 */
	public static class Type {

		public static final String INITIATION = "Initiation";

		public static final String JOIN = "Join";

		public static final String SECRET_SHARES = "Secret Shares";

		public static final String SECRET_SHARE_VERIFICATION_COMMITMENT = "Cik Commitments";

		public static final String COMPLAINT = "Complaint";

		public static final String PUBLIC_SHARE_COMPLAINT = "Public Share Complaint";

		public static final String COMPLAINT_RESPONSE = "Complaint Response";

		public static final String PUBLIC_SHARE_VERIFICATION_COMMITMENT = "Aik Commitment";

		public static final String RECONSTRUCTION_REQUEST = "Reconstruction Request";

		public static final String DKG_PROTOCOL_EXECUTION = "DKG Protocol Execution";

	}

	public SDKGMessage() {
		super();
		extension = new SDKGExtension();
	}

	public SDKGMessage(SDKGExtension extension) {
		super(); // TODO
		this.extension = extension;
		super.setFrom(extension.getFrom());
	}

	public SDKGMessage(String type, String sessionId, String[] values, Context context) {
		super();
		extension = new SDKGExtension(type, sessionId, values, context);
		addExtension(extension);
	}

	public SDKGMessage(RSAPrivateCrtKeyParameters privateKey, String type, String sessionId,
			String[] values, Context context) {

		super();
		extension = new SDKGExtension(privateKey, type, sessionId, values, context);
		extension.useSignature();
		addExtension(extension);
	}

	public SDKGMessage(SecretKey key, RSAPrivateCrtKeyParameters privateKey, String type,
			String sessionId, String[] values, Context context) {

		super();
		extension = new SDKGExtension(key, privateKey, type, sessionId, values, context);
		extension.useSignature();
		extension.useEncryption();
		addExtension(extension);
	}

	public String getMessageType() {
		return extension.getType();
	}

	public void setMessageType(String type) {
		extension.setType(type);
	}

	public String getSessionId() {
		return extension.getSessionId();
	}

	public void setSessionId(String sessionId) {
		extension.setSessionId(sessionId);
	}

	public String[] getValues() {
		return extension.getValues();
	}

	public void setValues(String[] values) {
		extension.setValues(values);
	}

	public boolean isInitiationMessage() {
		return getMessageType().equals(SDKGMessage.Type.INITIATION);
	}

	public boolean isJoinMessage() {
		return getMessageType().equals(SDKGMessage.Type.JOIN);
	}

	public boolean isSharesMessage() {
		return getMessageType().equals(SDKGMessage.Type.SECRET_SHARES);
	}

	// cik commitment message
	public boolean isSecretShareCommitmentMessage() {
		return getMessageType().equals(SDKGMessage.Type.SECRET_SHARE_VERIFICATION_COMMITMENT);
	}

	// aik commitment message
	public boolean isPublicShareCommitmentMessage() {
		return getMessageType().equals(SDKGMessage.Type.PUBLIC_SHARE_VERIFICATION_COMMITMENT);
	}

	public boolean isComplaintMessage() {
		return getMessageType().equals(SDKGMessage.Type.COMPLAINT);
	}

	public boolean isPublicShareComplaintMessage() {
		return getMessageType().equals(SDKGMessage.Type.PUBLIC_SHARE_COMPLAINT);
	}

	public boolean isComplaintResponseMessage() {
		return getMessageType().equals(SDKGMessage.Type.COMPLAINT_RESPONSE);
	}

	public boolean isReconstructionRequestMessage() {
		return getMessageType().equals(SDKGMessage.Type.RECONSTRUCTION_REQUEST);
	}

	public boolean isDkgProtocolExecutionMessage() {
		return getMessageType().equals(SDKGMessage.Type.DKG_PROTOCOL_EXECUTION);
	}

	public void setTo(SDKGPlayer player) {
		super.setTo(player.getName());
	}

	@Override
	public void setFrom(String from) {
		super.setFrom(from);
		extension.setFrom(from);
	}

}
