package com.murati.smartdkg.dkg;

import com.murati.smartdkg.communication.SDKGExtension;
import com.murati.smartdkg.communication.SDKGMessage;
import com.murati.smartdkg.communication.SDKGProvider;
import com.murati.smartdkg.dkg.arithm.SDKGZqElement;
import com.murati.smartdkg.dkg.commitments.SDKGAikCommitment;
import com.murati.smartdkg.dkg.commitments.SDKGCikCommitment;
import com.murati.smartdkg.dkg.commitments.SDKGShare;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.spongycastle.crypto.params.RSAPrivateCrtKeyParameters;

import android.content.Context;
import android.util.Log;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class SDKGFeldmanVSS implements PacketListener {

	private SDKGPedersenVSS pedersenVSS; // an instance of the pedersenVSS

	private SDKGElGamalParameters parameters; // threshold el gamal parameters (p, q, g, h, t)

	private RSAPrivateCrtKeyParameters privateKey; // rsa private key

	//////////////////////////////////////////////////

	private SDKGPlayer myself; // instance of the player who is currently running this instance of feldmanVSS

	private SDKGAikCommitment[] myAikCommitmentsForPlayers; // my aik commitments

	private SDKGPlayerList qualifiedPlayersList; // holds all players who runned pedersenVSS successfully

	private Map<SDKGPlayer, SDKGAikCommitment[]> receivedPlayerAikCommitmentsMap; // all received aik commitments

	private SDKGPlayerList disqualifiedPlayersList; // holds all players who have been disqualified during the execution of this protocol

	private Map<SDKGPlayer, ArrayList<SDKGShare[]>> complaintSharesMap; // needed to verify the response of a player pj who has been complained by another player pi

	private Map<SDKGPlayer, SDKGShare[]> reconstructionSharesMap; // holds all shares sji of a disqualified player

	private List<SDKGZqElement> reconstructedSecretsList; // holds all reconstructed secrets zj for each missing player zj

	private List<SDKGPlayer> complainedPlayers; // holds all players who have been complained by me

	private BigInteger jointPublicKey = BigInteger.ZERO; // jointly generated public key y

	//////////////////////////////////////////////////

	private String sessionId; // unique id for the current instance of the overlying dkg protocol

	private Connection connection; // for sending messages

	private State state; // current state

	private Context context; // app context

	//////////////////////////////////////////////////

	private static final String TAG = "SDKGFeldmanVSS";

	private static final long STATE_TRANSITION_TIME = 20000; // time delay between two states in milliseconds

	/*
	 * represents a state in the FeldmanVSS
	 */
	public enum State {
		INITIAL,
		COMMITMENTS_COMPUTED,
		COMMITMENTS_EXCHANGED,
		COMMITMENTS_VERIFIED_AND_COMPLAINTS_ISSUED,
		COMPLAINTS_SENT,
		COMPLAINTS_VERIFIED,
		RECONSTRUCTION_REQUEST_SENT,
		MISSING_PUBLICKEY_SHARES_RECONSTRUCTED,
		PROTOCOL_EXECUTION_COMPLETED
	}

	public SDKGFeldmanVSS(
			SDKGPedersenVSS pedersenVSS,
			SDKGElGamalParameters parameters,
			RSAPrivateCrtKeyParameters privateKey,
			SDKGPlayer player,
			SDKGPlayerList qualifiedPlayersList,
			String sessionId,
			Connection connection,
			Context context) {

		this.pedersenVSS = pedersenVSS;
		this.parameters = parameters;
		this.privateKey = privateKey;

		this.myself = player;
		this.qualifiedPlayersList = qualifiedPlayersList;

		this.sessionId = sessionId;
		this.connection = connection;
		this.context = context;

		disqualifiedPlayersList = new SDKGPlayerList();
		receivedPlayerAikCommitmentsMap = new HashMap<SDKGPlayer, SDKGAikCommitment[]>();
		complaintSharesMap = new HashMap<SDKGPlayer, ArrayList<SDKGShare[]>>();

		reconstructedSecretsList = new ArrayList<SDKGZqElement>();
		reconstructionSharesMap = new HashMap<SDKGPlayer, SDKGShare[]>();

		complainedPlayers = new ArrayList<SDKGPlayer>();

		ProviderManager.getInstance().addExtensionProvider(SDKGExtension.ELEMENT_NAME,
				SDKGExtension.NAMESPACE, new SDKGProvider(context));

		connection.addPacketListener(this, null);

		state = State.INITIAL;

	}

	private void computeCommitments() {
		if (state != State.INITIAL) {
			Log.i(TAG, "commitments can only be computed in the first state of the protocol");
			return;
		}

		// compute Ai0...Aik
		int t = parameters.getT();
		myAikCommitmentsForPlayers = new SDKGAikCommitment[t + 1];

		for (int k = 0; k <= t; k++) {
			SDKGAikCommitment aikCommitment = new SDKGAikCommitment(
					myself.getIndex(),
					k,
					parameters,
					pedersenVSS.getCoefficients().get(k));

			aikCommitment.compute();
			myAikCommitmentsForPlayers[k] = aikCommitment;
		}

		state = State.COMMITMENTS_COMPUTED;

	}

	private void exchangeCommitments() {
		if (state != State.COMMITMENTS_COMPUTED) {
			Log.i(TAG, "no commitments have been computed before");
			return;
		}

		String[] strValues = new String[myAikCommitmentsForPlayers.length + 2];
		strValues[0] = myself.getName();

		for (int i = 0; i < myAikCommitmentsForPlayers.length; i++) {
			strValues[i + 2] = myAikCommitmentsForPlayers[i].toString();
		}

		int n = qualifiedPlayersList.getSize() - 1;

		// broadcast commitments Ai0,...,Ait
		for (int j = 1; j <= n; j++) {
			if (j == myself.getIndex()
					|| qualifiedPlayersList.findPlayer(j) == null) {
				continue;
			}

			strValues[1] = qualifiedPlayersList.findPlayer(j).getName();

			SDKGMessage commitmentMessage = new SDKGMessage(
					privateKey,
					SDKGMessage.Type.PUBLIC_SHARE_VERIFICATION_COMMITMENT,
					sessionId,
					strValues,
					context);

			commitmentMessage.setFrom(connection.getUser());
			commitmentMessage.setTo(qualifiedPlayersList.findPlayer(j));
			connection.sendPacket(commitmentMessage);
		}

		state = State.COMMITMENTS_EXCHANGED;

	}

	private void verifyCommitments() {
		if (state != State.COMMITMENTS_EXCHANGED) {
			Log.i(TAG, "verification failed since no commitments have been exchanged");
			return;
		}

		int n = qualifiedPlayersList.getSize() - 1;

		// check whether g^sji = prod(k=0 to t) (Ajk)^i^k mod p for each player pj in QUAL - broadcast complaints if needed
		for (int j = 1; j <= n; j++) {
			if (j == myself.getIndex()
					|| qualifiedPlayersList.findPlayer(j) == null) {
				continue;
			}

			SDKGPlayer player = qualifiedPlayersList.findPlayer(j);
			SDKGShare[] shares = pedersenVSS.getReceivedShares().get(player);

			SDKGShareVsCommitmentEquation equation = new SDKGShareVsCommitmentEquation(
					parameters,
					myself.getIndex(),
					shares,
					receivedPlayerAikCommitmentsMap.get(player));

			// if it doesnt fit -> issue a complaint against player pj
			if (!equation.satisfy()) {
				// increment complaint counter for player pj
				player.markComplained();
				complainedPlayers.add(player);
			}
		}

		state = State.COMMITMENTS_VERIFIED_AND_COMPLAINTS_ISSUED;

	}

	private void broadcastComplaint() {
		if (state != State.COMMITMENTS_VERIFIED_AND_COMPLAINTS_ISSUED) {
			Log.i(TAG, "since no commitments have been verified a broadcast of complaint messages will fail");
			return;
		}

		// a complainer pi has to reveal the shares sji and sji' which dont satisfy the equation - message := (PUBLIC_SHARE_COMPLAINT, Pi, Pj, (Pi, Pj, sji, sji'))
		for (SDKGPlayer player : complainedPlayers) {
			SDKGShare[] shares = pedersenVSS.getReceivedShares().get(player);

			String[] values = new String[] {
					myself.getName(),
					player.getName(),
					shares[0].toString(), // sji
					shares[1].toString()  // sji'
			};

			int n = qualifiedPlayersList.getSize() - 1;

			// broadcast a complaint against pj by revealing the shares sji and sji' he sent to me
			for (int m = 1; m <= n; m++) {
				if (m == myself.getIndex()
						|| qualifiedPlayersList.findPlayer(m) == null) {
					continue;
				}

				SDKGMessage publicShareComplaintMessage = new SDKGMessage(
						privateKey,
						SDKGMessage.Type.PUBLIC_SHARE_COMPLAINT,
						sessionId,
						values,
						context);

				publicShareComplaintMessage.setFrom(connection.getUser());
				publicShareComplaintMessage.setTo(qualifiedPlayersList.findPlayer(m));
				connection.sendPacket(publicShareComplaintMessage);
			}
		}

		state = State.COMPLAINTS_SENT;

	}

	private void verifyComplaints() {
		if (state != State.COMPLAINTS_SENT) {
			Log.i(TAG, "complaint verification not possible since no complaints have been received");
			return;
		}

		// check whether a complaint from player pi against a player pj is valid (check for sji and sji')
		// a valid complaint satisfies equation (4) but fails to satisfy equation (5)
		for (SDKGPlayer p : complaintSharesMap.keySet()) {
			// fetch aik commitments of player pj
			SDKGAikCommitment[] aikCommitments = receivedPlayerAikCommitmentsMap.get(p);

			// fetch shares
			for (SDKGShare[] shares : complaintSharesMap.get(p)) {

				// Verify the complaint from player pi against player pj
				SDKGShareVsCommitmentEquation equation = new SDKGShareVsCommitmentEquation(
						parameters,
						shares[0].getSecondIndex(),
						shares,
						aikCommitments);

				// if the equation doesnt match for a sji and sji' -> check also equation (4) and then disqualify player pj
				if (!equation.satisfy()) {
					// fetch cik commitments of player pj
					SDKGCikCommitment[] cikCommitments = pedersenVSS.getReceivedPublicCommitments().get(p);

					// check if the shares of pj which have been revealed by pi match equation (4)
					SDKGShareVsCommitmentEquation equation4 = new SDKGShareVsCommitmentEquation(
							parameters,
							shares[0].getSecondIndex(),
							shares,
							cikCommitments);

					// verify
					if (equation4.satisfy()) {
						// disqualify pj
						p.markDisqualified();
						disqualifiedPlayersList.add(p);
						receivedPlayerAikCommitmentsMap.remove(p); // remove the faulty commitments received by the cheater otherwise we get an invalid public key

						// store the shares of pj - we need them to reconstruct zj
						SDKGShare[] sjiShares = new SDKGShare[qualifiedPlayersList.size() + 1];
						sjiShares[myself.getIndex()] = pedersenVSS.getReceivedShares().get(p)[0];
						reconstructionSharesMap.put(p, sjiShares);
						break;
					} else {
						// TODO: what if the shares dont match equation (4) as well ???
					}
				}
			}
		}

		state = State.COMPLAINTS_VERIFIED;

	}

	private void broadcastReconstructionRequest() {
		if (state != State.COMPLAINTS_VERIFIED) {
			Log.i(TAG, "no complaints have been verified for this reason reconstruction requests cannot be broadcasted");
			return;
		}

		// broadcast a reconstruction request here - but first remove all disqualified players from QUAL
		qualifiedPlayersList.removeDisqualifiedPlayers();

		// broadcast a reconstruction request for each disqualified player
		for (SDKGPlayer p : disqualifiedPlayersList) {
			// broadcast reconstruction message to all honest players
			for (SDKGPlayer q : qualifiedPlayersList) {
				if (p == null || q == null || q.equals(myself)) {
					continue;
				}

				String[] values = new String[] {
						myself.getName(),
						p.getName(),
						pedersenVSS.getReceivedShares().get(p)[0].toString(),
						pedersenVSS.getReceivedShares().get(p)[1].toString()
				};

				SDKGMessage reconstructionMessage = new SDKGMessage(
						privateKey,
						SDKGMessage.Type.RECONSTRUCTION_REQUEST,
						sessionId,
						values,
						context);

				reconstructionMessage.setFrom(connection.getUser());
				reconstructionMessage.setTo(qualifiedPlayersList.findPlayer(q));
				connection.sendPacket(reconstructionMessage);
			}
		}

		state = State.RECONSTRUCTION_REQUEST_SENT;

	}

	private void reconstruct() {
		if (state != State.RECONSTRUCTION_REQUEST_SENT) {
			Log.i(TAG, "reconstruction of a secret key share not possible since no reconstruction request received");
			return;
		}

		// run reconstruction phase of PedersenVSS to gain  the missing public key share of a player pj
		ArrayList<Integer> indices = new ArrayList<Integer>();
		ArrayList<BigInteger> values = new ArrayList<BigInteger>();

		for (SDKGPlayer p : reconstructionSharesMap.keySet()) {
			int numShares = reconstructionSharesMap.get(p).length;

			for (int k = 1; k < numShares; k++) {
				if (reconstructionSharesMap.get(p)[k] != null) {
					indices.add(k);
					values.add(reconstructionSharesMap.get(p)[k].getValue());
				}
			}
			SDKGZqElement secret = pedersenVSS.reconstruct(indices, values);
			reconstructedSecretsList.add(secret);
		}

		state = State.MISSING_PUBLICKEY_SHARES_RECONSTRUCTED;

	}

	/*
	 * returns the public key y = prod(j in QUAL) yj mod p
	 * 
	 * yj = g^zj mod p
	 */
	private void computePublicKey() {
		if (state != State.MISSING_PUBLICKEY_SHARES_RECONSTRUCTED) {
			Log.i(TAG, "computation of public key not possible since no public key shares reconstructed");
			return;
		}

		BigInteger yi = myAikCommitmentsForPlayers[0].getValue().mod(parameters.getP()); // yi = g^zi mod p = Ai0 (my public key share)
		jointPublicKey = yi;

		// first use all reconstructed secrets
		for (SDKGZqElement zj : reconstructedSecretsList) {
			jointPublicKey = jointPublicKey.multiply(parameters.getG().modPow(zj.getValue(), parameters.getP()));
		}

		// next get the public key shares for each player in qual
		for (SDKGPlayer p : qualifiedPlayersList) {
			if (p != null && !p.equals(myself)) {
				yi = receivedPlayerAikCommitmentsMap.get(p)[0].getValue().mod(parameters.getP());
				jointPublicKey = jointPublicKey.multiply(yi);
			}
		}
		jointPublicKey = jointPublicKey.mod(parameters.getP());

		state = State.PROTOCOL_EXECUTION_COMPLETED;

	}


	@Override
	public void processPacket(Packet packet) {
		SDKGExtension extension = (SDKGExtension) packet.getExtension(SDKGExtension.NAMESPACE);
		extension.setFrom(packet.getFrom());
		SDKGMessage msg = new SDKGMessage(extension);

		SDKGPlayer sender = qualifiedPlayersList.findPlayer(msg.getFrom());
		int senderIndex = sender.getIndex(); // index of the player who sent the message
		String[] values = msg.getValues();

		// message with aik commitments data
		if (msg.isPublicShareCommitmentMessage()) {
			SDKGAikCommitment[] commitments = new SDKGAikCommitment[parameters.getT() + 1];

			for (int k = 0; k <= parameters.getT(); k++) {
				// deserialize
				commitments[k] = new SDKGAikCommitment(
						senderIndex,
						k,
						new BigInteger(values[k + 2]));
			}

			// this map holds all aik commitments which have been sent by the other participating players
			receivedPlayerAikCommitmentsMap.put(sender, commitments);
		}

		// public share complaint message (contains sji and sji')
		else if (msg.isPublicShareComplaintMessage()) {
			SDKGPlayer defendant = qualifiedPlayersList.findPlayer(values[1]);

			// deserialize
			SDKGShare sji = new SDKGShare(
					defendant.getIndex(),
					senderIndex,
					new BigInteger(values[2]));

			SDKGShare sji_ = new SDKGShare(
					defendant.getIndex(),
					senderIndex,
					new BigInteger(values[3]));

			if (!complaintSharesMap.containsKey(defendant)) {
				ArrayList<SDKGShare[]> shareList = new ArrayList<SDKGShare[]>();
				shareList.add(new SDKGShare[] {sji, sji_});
				complaintSharesMap.put(defendant, shareList);
			} else {
				complaintSharesMap.get(defendant).add(new SDKGShare[] {sji, sji_});
			}
		}

		// reconstruction message
		else if (msg.isReconstructionRequestMessage()) {
			SDKGPlayer missingPlayer = disqualifiedPlayersList.findPlayer(msg.getValues()[1]);

			// fetch share
			SDKGShare sij = new SDKGShare(
					missingPlayer.getIndex(),
					senderIndex,
					new BigInteger(values[2]));

			reconstructionSharesMap.get(missingPlayer)[senderIndex] = sij;
		}
	}

	private void runProtocol() {
		switch (state) {
		case INITIAL:
			computeCommitments();
			break;
		case COMMITMENTS_COMPUTED:
			exchangeCommitments();
			break;
		case COMMITMENTS_EXCHANGED:
			verifyCommitments();
			break;
		case COMMITMENTS_VERIFIED_AND_COMPLAINTS_ISSUED:
			broadcastComplaint();
			break;
		case COMPLAINTS_SENT:
			verifyComplaints();
			break;
		case COMPLAINTS_VERIFIED:
			broadcastReconstructionRequest();
			break;
		case RECONSTRUCTION_REQUEST_SENT:
			reconstruct();
			break;
		case MISSING_PUBLICKEY_SHARES_RECONSTRUCTED:
			computePublicKey();
			break;
		case PROTOCOL_EXECUTION_COMPLETED:
			break;
		}
	}

	public void execute() {
		state = State.INITIAL; // start always in inital state!
		final Timer timer = new Timer();

		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				if (state == State.PROTOCOL_EXECUTION_COMPLETED) {
					timer.cancel();
				}
				runProtocol();
			}
		};

		timer.schedule(task, 0, STATE_TRANSITION_TIME); // call next action for some certain time delay

	}

	/*
	 * returns true if the execution of this instance is completed
	 */
	public boolean executionFinished() {
		return state == State.PROTOCOL_EXECUTION_COMPLETED;
	}

	/*
	 * returns the jointly generated public key y
	 */
	public BigInteger getPublicKey() {
		return jointPublicKey;
	}

	/*
	 * returns the current state of this protocol
	 */
	public State getCurrentState() {
		return state;
	}

}
