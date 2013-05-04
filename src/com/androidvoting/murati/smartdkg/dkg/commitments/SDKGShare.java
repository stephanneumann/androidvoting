package com.androidvoting.murati.smartdkg.dkg.commitments;

import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGPolynom;
import com.androidvoting.murati.smartdkg.exceptions.SDKGInvalidPolynomException;

import java.math.BigInteger;

public class SDKGShare {

	private int firstIndex;

	private int secondIndex;

	private SDKGPolynom p;

	private BigInteger value;

	public SDKGShare(int firstIndex, int secondIndex) {
		this.firstIndex = firstIndex;
		this.secondIndex = secondIndex;
	}

	public SDKGShare(int firstIndex, int secondIndex, SDKGPolynom p) {
		this(firstIndex, secondIndex);
		this.p = p;
	}

	public SDKGShare(int firstIndex, int secondIndex, BigInteger value) {
		this(firstIndex, secondIndex);
		this.value = value;
	}

	// computes a share sij = f(j) mod q
	public BigInteger compute() {
		if (p == null) {
			throw new SDKGInvalidPolynomException("polynom should not be null");
		}
		value = p.evaluateModQ(BigInteger.valueOf(secondIndex));
		return value;
	}

	public BigInteger getValue() {
		return value;
	}

	public int getFirstIndex() {
		return firstIndex;
	}

	public int getSecondIndex() {
		return secondIndex;
	}

	@Override
	public String toString() {
		return value.toString();
	}

}
