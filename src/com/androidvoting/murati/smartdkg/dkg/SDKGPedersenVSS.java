package com.androidvoting.murati.smartdkg.dkg;

import com.androidvoting.murati.smartdkg.communication.SDKGExtension;
import com.androidvoting.murati.smartdkg.communication.SDKGMessage;
import com.androidvoting.murati.smartdkg.communication.SDKGProvider;
import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGLagrange;
import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGPolynom;
import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZq;
import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZqElement;
import com.androidvoting.murati.smartdkg.dkg.commitments.SDKGCikCommitment;
import com.androidvoting.murati.smartdkg.dkg.commitments.SDKGShare;
import com.androidvoting.murati.smartdkg.exceptions.SDKGReconstructionException;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.spongycastle.crypto.params.RSAPrivateCrtKeyParameters;

import android.content.Context;
import android.util.Log;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.SecretKey;

public class SDKGPedersenVSS implements PacketListener {

	private SDKGElGamalParameters parameters; // el gamal threshold parameters (p, q, g, h, t)

	private SecretKey key; // AES key for encryption and decryption

	private RSAPrivateCrtKeyParameters privateKey; // my RSA private key

	//////////////////////////////////////////////////

	private ArrayList<SDKGZqElement> aikCoefficients; // aik coefficients of my polynom f

	private SDKGShare[] myShares; // my own shares sii and sii'

	private SDKGPlayer myself; // instance of the player who is currently running an instance of this protocol

	private SDKGPlayerList playerList; // all players

	private SDKGCikCommitment[] myCikCommitmentsForPlayers;

	private Map<SDKGPlayer, SDKGShare[]> mySharesForPlayersMap;

	private Map<SDKGPlayer, SDKGShare[]> receivedPlayerSharesMap;

	private Map<SDKGPlayer, SDKGCikCommitment[]> receivedPlayerCommitmentsMap;

	private ArrayList<SDKGPlayer> complainerPlayers; // players who complained against me

	private Map<SDKGPlayer, ArrayList<SDKGShare[]>> complaintResponseShares; // needed to verify the response of a player pi who has been complained by another player pj

	//////////////////////////////////////////////////

	private String sessionId; // unique id for the current instance of the dkg protocol

	private Connection connection; // for sending messages

	private State state; // current progress of the executed protocol instance

	private Context context; // app context

	//////////////////////////////////////////////////

	private static final String TAG = "SDKGPedersenVSS";
	
	private static final long STATE_TRANSITION_TIME = 20000; // time delay between two states in milliseconds

	/*
	 * represents a state in the PedersenVSS
	 */
	public enum State {
		INITIAL,
		COMMITMENTS_AND_SHARES_COMPUTED,
		COMMITMENTS_EXCHANGED,
		SHARES_EXCHANGED,
		SHARES_VERIFIED_AND_COMPLAINTS_ISSUED_AND_SENT,
		RETRIEVED_COMPLAINTS_AND_SENT_RESPONSES,
		PROTOCOL_EXECUTION_COMPLETED
	}

	public SDKGPedersenVSS(
			SDKGElGamalParameters parameters,
			SecretKey key,
			RSAPrivateCrtKeyParameters privateKey,
			SDKGPlayer player,
			SDKGPlayerList playerList,
			String sessionId,
			Connection connection,
			Context context) {

		this.parameters = parameters;
		this.key = key;
		this.privateKey = privateKey;

		this.myself = player;
		this.playerList = playerList;
		this.sessionId = sessionId;
		this.connection = connection;

		this.context = context;

		mySharesForPlayersMap = new HashMap<SDKGPlayer, SDKGShare[]>();
		receivedPlayerSharesMap = new HashMap<SDKGPlayer, SDKGShare[]>();
		receivedPlayerCommitmentsMap = new HashMap<SDKGPlayer, SDKGCikCommitment[]>();
		complainerPlayers = new ArrayList<SDKGPlayer>();
		complaintResponseShares = new HashMap<SDKGPlayer, ArrayList<SDKGShare[]>>();

		ProviderManager.getInstance().addExtensionProvider(SDKGExtension.ELEMENT_NAME,
				SDKGExtension.NAMESPACE, new SDKGProvider(context));

		connection.addPacketListener(this, null);

		state = State.INITIAL; // start in initial state

	}

