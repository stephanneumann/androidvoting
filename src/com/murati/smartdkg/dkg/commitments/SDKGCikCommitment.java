package com.murati.smartdkg.dkg.commitments;

import com.murati.smartdkg.dkg.SDKGElGamalParameters;
import com.murati.smartdkg.dkg.arithm.SDKGZqElement;

import java.math.BigInteger;

/*
 * represents the public verification commitments for shares (Cik)
 */
public class SDKGCikCommitment {

	private int i;

	private int k;

	private SDKGElGamalParameters parameters;

	private SDKGZqElement aik;

	private SDKGZqElement bik;

	private BigInteger value;

	public SDKGCikCommitment(
			int i,
			int k,
			SDKGElGamalParameters parameters,
			SDKGZqElement aik,
			SDKGZqElement bik) {

		this.i = i;
		this.k = k;
		this.parameters = parameters;
		this.aik = aik;
		this.bik = bik;
	}

	public SDKGCikCommitment(
			int i,
			int k,
			BigInteger value) {

		this.i = i;
		this.k = k;
		this.value = value;
	}

	/*
	 * Cik = (g^aik * h^bik) mod p = (g^aik mod p * h^bik mod p) mod p
	 */
	public BigInteger compute() {
		BigInteger g = parameters.getG();
		BigInteger h = parameters.getH();
		BigInteger p = parameters.getP();

		BigInteger gPowAik = g.modPow(aik.getValue(), p);
		BigInteger hPowBik = h.modPow(bik.getValue(), p);

		/*
		 *  ((x^a mod q) * (y^b mod q)) mod q = (x^a * y^b) mod q
		 * 
		 *  otherwise modPow cannot be used!
		 */
		value = gPowAik.multiply(hPowBik).mod(p);

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
