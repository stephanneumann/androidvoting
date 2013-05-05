package com.androidvoting.oksana.data;

import java.math.BigInteger;
import java.util.ArrayList;

import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGLagrange;
import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZqElement;

import android.util.SparseArray;

public class ValidDecryptionsSet {
	public ValidDecryptionsSet(BigInteger p, BigInteger q) {
		this.p = p;
		this.q = q;
		partialDecryptions = new SparseArray<SDKGZqElement>();
	}

	private SparseArray<SDKGZqElement> partialDecryptions;
	private BigInteger p, q;

	public void add(SDKGZqElement partialDecryption, int playerIndex) {
		partialDecryptions.put(playerIndex, partialDecryption);
	}

	public SDKGZqElement reconstruct() {
		SDKGLagrange lagrange = new SDKGLagrange(q);
		ArrayList<Integer> indices = new ArrayList<Integer>();
		int n = partialDecryptions.size();

		for (int i = 0; i < n; i++) {
			indices.add(partialDecryptions.keyAt(i));
		}
		BigInteger[] lambdas = lagrange.calculateLagrangeCoefficients(indices);
		
		SDKGZqElement result = new SDKGZqElement(p, BigInteger.ONE);
		
		for(int i = 0; i < n; i++){
			result = result.mul(partialDecryptions.get(indices.get(i)).modPow(lambdas[i]));
		}
		
		return result;
	}
}
