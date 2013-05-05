package com.androidvoting.oksana.protocol;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.androidvoting.murati.smartdkg.communication.SDKGExtension;
import com.androidvoting.murati.smartdkg.communication.SDKGMessage;
import com.androidvoting.murati.smartdkg.communication.SDKGProvider;
import com.androidvoting.murati.smartdkg.dkg.SDKGElGamalParameters;
import com.androidvoting.murati.smartdkg.dkg.SDKGPlayer;
import com.androidvoting.murati.smartdkg.dkg.SDKGPlayerList;
import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZqElement;
import com.androidvoting.oksana.application.UserData;
import com.androidvoting.oksana.data.DecryptionCommitmentsMap;
import com.androidvoting.oksana.data.DecryptionShare;
import com.androidvoting.oksana.data.DecryptionSharesOfVote;
import com.androidvoting.oksana.data.EncryptedVote;
import com.androidvoting.oksana.data.ValidDecryptionsSet;
import com.androidvoting.oksana.data.VotesDecryptionSharesMap;
import com.androidvoting.oksana.zkp.ZKPDlog;
import com.androidvoting.oksana.zkp.ZKPDlogProof;

public class Decryption implements PacketListener {
	public Decryption(
			String sessionId, Connection connection, Context context,
			SDKGPlayer myself, SDKGPlayerList players,
			SDKGElGamalParameters parameters,
			SparseArray<EncryptedVote> votesMap,
			DecryptionCommitmentsMap commitmentsMap) {
		this.sessionId = sessionId;
		this.connection = connection;
		this.context = context;
		this.myself = myself;
		this.players = players;
		this.votesMap = votesMap;
		this.commitmentsMap = commitmentsMap;
		
		this.decryptionSharesMap = new VotesDecryptionSharesMap();
		
		ProviderManager.getInstance().addExtensionProvider(SDKGExtension.ELEMENT_NAME,
				SDKGExtension.NAMESPACE, new SDKGProvider(context));
		
		connection.addPacketListener(this, null);
				
		myDecryptionKey = UserData.getDecryptionKey();

		this.g = new SDKGZqElement(parameters.getP(), parameters.getG());
		this.q = parameters.getQ();
		
		state = State.INITIAL;

		this.computeCommitment();
	}

	public enum State { // TODO: complains
		INITIAL, DECRYPTION_SHARES_COMPUTED, DECRYPTION_SHARES_EXCHANGED, PROTOCOL_EXECUTION_COMPLETED, ERROR
	}
	
	private String errorMessage = "";

	private String tag = "DECRYPTION";

	private static final long STATE_TRANSITION_TIME = 20000;

	private State state;

	private String sessionId; // unique id for the current instance of the dkg
								// protocol

	private Connection connection; // for sending messages

	private Context context; // app context

	private SDKGPlayer myself;

	private SDKGPlayerList players;

	private DecryptionCommitmentsMap commitmentsMap; // [playerId =>
														// commitment

	private SparseArray<EncryptedVote> votesMap; // [voteId] => vote

	private VotesDecryptionSharesMap decryptionSharesMap;

	private SparseArray<DecryptionShare> myDecryptionShares = new SparseArray<DecryptionShare>(); // [voteId] =>
																// decryption
																// share

	private BigInteger myDecryptionKey;

	private SDKGZqElement g;
	
	private BigInteger q;

	private SDKGZqElement myCommitment;

	ArrayList<SDKGZqElement> decryptedVotes = new ArrayList<SDKGZqElement>();

	EncryptedVote getVote(int voteId) {
		return this.votesMap.get(voteId);
	}

	private DecryptionShare getDecryptionShare(EncryptedVote vote)
			throws NoSuchAlgorithmException {
		SDKGZqElement partialDecryption = this.getPartialDecryption(vote);
		return new DecryptionShare(partialDecryption, this.getProof(vote,
				partialDecryption), this.myself.getIndex(), vote.getIndex()); // TODO
	}

	public void computeMyDecryptionShares() throws NoSuchAlgorithmException {
//		if (state != State.INITIAL) {
//			return;
//		}

		for (int i = 0; i < votesMap.size(); i++) {
			myDecryptionShares.put(i, getDecryptionShare(votesMap.get(i)));
		}
		state = State.DECRYPTION_SHARES_COMPUTED;
	}

