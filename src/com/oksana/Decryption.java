package com.oksana;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import com.murati.smartdkg.dkg.SDKGElGamalParameters;
import com.murati.smartdkg.dkg.SDKGPedersenVSS;
import com.murati.smartdkg.dkg.SDKGPlayer;
import com.murati.smartdkg.dkg.SDKGPlayerList;
import com.murati.smartdkg.dkg.arithm.SDKGLagrange;
import com.murati.smartdkg.dkg.arithm.SDKGZqElement;
import com.murati.smartdkg.dkg.commitments.SDKGShare;

//Party in election protocoll

public class Decryption {
	public Decryption(SDKGPlayer myself, SDKGPlayerList players,
			SDKGElGamalParameters parameters, SDKGPedersenVSS pedersen) {
		super();
		this.myself = myself;
		this.players = players;
		this.parameters = parameters;
		this.pedersen = pedersen;
		
		this.g = new SDKGZqElement(parameters.getQ(), parameters.getG());
		
		this.computeKeyShare();
		this.commit();
		//TODO: Add reading variables from saved data
	}

	private SDKGPlayer myself;
	private SDKGPlayerList players;
	private SDKGElGamalParameters parameters;
	private SDKGPedersenVSS pedersen;

	private BigInteger keyShare; //lambda_i * xi
	private SDKGZqElement g;
	private SDKGZqElement commitment;
	
	private BigInteger getLagrange(){
		Integer j = myself.getIndex();
		ArrayList<Integer> indices = new ArrayList<>();
		for(SDKGPlayer player : players){
			indices.add(player.getIndex());
		}
		SDKGLagrange lagrange = new SDKGLagrange(parameters.getQ());
		BigInteger lambda = lagrange.getLagrangeCoefficient(j, indices);
		return lambda;
	}
	
	void setParameters(){
		
	}
	
	void computeKeyShare(){
		BigInteger xi = pedersen.getMyShares()[0].getValue();
		for (SDKGShare[] share : pedersen.getReceivedShares().values()){
			xi = xi.add(share[0].getValue());
		}
		this.keyShare = xi.multiply(this.getLagrange());
	}
	
	BigInteger getKeyShare(){
		if (keyShare == null)
			this.computeKeyShare();
		return this.keyShare;
	}
	
	DecryptionShare getDecryptionShare(EncryptedVote vote) throws NoSuchAlgorithmException{
		return new DecryptionShare(this.getPartialDecryption(vote), commitment, this.getProof(vote));
	}
	
	ZKPDlogProof getProof(EncryptedVote vote) throws NoSuchAlgorithmException{
		ZKPDlog zkp = new ZKPDlog(g, vote.getA(), commitment, this.getPartialDecryption(vote));
		return zkp.prove(keyShare);
	}
	
	/**
	 * Gets a commitment for future decryption verification
	 * @return
	 */
	SDKGZqElement getCommitment() {
		if (commitment == null)
			this.commit();
		return this.commitment;
	}

	private void commit() {
		this.commitment = g.modPow(keyShare);
	}

	SDKGZqElement getPartialDecryption(EncryptedVote vote) {
		return vote.getA().modPow(keyShare);
	}

	/**
	 * Verifies a decryption share provided by party j for a selected vote
	 * @param vote
	 * @param dShare
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public boolean verifyProof(EncryptedVote vote, DecryptionShare dShare) throws NoSuchAlgorithmException {
		ZKPDlog zpk_j = new ZKPDlog(g, vote.getA(), dShare.getCommitment(), dShare.getPartialDecryption());
		return zpk_j.verify(dShare.getProof());
	}

	/**
	 * Gets a set of valid partial decryptions for one vote
	 * @param shares
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	ArrayList<SDKGZqElement> getValidPD(DecryptionShareList shares) throws NoSuchAlgorithmException {
		ArrayList<SDKGZqElement> validDecryptions = new ArrayList<>();
		for (DecryptionShare ds : shares) {
			if (this.verifyProof(shares.getVote(), ds))
				validDecryptions.add(ds.getPartialDecryption());
		}
		validDecryptions.add(this.getPartialDecryption(shares.getVote()));
		return validDecryptions;
	}
	
	/**
	 * Reconstructs the vote from partial decryptions
	 * @param validDecryptions
	 * @return
	 */
	public SDKGZqElement reconstruct(ArrayList<SDKGZqElement> validDecryptions){
		SDKGZqElement y = g.getOneElement();
		for(SDKGZqElement yi: validDecryptions){
			y = y.mul(yi);
		}
		return y;
	}
}
