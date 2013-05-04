package com.androidvoting.oksana;

import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZqElement;

import android.util.SparseArray;

public class DecryptionCommitmentsMap {
	private SparseArray<SDKGZqElement> decryptionCommitments;
	
	public SDKGZqElement getDecryptionCommitment(int index) {
		return decryptionCommitments.get(index);
	}
}
