package com.androidvoting.oksana;

import java.math.BigInteger;

public class ZKPDlogProof {
	
	private BigInteger e,y;
	
	public ZKPDlogProof(BigInteger _e, BigInteger _y) {
		e = _e;
		y = _y;
	}
	
	BigInteger getE(){
		return e;
	}
	
	BigInteger getY(){
		return y;
	}
}
