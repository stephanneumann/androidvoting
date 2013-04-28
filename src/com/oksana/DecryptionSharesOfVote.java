package com.oksana;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Decryption shares for one vote
 * @author oksana
 *
 */
public class DecryptionSharesOfVote implements Iterable<DecryptionShare>{
	
	public DecryptionSharesOfVote(int voteId) {
		this.voteId = voteId;
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

	@Override
	public Iterator<DecryptionShare> iterator() {
		return decryptionShares.iterator();
	}
}