	private void exchangePartialDecryptions() throws NoSuchAlgorithmException {
		if (state != State.DECRYPTION_SHARES_COMPUTED) {
			return;
		}
		for (int i = 0; i < myDecryptionShares.size(); i++) {
			DecryptionShare dS = myDecryptionShares.get(i);
			String[] strValues = { dS.getPartialDecryption().toString(),
					dS.getProof().getE().toString(),
					dS.getProof().getY().toString(),
					String.valueOf(dS.getVoteIndex()) };

			for (int j = 1; j < players.getSize(); j++) {
				if (players.get(j).getIndex() == myself.getIndex()) {
					// dont send the commitments to myself...
					continue;
				}

				SDKGMessage decryptionShareMessage = new SDKGMessage(
						SDKGMessage.Type.DECRYPTION_SHARE,
						sessionId, strValues, context);

				decryptionShareMessage.setFrom(connection.getUser());
				decryptionShareMessage.setTo(players.findPlayer(j));
				connection.sendPacket(decryptionShareMessage);
			}
		}
		state = State.DECRYPTION_SHARES_EXCHANGED;
	}

	ZKPDlogProof getProof(EncryptedVote vote) throws NoSuchAlgorithmException {
		SDKGZqElement partialDecryption = this.getPartialDecryption(vote);
		return this.getProof(vote, partialDecryption);
	}

	private ZKPDlogProof getProof(EncryptedVote vote,
			SDKGZqElement partialDecryption) throws NoSuchAlgorithmException {
		ZKPDlog zkp = new ZKPDlog(g, vote.getA(), getCommitment(),
				partialDecryption);
		return zkp.prove(myDecryptionKey);
	}

	/**
	 * Gets a commitment for future decryption verification
	 * 
	 * @return
	 */
	SDKGZqElement getCommitment() {
		if (myCommitment == null && commitmentsMap.get(myself.getIndex()) == null)
			this.computeCommitment();
		else
			myCommitment = commitmentsMap.get(myself.getIndex());
		return this.myCommitment;
	}

	private void computeCommitment() {
		if (state != State.INITIAL)
			return;
		if(commitmentsMap.get(myself.getIndex()) == null)
				this.myCommitment = g.modPow(myDecryptionKey);
		else
			myCommitment = commitmentsMap.get(myself.getIndex());
	}

	SDKGZqElement getPartialDecryption(EncryptedVote vote) {
		return vote.getA().modPow(myDecryptionKey);
	}

	/**
	 * Verifies a decryption share provided by party j for a selected vote
	 * 
	 * @param vote
	 * @param dShare
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	private boolean verifyProof(EncryptedVote vote, DecryptionShare dShare)
			throws NoSuchAlgorithmException {
		ZKPDlog zpk_j = new ZKPDlog(g, vote.getA(), this.getCommitment(dShare
				.getPlayerIndex()), dShare.getPartialDecryption());
//		try{
//			zpk_j.verify(dShare.getProof());
//		}
//		catch(Exception e){
//			outputException(e);
//			if(g == null)
//				errorMessage = "g is null";
//			else if(this.getCommitment(dShare
//				.getPlayerIndex()) == null)
//				errorMessage = "com. is null";
//			else if(dShare.getPartialDecryption() == null){
//				errorMessage = "pd is null";
//			}
//		}
//		return true;
		return zpk_j.verify(dShare.getProof());
	}

	private SDKGZqElement getCommitment(int playerId) {
		return this.commitmentsMap.get(playerId);
	}

	/**
	 * Gets a set of valid partial decryptions for one vote
	 * 
	 * @param shares
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	ValidDecryptionsSet getValidPartialDecryptions(DecryptionSharesOfVote shares)
			throws NoSuchAlgorithmException {
		ValidDecryptionsSet validDecryptions = new ValidDecryptionsSet(g.getOrder(), q);
		ArrayList<DecryptionShare> s = shares.getDecryptionShares();
		
		for (DecryptionShare ds : s) {
			if (this.verifyProof(this.getVote(shares.getVoteId()), ds))
				validDecryptions.add(ds.getPartialDecryption(), ds.getPlayerIndex());
		
		}
		// adding own share
		validDecryptions.add(myDecryptionShares.get(shares.getVoteId())
				.getPartialDecryption(), myself.getIndex());
		return validDecryptions;
	}

	/**
	 * Reconstructs the value a^x from partial decryptions
	 * 
	 * @param validDecryptions
	 * @return
	 */
	private SDKGZqElement reconstruct(ValidDecryptionsSet validDecryptions) {
		return validDecryptions.reconstruct();
	}

