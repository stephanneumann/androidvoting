package com.androidvoting.oksana.data;

import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZqElement;
import com.androidvoting.oksana.zkp.ZKPDlogProof;

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
	
	public ZKPDlogProof getProof(){
		return proof;
	}
	
	public SDKGZqElement getPartialDecryption(){
		return partialDecryption;
	}

	public int getPlayerIndex() {
		return playerIndex;
	}

	public int getVoteIndex() {
		return voteIndex;
	}

}
