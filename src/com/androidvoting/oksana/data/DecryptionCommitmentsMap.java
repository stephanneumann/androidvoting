package com.androidvoting.oksana.data;

import com.androidvoting.murati.smartdkg.dkg.arithm.SDKGZqElement;

import android.util.SparseArray;

public class DecryptionCommitmentsMap {
	private SparseArray<SDKGZqElement> commitments = new SparseArray<SDKGZqElement>();
	
	public void put(int playerIndex, SDKGZqElement commitment){
		commitments.put(playerIndex, commitment);
	}
	
	public SDKGZqElement get(int playerIndex){
		return commitments.get(playerIndex);
	}
}