	private void addDecryptionShare(DecryptionShare share) {
		this.decryptionSharesMap.addDecryptionShare(share);
	}

	/**
	 * Returns the decrypted vote
	 * 
	 * @param vote
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	private SDKGZqElement decryptVote(EncryptedVote vote)
			throws NoSuchAlgorithmException {
		DecryptionSharesOfVote allShares = this.getOtherDecryptionShares(vote);
		ValidDecryptionsSet validShares = this.getValidPartialDecryptions(allShares);

//		if (validShares.size() < parameters.getT()) {
//			Log.e(tag, "Less than t valid shares");
//			return null;
//		}
//
		return this.reconstruct(validShares).getMultiplicativeInverse()
				.mul(vote.getB());
	}

	private DecryptionSharesOfVote getOtherDecryptionShares(EncryptedVote vote)
			throws NoSuchAlgorithmException {
		return this.decryptionSharesMap.get(vote.getIndex());
	}

	int count = 0;
	@Override
	public void processPacket(Packet packet) {
		SDKGExtension extension = (SDKGExtension) packet
				.getExtension(SDKGExtension.NAMESPACE);
		extension.setFrom(packet.getFrom());
		SDKGMessage msg = new SDKGMessage(extension);

		SDKGPlayer sender = players.findPlayer(msg.getFrom());

		String[] values = msg.getValues();
		int playerIndex = sender.getIndex();

		/*
		 * Process decryption share Stored as a String array: [0] => partial
		 * Decryption [1] => ZKP, e [2] => ZKP, y [3] => vote index
		 */
		if (msg.isDecryptionShareMessage()) {
			count++;

			SDKGZqElement partialDecryption = g.getGroup()
					.fromString(values[0]);
			ZKPDlogProof proof = new ZKPDlogProof(new BigInteger(values[1]),
					new BigInteger(values[2]));
			int voteIndex = Integer.parseInt(values[3]);
			addDecryptionShare(new DecryptionShare(partialDecryption, proof,
					playerIndex, voteIndex));
		}
	}

	private void decrypt() throws NoSuchAlgorithmException {
		if (state != State.DECRYPTION_SHARES_EXCHANGED) {
			Log.e(tag, "Exchange shares first!");
			state = State.ERROR;
			errorMessage = "Exchange shares first!";
			return; // Exchange shares first!
		}

		if (decryptionSharesMap.countNumVotes() != votesMap.size()) {
			state = State.ERROR;
			errorMessage = "Not all votes!" + decryptionSharesMap.countNumVotes() + " " + votesMap.size() + " " + count;
			Log.e(tag, "Not all votes!");
			return; // Not all votes!
		}

		for (int i = 0; i < votesMap.size(); i++) {
			SDKGZqElement vote = decryptVote(getVote(i));

			if (vote == null) {
				Log.e(tag, "Some vote cannot be reconstructed!");
				return; // some vote cannot be reconstructed!
			}
			decryptedVotes.add(vote);
		}
		state = State.PROTOCOL_EXECUTION_COMPLETED;
	}

	private void runProtocoll() throws NoSuchAlgorithmException {
		switch (state) {
		case INITIAL:
			computeMyDecryptionShares();
			break;
		case DECRYPTION_SHARES_COMPUTED:
			exchangePartialDecryptions();
			break;
		case DECRYPTION_SHARES_EXCHANGED:
			decrypt();
			break;
		case PROTOCOL_EXECUTION_COMPLETED:
			break;
		case ERROR:
			break;
		}
	}

	public void execute() {
		Log.e(tag, state.toString());
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				try {
					runProtocoll();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		timer.schedule(task, 0, STATE_TRANSITION_TIME);
	}

	public ArrayList<SDKGZqElement> getDecryptedVotes() {
		return decryptedVotes;
	}

	public boolean executionFinished() {
		return this.state == State.PROTOCOL_EXECUTION_COMPLETED;
	}
	
	public State getCurrentState() {
		return this.state;
	}
	
	public String getErrorMessage() {
		return this.errorMessage;
	}
}
