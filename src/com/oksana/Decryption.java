package com.oksana;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

import android.util.SparseArray;

import com.murati.smartdkg.dkg.SDKGElGamalParameters;
import com.murati.smartdkg.dkg.SDKGPlayer;
import com.murati.smartdkg.dkg.SDKGPlayerList;
import com.murati.smartdkg.dkg.arithm.SDKGLagrange;
import com.murati.smartdkg.dkg.arithm.SDKGZqElement;

//Party in election protocoll

public class Decryption implements PacketListener {
	public Decryption(SDKGPlayer myself, SDKGPlayerList players,
			SDKGElGamalParameters parameters,
			SparseArray<SDKGZqElement> commitmentsMap,
			SparseArray<EncryptedVote> votesMap, BigInteger xi) {
		this.myself = myself;
		this.players = players;
		this.parameters = parameters;
		this.commitmentsMap = commitmentsMap;
		this.votesMap = votesMap;
		this.myDecryptionKey = xi.multiply(this.getLagrange()).mod(
				parameters.getQ());

		this.g = new SDKGZqElement(parameters.getP(), parameters.getG());

		this.computeCommitment();
	}

	public enum State { // TODO: complains
		INITIAL, DECRYPTION_SHARES_COMPUTED, DECRYPTION_SHARES_EXCHANGED, PROTOCOL_EXECUTION_COMPLETED
	}
	
	private static final long STATE_TRANSITION_TIME = 20000;

	private State state;

	private SDKGPlayer myself;

	private SDKGPlayerList players;

	private SDKGElGamalParameters parameters;

	private SparseArray<SDKGZqElement> commitmentsMap; // [playerId =>
														// commitment

	private SparseArray<EncryptedVote> votesMap; // [voteId] => vote

	private VotesDecryptionSharesMap decryptionSharesMap;

	private SparseArray<DecryptionShare> myDecryptionShares; // [voteId] =>
																// decryption
																// share

	private BigInteger myDecryptionKey;

	private SDKGZqElement g;

	private SDKGZqElement myCommitment;
	
	ArrayList<SDKGZqElement> decryptedVotes = new ArrayList<SDKGZqElement>();

	private BigInteger getLagrange() {
		Integer j = myself.getIndex();
		ArrayList<Integer> indices = new ArrayList<Integer>();
		for (SDKGPlayer player : players) {
			indices.add(player.getIndex());
		}
		SDKGLagrange lagrange = new SDKGLagrange(parameters.getQ());
		return lagrange.getLagrangeCoefficient(j, indices);
	}

	EncryptedVote getVote(int voteId) {
		return this.votesMap.get(voteId);
	}

	private DecryptionShare getDecryptionShare(EncryptedVote vote)
			throws NoSuchAlgorithmException {
		SDKGZqElement partialDecryption = this.getPartialDecryption(vote);
		return new DecryptionShare(partialDecryption, this.getProof(vote,
				partialDecryption), this.myself.getIndex(), vote.getIndex()); // TODO
	}

	private void computeMyDecryptionShares() throws NoSuchAlgorithmException {
		if (state != State.INITIAL) {
			return;
		}

		for (int i = 0; i < votesMap.size(); i++) {
			myDecryptionShares.append(i, getDecryptionShare(votesMap.get(i)));
		}
		state = State.DECRYPTION_SHARES_COMPUTED;
	}

	private void exchangePartialDecryptions(EncryptedVote vote)
			throws NoSuchAlgorithmException {
		if (state != State.DECRYPTION_SHARES_COMPUTED) {
			return;
		}
		for (int i = 0; i < myDecryptionShares.size(); i++) {
			DecryptionShare dS = myDecryptionShares.get(i);
			String[] strValues = { dS.getPartialDecryption().toString(),
					dS.getProof().getE().toString(),
					dS.getProof().getY().toString(),
					String.valueOf(dS.getVoteIndex()) };

			// TODO: send the message
		}

		state = State.DECRYPTION_SHARES_EXCHANGED;
	}
	
	private void exchangeAllPartialDecryptions() throws NoSuchAlgorithmException{
		for(int i = 0; i < votesMap.size(); i++) {
			exchangePartialDecryptions(getVote(i));
		}
	}

	ZKPDlogProof getProof(EncryptedVote vote) throws NoSuchAlgorithmException {
		SDKGZqElement partialDecryption = this.getPartialDecryption(vote);
		return this.getProof(vote, partialDecryption);
	}