	/*
	 * generates polynomials and private and public commitments
	 */
	private void computeCommitments() {
		if (state != State.INITIAL) {
			Log.i(TAG, "commitments can only be computed in the first state of the protocol");
			return;
		}

		SDKGZq zqGroup = new SDKGZq(parameters.getQ());
		int t = parameters.getT();

		aikCoefficients = zqGroup.getRandomElements(t + 1, new SecureRandom());

		ArrayList<SDKGZqElement> bikCoefficients =
				zqGroup.getRandomElements(t + 1, new SecureRandom());

		SDKGPolynom fi = new SDKGPolynom(t, aikCoefficients);
				
		SDKGPolynom fi_ = new SDKGPolynom(t, bikCoefficients);

		// allocate some space for the public commitments
		myCikCommitmentsForPlayers = new SDKGCikCommitment[t + 1];

		// compute public commitments cik - the cik commitments from player pi are the same for all other players
		for (int k = 0; k <= t; k++) {
			SDKGCikCommitment cik = new SDKGCikCommitment(
					myself.getIndex(),
					k,
					parameters,
					aikCoefficients.get(k),
					bikCoefficients.get(k));

			cik.compute();
			myCikCommitmentsForPlayers[k] = cik;
		}

		// computeShares sij and sij'
		int n = playerList.getSize() - 1;

		for (int j = 1; j <= n; j++) {
			SDKGShare sij = new SDKGShare(myself.getIndex(), playerList.get(j).getIndex(), fi); // sij
			SDKGShare sij_ = new SDKGShare(myself.getIndex(), j, fi_); // sij'

			sij.compute(); sij_.compute(); // sij = fi(j) mod q and sij' = fi'(j) mod q
			SDKGPlayer currentPlayer = playerList.findPlayer(j);

			// compute my own shares sii = fi(i) mod q and sii' = fi'(i) mod q
			if (j == myself.getIndex()) {
				myShares = new SDKGShare[] {sij, sij_};
			} else {
				mySharesForPlayersMap.put(currentPlayer, new SDKGShare[] {sij, sij_});
			}
		}

		state = State.COMMITMENTS_AND_SHARES_COMPUTED;

	}

	private void exchangeCommitments() {
		if (state != State.COMMITMENTS_AND_SHARES_COMPUTED) {
			Log.i(TAG, "no commitments exchanged, need to be computed first");
			return;
		}

		String[] strValues = new String[myCikCommitmentsForPlayers.length + 2];
		strValues[0] = myself.getName();

		for (int i = 0; i < myCikCommitmentsForPlayers.length; i++) {
			strValues[i + 2] = myCikCommitmentsForPlayers[i].toString();
		}

		int n = playerList.getSize() - 1;

		// broadcast Ci0...Cit (dont forget to add the sessionId)
		for (int j = 1; j <= n; j++) {
			if (j == myself.getIndex()) {
				// dont send the commitments to myself...
				continue;
			}

			strValues[1] = playerList.findPlayer(j).getName();

			SDKGMessage commitmentMessage = new SDKGMessage(
					privateKey,
					SDKGMessage.Type.SECRET_SHARE_VERIFICATION_COMMITMENT,
					sessionId,
					strValues,
					context);

			commitmentMessage.setFrom(connection.getUser());
			commitmentMessage.setTo(playerList.findPlayer(j));
			connection.sendPacket(commitmentMessage);
		}

		state = State.COMMITMENTS_EXCHANGED;

	}

	private void exchangeShares() {
		if (state != State.COMMITMENTS_EXCHANGED) {
			Log.i(TAG, "no shares exchanged, first exchange the commitments");
			return;
		}

		for (SDKGPlayer p : mySharesForPlayersMap.keySet()) {
			// dont send the shares to myself
			if (p.equals(myself)) {
				continue;
			}

			String[] strShares = new String[] {
					myself.getName(),
					p.getName(),
					mySharesForPlayersMap.get(p)[0].toString(), // share sij for player Pi
					mySharesForPlayersMap.get(p)[1].toString()  // share sij' for player Pi
			};

			// a share message is always encrypted...
			SDKGMessage shareMessage = new SDKGMessage(
					key,
					privateKey,
					SDKGMessage.Type.SECRET_SHARES,
					sessionId,
					strShares,
					context);

			shareMessage.setFrom(connection.getUser());
			shareMessage.setTo(playerList.findPlayer(p.getIndex()));
			connection.sendPacket(shareMessage);
		}

		state = State.SHARES_EXCHANGED;

	}

