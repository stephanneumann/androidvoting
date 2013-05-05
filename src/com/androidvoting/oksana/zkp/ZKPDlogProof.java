package com.androidvoting.oksana.zkp;

import java.math.BigInteger;

public class ZKPDlogProof {
	
	private BigInteger e,y;
	
	public ZKPDlogProof(BigInteger _e, BigInteger _y) {
		e = _e;
		y = _y;
	}
	
	public BigInteger getE(){
		return e;
	}
	
	public BigInteger getY(){
		return y;
	}
}
