package com.androidvoting.oksana.protocol;

import java.math.BigInteger;
import java.util.Timer;
import java.util.TimerTask;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;

import android.content.Context;

import com.androidvoting.murati.smartdkg.communication.SDKGExtension;
import com.androidvoting.murati.smartdkg.communication.SDKGMessage;
import com.androidvoting.murati.smartdkg.communication.SDKGProvider;
import com.androidvoting.murati.smartdkg.dkg.SDKGElGamalParameters;
import com.androidvoting.murati.smartdkg.dkg.SDKGPlayer;
import com.androidvoting.murati.smartdkg.dkg.SDKGPlayerList;
import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZqElement;
import com.androidvoting.oksana.application.UserData;
import com.androidvoting.oksana.data.DecryptionCommitmentsMap;

public class CommitmentsInitialisation implements PacketListener{
	public CommitmentsInitialisation(SDKGElGamalParameters parameters, String sessionId,
			Connection connection, Context context,
			SDKGPlayer myself, SDKGPlayerList players, BigInteger myKeyShare) {
		this.parameters = parameters;
		this.sessionId = sessionId;
		this.connection = connection;
		this.context = context;
		this.myself = myself;
		this.players = players;
		this.myDecryptionKey = myKeyShare;
		
		ProviderManager.getInstance().addExtensionProvider(SDKGExtension.ELEMENT_NAME,
				SDKGExtension.NAMESPACE, new SDKGProvider(context));
		
		connection.addPacketListener(this, null);
		
		this.state = State.INITIAL;
		
		computeCommitment();
	}
	
	public enum State { 
		INITIAL, COMMITMENT_COMPUTED, COMMITMENTS_EXCHANGED, PROTOCOL_EXECUTION_COMPLETED, ERROR
	}

	private SDKGElGamalParameters parameters;
	
	private String sessionId; // unique id for the current instance of the dkg protocol

	private Connection connection; // for sending messages

	private State state; // current progress of the executed protocol instance

	private Context context; // app context
	
	private SDKGPlayer myself;

	private SDKGPlayerList players;
		
	private BigInteger myDecryptionKey; //xi
	
	private SDKGZqElement myCommitment;
	
	public static DecryptionCommitmentsMap commitmentsMap = new DecryptionCommitmentsMap();
	
	private static final long STATE_TRANSITION_TIME = 20000;
	
	private void computeCommitment() {
		myCommitment = new SDKGZqElement(parameters.getP(), parameters.getG().modPow(myDecryptionKey, parameters.getP()));
		commitmentsMap.put(myself.getIndex(), myCommitment);
		
		state = State.COMMITMENT_COMPUTED;
	}
	
	private void exchangeCommitment(){
		for (int j = 1; j < players.getSize(); j++) {
			if (players.get(j).getIndex() == myself.getIndex()) {
				// dont send the commitments to myself...
				continue;
			}
			
			String[] strValues = { myCommitment.toString() };

			SDKGMessage decryptionShareMessage = new SDKGMessage(
					SDKGMessage.Type.DECRYPTION_VERIFICATION_COMMITMENT,
					sessionId, strValues, context);

			decryptionShareMessage.setFrom(connection.getUser());
			decryptionShareMessage.setTo(players.findPlayer(j));
			connection.sendPacket(decryptionShareMessage);
		}
		state = State.COMMITMENTS_EXCHANGED;
	}
	
	private void finish(){
		UserData.initDecryptionKey(myDecryptionKey);
		state = State.PROTOCOL_EXECUTION_COMPLETED;
	}

	@Override
	public void processPacket(Packet packet) {
		
		SDKGExtension extension = (SDKGExtension) packet
				.getExtension(SDKGExtension.NAMESPACE);
		extension.setFrom(packet.getFrom());
		SDKGMessage msg = new SDKGMessage(extension);

		SDKGPlayer sender = players.findPlayer(msg.getFrom());

		String[] values = msg.getValues();
		int playerIndex = sender.getIndex();

		if (msg.isViCommitmentMessage()) {
			commitmentsMap.put(playerIndex, new SDKGZqElement(parameters.getP(), new BigInteger(values[0])));
		}
	}
	
	private void runProtocoll(){
		switch (state) {
		case INITIAL:
			computeCommitment();
			break;
		case COMMITMENT_COMPUTED:
			exchangeCommitment();
			break;
		case COMMITMENTS_EXCHANGED:
			finish();
			break;
		case PROTOCOL_EXECUTION_COMPLETED:
			break;
		case ERROR:
			break;
		}
	}

	public void execute() {
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				runProtocoll();
			}
		};
		timer.schedule(task, 0, STATE_TRANSITION_TIME);
	}

	public boolean executionFinished() {
		return state == State.PROTOCOL_EXECUTION_COMPLETED;
	}

	public State getCurrentState() {
		return state;
	}
}