	private void verifyShares() {
		if (state != State.SHARES_EXCHANGED) {
			Log.i(TAG, "verification failed since no shares have been exchanged");
			return;
		}

		/*
		 *  verify all received shares...
		 * 
		 *  check whether g^sji * h^sji' = prod(k=0 to t) (Cjk)^i^k mod p (foreach j=1 to n)
		 */
		for (SDKGPlayer p : receivedPlayerSharesMap.keySet()) {
			SDKGShare[] shares = receivedPlayerSharesMap.get(p);
			SDKGCikCommitment[] commitments = receivedPlayerCommitmentsMap.get(p);

			SDKGShareVsCommitmentEquation equation = new SDKGShareVsCommitmentEquation(
					parameters,
					myself.getIndex(),
					shares,
					commitments);

			// equation does not satisfy -> that means that the received shares must be wrong
			if (!equation.satisfy()) {
				// remember to issue a complaint against this player
				p.markComplained();

				for (int j = 1; j < playerList.getSize(); j++) {
					if (j == myself.getIndex()) {
						continue;
					}

					SDKGMessage complaintMessage = new SDKGMessage(
							privateKey,
							SDKGMessage.Type.COMPLAINT,
							sessionId,
							new String[] {myself.getName(), p.getName()}, // issue a complaint against the player pj
							context);

					complaintMessage.setFrom(connection.getUser());
					complaintMessage.setTo(playerList.findPlayer(j));
					connection.sendPacket(complaintMessage);
				}
			}
		}

		state = State.SHARES_VERIFIED_AND_COMPLAINTS_ISSUED_AND_SENT;

	}

	/*
	 * check whether there are complaints against me and if so broadcast responses
	 */
	private void fendComplaints() {
		if (state != State.SHARES_VERIFIED_AND_COMPLAINTS_ISSUED_AND_SENT) {
			Log.i(TAG, "complaints could not be handled since the needed shares have not been verified");
			return;
		}

		// handle complaint for each player who attached blame against me
		if (!complainerPlayers.isEmpty()) {
			for (SDKGPlayer p : complainerPlayers) {
				SDKGShare[] shares = mySharesForPlayersMap.get(p);

				// complaint response message := (COMPLAINT_RESPONSE, Pi, Pj, (Pi, Pj, sij, sij'))
				String[] values = new String[] {
						myself.getName(),
						p.getName(),
						shares[0].toString(),
						shares[1].toString()
				};

				// broadcast each complaint response
				for (int j = 1; j < playerList.getSize(); j++) {
					if (j == myself.getIndex()) {
						continue;
					}

					// create new complaint_response_message and broadcast
					SDKGMessage complaintResponse = new SDKGMessage(
							privateKey,
							SDKGMessage.Type.COMPLAINT_RESPONSE,
							sessionId,
							values,
							context);

					complaintResponse.setFrom(connection.getUser());
					complaintResponse.setTo(playerList.findPlayer(j));
					connection.sendPacket(complaintResponse);
				}
			}
		}

		state = State.RETRIEVED_COMPLAINTS_AND_SENT_RESPONSES;

	}

	private void verifyComplaintResponses() {
		if (state != State.RETRIEVED_COMPLAINTS_AND_SENT_RESPONSES) {
			Log.i(TAG, "complaint responses could not bee verified since there were no response messages");
			return;
		}

		for (SDKGPlayer p : complaintResponseShares.keySet()) {
			// fetch the shares of the player who sent a complaint response
			for (SDKGShare[] shares : complaintResponseShares.get(p)) {
				// fetch cik commitments of player pi
				SDKGCikCommitment[] cikCommitments = receivedPlayerCommitmentsMap.get(p);

				// Verify the complaint response from player pi to player pj for each shares sij and sij'
				SDKGShareVsCommitmentEquation equation = new SDKGShareVsCommitmentEquation(
						parameters,
						shares[0].getSecondIndex(),
						shares,
						cikCommitments);

				// if a certain player got more than t complaints or the equation doesnt match for a sij and sij' -> disqualify player pi
				if (p.getNumComplaints() > parameters.getT()
						|| !equation.satisfy()) {
					p.markDisqualified();
					break;
				} else {
					receivedPlayerSharesMap.put(p, shares); // update the shares of the complained player otherwise he would remain in QUAL with the wrong values
				}
			}
		}

		state = State.PROTOCOL_EXECUTION_COMPLETED;

	}

	protected SDKGZqElement reconstruct(ArrayList<Integer> indices, ArrayList<BigInteger> values) {
		if (indices.size() <= parameters.getT()) {
			throw new SDKGReconstructionException("reconstruction failed due to insufficient number of sampling points");
		}

		SDKGLagrange lagrange = new SDKGLagrange(parameters.getQ());
		SDKGZqElement zj = lagrange.reconstruct(indices, values);
		return zj;
	}

