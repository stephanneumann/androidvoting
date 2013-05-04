package com.androidvoting.oksana;

import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZqElement;

public class DecryptionShare {
	public DecryptionShare(SDKGZqElement partialDecryption, ZKPDlogProof proof,
			int playerIndex, int voteIndex) {
		this.partialDecryption = partialDecryption;
		this.proof = proof;
		this.playerIndex = playerIndex;
		this.voteIndex = voteIndex;
	}

	private SDKGZqElement partialDecryption;
	private ZKPDlogProof proof;
	private int playerIndex, voteIndex;
	
	ZKPDlogProof getProof(){
		return proof;
	}
	
	SDKGZqElement getPartialDecryption(){
		return partialDecryption;
	}

	public int getPlayerIndex() {
		return playerIndex;
	}

	public int getVoteIndex() {
		return voteIndex;
	}

}
