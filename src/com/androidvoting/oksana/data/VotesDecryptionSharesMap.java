package com.androidvoting.oksana.data;

import android.util.SparseArray;

public class VotesDecryptionSharesMap {
	private SparseArray<DecryptionSharesOfVote> votes = new SparseArray<DecryptionSharesOfVote>();
	
	public void addDecryptionShare(DecryptionShare share) {
		if (votes.get(share.getVoteIndex()) != null)
			votes.get(share.getVoteIndex()).addDecryptionShare(share);
		else {
			DecryptionSharesOfVote shares = new DecryptionSharesOfVote(share.getVoteIndex());
			shares.addDecryptionShare(share);
			votes.put(share.getVoteIndex(), shares);
		}
		
	}
	
	public int countNumVotes(){
		return votes.size();
	}
	
	public int countNumShares(int voteIndex) {
		return votes.get(voteIndex).countNumShares();
	}

	public DecryptionSharesOfVote get(int voteIndex) {
		return votes.get(voteIndex);
	}
}
