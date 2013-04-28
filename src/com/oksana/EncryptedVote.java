package com.oksana;

import com.murati.smartdkg.dkg.arithm.SDKGZqElement;

public class EncryptedVote implements Comparable<EncryptedVote>{
	private SDKGZqElement a, b;
	private int index;

	public EncryptedVote(SDKGZqElement a, SDKGZqElement b) {
		super();
		this.a = a;
		this.b = b;
	}
	
	public SDKGZqElement getA(){
		return a;
	}

	public SDKGZqElement getB() {
		return b;
	}
	
	public int getIndex() {
		return index;
	}

	@Override
	public int compareTo(EncryptedVote other) {
		if (this.index > other.index) return 1;
		else if (this.index < other.index) return -1;
		return 0;
	}
	
	@Override
	public String toString(){
		return this.getA().toString() + ";" + this.getB().toString();
	}
}
