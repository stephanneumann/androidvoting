package com.androidvoting.oksana.data;

import java.util.ArrayList;
import java.util.Iterator;

public class EncryptedVoteList implements Iterable<EncryptedVote>{
	
	public EncryptedVoteList() {
		super();
		votes = new ArrayList<EncryptedVote>();
	}

	public EncryptedVoteList(ArrayList<EncryptedVote> votes) {
		super();
		this.votes = votes;
	}

	private ArrayList<EncryptedVote> votes;

	@Override
	public Iterator<EncryptedVote> iterator() {
		return votes.iterator();
	}
	
	public void add(EncryptedVote vote){
		votes.add(vote);
	}
	
	public EncryptedVote getByIndex(int index) {
		for (EncryptedVote vote: votes) {
			if (vote.getIndex() == index)
				return vote;
		}
		return null; //TODO: Not found exc.
	}

}
