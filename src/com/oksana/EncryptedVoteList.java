package com.oksana;

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

}
