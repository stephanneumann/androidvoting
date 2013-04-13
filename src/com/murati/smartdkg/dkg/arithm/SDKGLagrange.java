package com.murati.smartdkg.dkg.arithm;

import com.murati.smartdkg.exceptions.SDKGReconstructionException;

import java.math.BigInteger;
import java.util.ArrayList;

/*
 * represents the lagrange interpolation algorithm
 */
public class SDKGLagrange {

	private BigInteger q;

	public SDKGLagrange(BigInteger q) {
		this.q = q;
	}

	/*
	 * calculation of the lagrange coefficients lambda_j = l / l-j
	 */
	private BigInteger[] calculateLagrangeCoefficients(ArrayList<Integer> indices) {
		BigInteger[] coefficients = new BigInteger[indices.size()];
		BigInteger numerator;
		BigInteger denominator;
		BigInteger product;

		for (int j = 0; j < indices.size(); j++) {
			product = BigInteger.ONE;

			for (int l = 0; l < indices.size(); l++) {
				if (l == j) {
					continue;
				}
				numerator = BigInteger.valueOf(indices.get(l)); // zaehler
				denominator = BigInteger.valueOf(indices.get(l) - indices.get(j)); // nenner

				BigInteger inv = denominator.modInverse(q);
				product = product.multiply(numerator.multiply(inv).mod(q));
			}

			coefficients[j] = product;
		}
		return coefficients;
	}

	// uses a set of n function values f1(x),...,fn(x) with given indices to reconstruct the constant term of a polynom
	public SDKGZqElement reconstruct(ArrayList<Integer> indices, ArrayList<BigInteger> values) {
		if (values.isEmpty()) {
			throw new SDKGReconstructionException("at least degree + 1 indices and values are required for reconstruction");
		}
		BigInteger zi = BigInteger.ZERO;
		BigInteger[] coefficients = calculateLagrangeCoefficients(indices);

		for (int j = 0; j < values.size(); j++) {
			BigInteger current = values.get(j).multiply(coefficients[j]);
			zi = zi.add(current).mod(q);
		}
		return new SDKGZqElement(q, zi);
	}

}
