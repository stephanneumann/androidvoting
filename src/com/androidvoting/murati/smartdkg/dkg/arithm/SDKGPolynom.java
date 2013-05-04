package com.androidvoting.murati.smartdkg.dkg.arithm;

import com.androidvoting.murati.smartdkg.exceptions.SDKGInvalidPolynomException;

import java.math.BigInteger;
import java.util.ArrayList;

/*
 * represents a polynomial over Zq
 */
public class SDKGPolynom {

	private int degree;

	private ArrayList<SDKGZqElement> coefficients; // max. degree + 1 coefficients

	public SDKGPolynom(int degree) {
		this.degree = degree;
		this.coefficients = new ArrayList<SDKGZqElement>();
	}

	public SDKGPolynom(int degree, ArrayList<SDKGZqElement> coefficients) {
		this.degree = degree;
		this.coefficients = coefficients;
	}

	public int getDegree() {
		return degree;
	}

	public ArrayList<SDKGZqElement> getCoefficients() {
		return coefficients;
	}

	// use horner shema instead?
	public BigInteger evaluate(BigInteger x) {
		if (x.signum() < 0) {
			throw new SDKGInvalidPolynomException("only positive evaluation points are permitted");
		}

		BigInteger sum = BigInteger.ZERO;
		for (int i = 0; i <= degree; i++) {
			sum = sum.add(coefficients.get(i).getValue().multiply(x.pow(i)));
		}
		return sum;
	}

	public BigInteger evaluateModQ(BigInteger x) {
		BigInteger q = coefficients.get(0).getOrder(); // get order of any element in Zq
		BigInteger value = evaluate(x);
		return value.mod(q);
	}

	public void addCoefficient(SDKGZqElement coefficient) {
		coefficients.add(coefficient);
		degree++;
	}

	public SDKGZqElement getConstantCoefficient() {
		return coefficients.get(0);
	}

	public SDKGZqElement getCoefficient(int position) {
		if (position < 0 || position > degree + 1) {
			throw new SDKGInvalidPolynomException("position must be in range 0...degree + 1");
		}
		return coefficients.get(position);
	}

	public boolean hasCoefficient(SDKGZqElement coefficient) {
		for (SDKGZqElement c : coefficients) {
			if (c.isEqual(coefficient)) {
				return true;
			}
		}
		return false;
	}

	public void reset() {
		coefficients.clear();
	}

	@Override
	public SDKGPolynom clone() {
		return new SDKGPolynom(degree, coefficients);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("p(x) = ").append(coefficients.get(0));

		for(int i = 1; i <= degree; i++) {
			sb.append(" + ").append(coefficients.get(i)).append("x^").append(i);
		}
		return sb.toString();
	}

}