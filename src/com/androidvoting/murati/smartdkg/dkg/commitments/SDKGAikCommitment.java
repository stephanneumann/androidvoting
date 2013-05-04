package com.androidvoting.murati.smartdkg.dkg.commitments;

import com.androidvoting.murati.smartdkg.dkg.SDKGElGamalParameters;
import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZqElement;

import java.math.BigInteger;

public class SDKGAikCommitment {

	private int i;

	private int k;

	private SDKGElGamalParameters parameters;

	private SDKGZqElement aik;

	private BigInteger value;

	public SDKGAikCommitment(
			int i,
			int k,
			SDKGElGamalParameters parameters,
			SDKGZqElement aik) {

		this.i = i;
		this.k = k;
		this.parameters = parameters;
		this.aik = aik;
	}

	public SDKGAikCommitment(
			int i,
			int k,
			BigInteger value) {

		this.i = i;
		this.k = k;
		this.value = value;
	}

	/*
	 * Aik = g^aik mod p
	 */
	public BigInteger compute() {
		BigInteger g = parameters.getG();
		BigInteger p = parameters.getP();

		value = g.modPow(aik.getValue(), p);
		return value;
	}

	public int getIndexI() {
		return i;
	}

	public int getIndexK() {
		return k;
	}

	public BigInteger getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value.toString();
	}

}