	@Override
	public void processPacket(Packet packet) {
		SDKGExtension extension = (SDKGExtension) packet.getExtension(SDKGExtension.NAMESPACE);
		extension.setFrom(packet.getFrom());
		SDKGMessage msg = new SDKGMessage(extension);

		SDKGPlayer sender = playerList.findPlayer(msg.getFrom());
		int senderIndex = sender.getIndex(); // index of the player who sent the message
		String[] strValues = msg.getValues();

		// cik commitments
		if (msg.isSecretShareCommitmentMessage()) {
			SDKGCikCommitment[] commitments = new SDKGCikCommitment[parameters.getT() + 1];

			// deserialize
			for (int k = 0; k <= parameters.getT(); k++) {
				commitments[k] = new SDKGCikCommitment(
						senderIndex,
						k,
						new BigInteger(strValues[k + 2]));
			}

			// this map holds all cik commitments which have been sent by the other participating players
			receivedPlayerCommitmentsMap.put(sender, commitments);
		}

		// received shares sji, sji' (deserialize)
		else if (msg.isSharesMessage()) {
			SDKGShare sji = new SDKGShare(
					senderIndex,
					myself.getIndex(),
					new BigInteger(strValues[2]));

			SDKGShare sji_ = new SDKGShare(
					senderIndex,
					myself.getIndex(),
					new BigInteger(strValues[3]));

			SDKGShare[] shares = new SDKGShare[] {sji, sji_};

			// store a player object pj with the corresponding shares sji and sji' (index i = my index)
			receivedPlayerSharesMap.put(sender, shares);
		}

		// retrieve all complaint messages
		else if (msg.isComplaintMessage()) {
			// player who got blamed
			SDKGPlayer defendant = playerList.findPlayer(msg.getValues()[1]);

			// complaint against me?
			if (defendant.getIndex() == myself.getIndex()) {
				myself.markComplained(); // im honest so i will mark myself as complained
				complainerPlayers.add(sender); // remember who attached blame against me
			} else {
				// increment complaint counter for the blamed player
				defendant.markComplained(); // increment complaint counter by one for a player pj
			}
		}

		// got a complaint response - retrieve all
		else if (msg.isComplaintResponseMessage()) {
			SDKGPlayer complainer = playerList.findPlayer(msg.getValues()[1]);

			// deserialize
			SDKGShare sij = new SDKGShare(
					sender.getIndex(),
					complainer.getIndex(),
					new BigInteger(msg.getValues()[2]));

			SDKGShare sij_ = new SDKGShare(
					sender.getIndex(),
					complainer.getIndex(),
					new BigInteger(msg.getValues()[3]));

			if (!complaintResponseShares.containsKey(sender)) {
				ArrayList<SDKGShare[]> shareList = new ArrayList<SDKGShare[]>();
				shareList.add(new SDKGShare[] {sij, sij_});
				complaintResponseShares.put(sender, shareList);
			} else {
				complaintResponseShares.get(sender).add(new SDKGShare[] {sij, sij_});
			}
		}
	}

	private void runProtocol() {
		switch (state) {
		case INITIAL:
			computeCommitments();
			break;
		case COMMITMENTS_AND_SHARES_COMPUTED:
			exchangeCommitments();
			break;
		case COMMITMENTS_EXCHANGED:
			exchangeShares();
			break;
		case SHARES_EXCHANGED:
			verifyShares();
			break;
		case SHARES_VERIFIED_AND_COMPLAINTS_ISSUED_AND_SENT:
			fendComplaints();
			break;
		case RETRIEVED_COMPLAINTS_AND_SENT_RESPONSES:
			verifyComplaintResponses();
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
	 * returns the coefficients ai0,...,aik of a player who is currently running an instance of this protocol
	 */
	public ArrayList<SDKGZqElement> getCoefficients() {
		return aikCoefficients;
	}

	/*
	 * sii and sii' - needed to compute xi and xi'
	 */
	public SDKGShare[] getMyShares() {
		return myShares;
	}

	/*
	 * returns all received shares sji and sji' for a player pi
	 */
	public Map<SDKGPlayer, SDKGShare[]> getReceivedShares() {
		return receivedPlayerSharesMap;
	}

	/*
	 * returns all received cik commitments for a player pi
	 */
	public Map<SDKGPlayer, SDKGCikCommitment[]> getReceivedPublicCommitments() {
		return receivedPlayerCommitmentsMap;
	}

	/*
	 * returns true if the execution of this instance is completed
	 */
	public boolean executionFinished() {
		return state == State.PROTOCOL_EXECUTION_COMPLETED;
	}

	/*
	 * returns the current state of this protocol
	 */
	public State getCurrentState() {
		return state;
	}

}
