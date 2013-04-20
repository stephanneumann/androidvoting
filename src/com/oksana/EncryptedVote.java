package com.oksana;

import com.murati.smartdkg.dkg.arithm.SDKGZqElement;

public class EncryptedVote {
	private SDKGZqElement a, b;

	public EncryptedVote(SDKGZqElement a, SDKGZqElement b) {
		super();
		this.a = a;
		this.b = b;
	}
	
	SDKGZqElement getA(){
		return a;
	}

	public SDKGZqElement getB() {
		return b;
	}
}
