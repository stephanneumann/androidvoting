package com.murati.smartdkg.dkg;

import com.murati.smartdkg.exceptions.SDKGNoPrimeNumberException;
import com.murati.smartdkg.exceptions.SDKGNoSafePrimeNumberException;

import java.math.BigInteger;

/*
 * represents the parameters of protocol and the cryptosystem
 */
public final class SDKGElGamalParameters {

	private BigInteger p;

	private BigInteger q;

	private BigInteger g;

	private BigInteger h;

	private int t; // threshold parameter

	public SDKGElGamalParameters() {

	}

	public SDKGElGamalParameters(
			BigInteger p,
			BigInteger q,
			BigInteger g,
			BigInteger h,
			int t) {

		this.p = p;
		this.q = q;
		this.g = g;
		this.h = h;
		this.t = t;

		if (!p.isProbablePrime(80) || !q.isProbablePrime(80)) {
			throw new SDKGNoPrimeNumberException("p and q must be prime numbers");
		}

		// p = 2q + 1
		if (!p.equals(q.multiply(BigInteger.valueOf(2)).add(BigInteger.ONE))) {
			throw new SDKGNoSafePrimeNumberException("p is not a safe prime. p must be p = 2q + 1");
		}
	}

	public void setP(BigInteger p) {
		this.p = p;
	}

	public BigInteger getP() {
		return p;
	}

	public void setQ(BigInteger q) {
		this.q = q;
	}

	public BigInteger getQ() {
		return q;
	}

	public void setG(BigInteger g) {
		this.g = g;
	}

	public BigInteger getG() {
		return g;
	}

	public void setH(BigInteger h) {
		this.h = h;
	}

	public BigInteger getH() {
		return h;
	}

	public void setT(int t) {
		this.t = t;
	}

	public int getT() {
		return t;
	}

}