	private ZKPDlogProof getProof(EncryptedVote vote,
			SDKGZqElement partialDecryption) throws NoSuchAlgorithmException {
		ZKPDlog zkp = new ZKPDlog(g, vote.getA(), myCommitment,
				partialDecryption);
		return zkp.prove(myDecryptionKey);
	}

	/**
	 * Gets a commitment for future decryption verification
	 * 
	 * @return
	 */
	SDKGZqElement getCommitment() {
		if (myCommitment == null)
			this.computeCommitment();
		return this.myCommitment;
	}

	private void computeCommitment() {
		this.myCommitment = g.modPow(myDecryptionKey);
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
	ArrayList<SDKGZqElement> getValidPD(DecryptionSharesOfVote shares)
			throws NoSuchAlgorithmException {
		ArrayList<SDKGZqElement> validDecryptions = new ArrayList<SDKGZqElement>();
		for (DecryptionShare ds : shares) {
			if (this.verifyProof(this.getVote(shares.getVoteId()), ds))
				validDecryptions.add(ds.getPartialDecryption());
		}
		// adding own share
		validDecryptions.add(myDecryptionShares.get(shares.getVoteId())
				.getPartialDecryption());
		return validDecryptions;
	}

	/**
	 * Reconstructs the value a^x from partial decryptions
	 * 
	 * @param validDecryptions
	 * @return
	 */
	private SDKGZqElement reconstruct(ArrayList<SDKGZqElement> validDecryptions) {
		SDKGZqElement y = g.getOneElement();
		for (SDKGZqElement yi : validDecryptions) {
			y = y.mul(yi);
		}
		return y;
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
		ArrayList<SDKGZqElement> validShares = this.getValidPD(allShares);
		
		if (validShares.size() < parameters.getT())
			return null;
		
		return this.reconstruct(validShares).getMultiplicativeInverse()
				.mul(vote.getB());
	}

	private DecryptionSharesOfVote getOtherDecryptionShares(EncryptedVote vote)
			throws NoSuchAlgorithmException {
		return this.decryptionSharesMap.get(vote.getIndex());
	}

	public void processPacket(Packet packet) {
		PacketData packetData = MainProtocol.getDataFromPacket(packet, players);
		String[] values = packetData.getMsg().getValues();
		int playerIndex = packetData.getFrom().getIndex();

		/*
		 * Process decryption share Stored as a String array: [0] => partial
		 * Decryption [1] => ZKP, e [2] => ZKP, y [3] => vote index
		 */
		if (packetData.getMsg().isDecryptionShareMessage()) {
			SDKGZqElement partialDecryption = g.getGroup()
					.fromString(values[0]);
			ZKPDlogProof proof = new ZKPDlogProof(new BigInteger(values[1]),
					new BigInteger(values[2]));
			int voteIndex = Integer.parseInt(values[3]);
			addDecryptionShare(new DecryptionShare(partialDecryption, proof,
					playerIndex, voteIndex));
		}

		// // process commitment
		// else if (packetData.getMsg().isViCommitmentMessage()) {
		// SDKGZqElement e = g.getGroup().fromString(values[0]);
		// commitmentsMap.append(playerIndex, e);
		// }
	}
	
	private void decrypt() throws NoSuchAlgorithmException{
		if(state != State.DECRYPTION_SHARES_EXCHANGED)
			return; //Exchange shares first!
		
		if(decryptionSharesMap.countNumVotes() != votesMap.size())
			return; //Not all votes!
		
		for (int i = 0; i < votesMap.size(); i++) {
			SDKGZqElement vote = decryptVote(getVote(i));
			
			if(vote == null)
				return; //some vote cannot be reconstructed!
			
			decryptedVotes.add(vote);
		}
		state = State.PROTOCOL_EXECUTION_COMPLETED;
	}

	private void runProtocoll()
			throws NoSuchAlgorithmException {
		switch (state) {
		case INITIAL:
			computeMyDecryptionShares();
			break;
		case DECRYPTION_SHARES_COMPUTED:
			exchangeAllPartialDecryptions();
			break;
		case DECRYPTION_SHARES_EXCHANGED:
			decrypt();
			break;
		case PROTOCOL_EXECUTION_COMPLETED:
			break;
		}
	}
	
	public void execute(){
		Timer timer = new Timer();
		TimerTask task = new TimerTask(){
			@Override
			public void run(){
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
}
