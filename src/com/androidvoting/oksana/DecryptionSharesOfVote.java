package com.androidvoting.oksana;

import java.util.ArrayList;

/**
 * Decryption shares for one vote
 * @author oksana
 *
 */
public class DecryptionSharesOfVote{
	
	public ArrayList<DecryptionShare> getDecryptionShares() {
		return decryptionShares;
	}

	public DecryptionSharesOfVote(int voteId) {
		this.voteId = voteId;
		decryptionShares = new ArrayList<DecryptionShare>();
	}

	private int voteId;
	private ArrayList<DecryptionShare> decryptionShares;
	
	public DecryptionSharesOfVote(int vote,
			ArrayList<DecryptionShare> decryptionShares) {
		super();
		this.voteId = vote;
		this.decryptionShares = decryptionShares;
	}
	
	public int countNumShares() {
		return decryptionShares.size();
	}
	
	int getVoteId(){
		return voteId;
	}

	void addDecryptionShare(DecryptionShare share){
		if (share.getVoteIndex() == this.voteId) 
			decryptionShares.add(share);
	}
}
