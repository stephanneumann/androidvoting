package com.androidvoting.oksana.data;

import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZqElement;

public class EncryptedVote implements Comparable<EncryptedVote>{
	private SDKGZqElement a, b;
	private int index;

	public EncryptedVote(SDKGZqElement a, SDKGZqElement b, int index) {
		super();
		this.a = a;
		this.b = b;
		this.index = index;
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
