package com.murati.smartdkg.dkg.arithm;

import com.murati.smartdkg.exceptions.SDKGNoPrimeNumberException;

import org.spongycastle.util.BigIntegers;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;

/*
 *  represents a commutative group mod q where q is prime
 */
public class SDKGZq {

	private BigInteger q; // must be prime

	private static final BigInteger ZERO = BigInteger.ZERO;

	private static final BigInteger ONE = BigInteger.ONE;

	public SDKGZq(BigInteger q) {
		if (!q.isProbablePrime(80)) {
			throw new SDKGNoPrimeNumberException("q must be a prime number");
		}
		this.q = q;
	}

	public BigInteger getGroupOrder() {
		return q;
	}

	// get neutral element with regard to addition operation
	public SDKGZqElement getAddNeutralElement() {
		return new SDKGZqElement(q, ZERO);
	}

	// get neutral element with regard to multiplication operation
	public SDKGZqElement getMulNeutralElement() {
		return new SDKGZqElement(q, ONE);
	}

	// returns a random selected element in Zq
	public SDKGZqElement getRandomElement(SecureRandom rnd) {
		BigInteger value = BigIntegers.createRandomInRange(ONE, q.subtract(ONE), rnd);
		return new SDKGZqElement(q, value);
	}

	// returns n randomly selected elements in Zq
	public ArrayList<SDKGZqElement> getRandomElements(int n, SecureRandom rnd) {
		ArrayList<SDKGZqElement> elements = new ArrayList<SDKGZqElement>(n);

		for (int i = 0; i < n; i++) {
			elements.add(i, getRandomElement(rnd));
		}
		return elements;
	}

}
