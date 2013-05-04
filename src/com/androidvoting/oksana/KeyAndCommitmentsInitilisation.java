package com.androidvoting.oksana;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;

import android.content.Context;
import android.util.SparseArray;

import com.androidvoting.murati.smartdkg.communication.SDKGExtension;
import com.androidvoting.murati.smartdkg.communication.SDKGMessage;
import com.androidvoting.murati.smartdkg.communication.SDKGProvider;
import com.androidvoting.murati.smartdkg.dkg.SDKGElGamalParameters;
import com.androidvoting.murati.smartdkg.dkg.SDKGPlayer;
import com.androidvoting.murati.smartdkg.dkg.SDKGPlayerList;
import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGLagrange;
import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZq;
import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZqElement;
import com.androidvoting.oksana.application.UserData;

public class KeyAndCommitmentsInitilisation implements PacketListener{
	public KeyAndCommitmentsInitilisation(SDKGElGamalParameters parameters, String sessionId,
			Connection connection, Context context,
			SDKGPlayer myself, SDKGPlayerList players, BigInteger myKeyShare) {
		this.parameters = parameters;
		this.sessionId = sessionId;
		this.connection = connection;
		this.context = context;
		this.myself = myself;
		this.players = players;
		this.myKeyShare = myKeyShare;
		
		ProviderManager.getInstance().addExtensionProvider(SDKGExtension.ELEMENT_NAME,
				SDKGExtension.NAMESPACE, new SDKGProvider(context));
		
		connection.addPacketListener(this, null);
		
		this.state = State.INITIAL;
		
		computeDecryptionKey();
		computeCommitment();
	}
	
	public enum State { 
		INITIAL, DECRYPTION_KEY_AND_COMMITMENT_COMPUTED, COMMITMENTS_EXCHANGED, PROTOCOL_EXECUTION_COMPLETED, ERROR
	}

	private SDKGElGamalParameters parameters;
	
	private String sessionId; // unique id for the current instance of the dkg protocol

	private Connection connection; // for sending messages

	private State state; // current progress of the executed protocol instance

	private Context context; // app context
	
	private SDKGPlayer myself;

	private SDKGPlayerList players;
	
	private BigInteger myKeyShare; // xi
	
	private BigInteger myDecryptionKey; //xi * lambda_i
	
	private SDKGZqElement myCommitment;
	
	public static SparseArray<SDKGZqElement> commitmentsMap = new SparseArray<SDKGZqElement>();
	
	private static final long STATE_TRANSITION_TIME = 20000;
	
	private BigInteger getLagrange(int playerIndex) {
		ArrayList<Integer> indices = new ArrayList<Integer>();
		for (SDKGPlayer player : players) {
			if (player != null){
				indices.add(player.getIndex());
			}
		}
		int j = indices.indexOf(playerIndex);
		SDKGLagrange lagrange = new SDKGLagrange(parameters.getQ());
		return lagrange.getLagrangeCoefficient(j, indices);
	}
	
	private void computeDecryptionKey() {
		myDecryptionKey = getLagrange(myself.getIndex()).multiply(myKeyShare).mod(parameters.getQ());
	}
	
	private void computeCommitment() {
		myCommitment = new SDKGZqElement(parameters.getP(), parameters.getG().modPow(myDecryptionKey, parameters.getP()));
		commitmentsMap.put(myself.getIndex(), myCommitment);
		
		state = State.DECRYPTION_KEY_AND_COMMITMENT_COMPUTED;
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
			computeDecryptionKey();
			computeCommitment();
			break;
		case DECRYPTION_KEY_AND_COMMITMENT_COMPUTED:
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
