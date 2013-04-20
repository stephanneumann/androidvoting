package com.oksana;

import java.util.ArrayList;
import java.util.Iterator;

public class DecryptionShareList implements Iterable<DecryptionShare>{
	
	public DecryptionShareList(EncryptedVote vote) {
		super();
		this.vote = vote;
		this.decryptionShares = new ArrayList<DecryptionShare>();
	}

	private EncryptedVote vote;
	private ArrayList<DecryptionShare> decryptionShares;
	
	public DecryptionShareList(EncryptedVote vote,
			ArrayList<DecryptionShare> decryptionShares) {
		super();
		this.vote = vote;
		this.decryptionShares = decryptionShares;
	}
	
	EncryptedVote getVote(){
		return vote;
	}

	void addDecryptionShare(DecryptionShare dShare){
		decryptionShares.add(dShare);
	}

	@Override
	public Iterator<DecryptionShare> iterator() {
		return decryptionShares.iterator();
	}
}
