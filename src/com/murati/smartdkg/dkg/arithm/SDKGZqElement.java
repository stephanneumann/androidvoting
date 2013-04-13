package com.murati.smartdkg.dkg.arithm;

import com.murati.smartdkg.exceptions.SDKGNoPrimeNumberException;

import java.math.BigInteger;

/*
 * represents an element of a group Zq
 */
public class SDKGZqElement {

	private BigInteger q;

	private BigInteger value;

	private static final BigInteger ZERO = BigInteger.ZERO;

	private static final BigInteger ONE = BigInteger.ONE;

	public SDKGZqElement(BigInteger q, BigInteger value) {
		if (!q.isProbablePrime(80)) {
			throw new SDKGNoPrimeNumberException("q must be a prime number");
		}
		this.q = q;
		this.value = value;
	}

	public BigInteger getOrder() {
		return q;
	}

	public BigInteger getValue() {
		return value;
	}

	public SDKGZqElement add(SDKGZqElement e) {
		BigInteger sum = value.add(e.getValue()).mod(q);
		return new SDKGZqElement(q, sum);
	}

	public SDKGZqElement sub(SDKGZqElement e) {
		BigInteger dif = value.subtract(e.getValue()).mod(q);
		return new SDKGZqElement(q, dif);
	}

	public SDKGZqElement mul(SDKGZqElement e) {
		BigInteger prod = value.multiply(e.getValue()).mod(q);
		return new SDKGZqElement(q, prod);
	}

	public SDKGZqElement div(SDKGZqElement e) {
		SDKGZqElement nPowMinusOne = e.getMultiplicativeInverse();
		return mul(nPowMinusOne);
	}

	public SDKGZqElement getAdditiveInverse() {
		BigInteger dif = q.subtract(value).mod(q);
		return new SDKGZqElement(q, dif);
	}

	public SDKGZqElement getMultiplicativeInverse() {
		if (isEqual(getZeroElement())) {
			throw new ArithmeticException("division by zero");
		}
		BigInteger multInverse = value.modInverse(q);
		return new SDKGZqElement(q, multInverse);
	}

	public boolean isEqual(SDKGZqElement e) {
		return q.equals(e.getOrder())
				&& value.equals(e.getValue());
	}

	public SDKGZqElement getZeroElement() {
		return new SDKGZqElement(q, ZERO);
	}

	public SDKGZqElement getOneElement() {
		return new SDKGZqElement(q, ONE);
	}

	@Override
	public String toString() {
		return value.toString();
	}

}