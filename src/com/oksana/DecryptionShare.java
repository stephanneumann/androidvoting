package com.oksana;

import com.murati.smartdkg.dkg.arithm.SDKGZqElement;

public class DecryptionShare {
	public DecryptionShare() {
		super(); //temp
	}

	public DecryptionShare(SDKGZqElement partialDecryption,
			SDKGZqElement commitment, ZKPDlogProof proof) {
		super();
		this.partialDecryption = partialDecryption;
		this.commitment = commitment;
		this.proof = proof;
	}

	private SDKGZqElement partialDecryption, commitment;
	private ZKPDlogProof proof;
	
	ZKPDlogProof getProof(){
		return proof;
	}
	
	SDKGZqElement getPartialDecryption(){
		return partialDecryption;
	}
	
	SDKGZqElement getCommitment(){
		return commitment;
	}

}
