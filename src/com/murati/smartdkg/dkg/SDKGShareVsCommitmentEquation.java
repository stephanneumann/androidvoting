package com.murati.smartdkg.dkg;

import com.murati.smartdkg.dkg.commitments.SDKGAikCommitment;
import com.murati.smartdkg.dkg.commitments.SDKGCikCommitment;
import com.murati.smartdkg.dkg.commitments.SDKGShare;

import java.math.BigInteger;

public class SDKGShareVsCommitmentEquation {

	private SDKGElGamalParameters parameters;

	private int powerIndex;

	private SDKGShare[] shares;

	private SDKGCikCommitment[] CikCommitments;

	private SDKGAikCommitment[] AikCommitments;

	private BigInteger lhs = BigInteger.ONE; // left hand side of equation

	private BigInteger rhs = BigInteger.ONE; // right hand side of equation

	public SDKGShareVsCommitmentEquation(
			SDKGElGamalParameters parameters,
			int powerIndex,
			SDKGShare[] shares,
			SDKGCikCommitment[] commitments) {

		this.parameters = parameters;
		this.powerIndex = powerIndex;
		this.shares = shares;
		this.CikCommitments = commitments;
	}

	public SDKGShareVsCommitmentEquation(
			SDKGElGamalParameters parameters,
			int j,
			SDKGShare[] shares,
			SDKGAikCommitment[] commitments) {

		this.parameters = parameters;
		this.powerIndex = j;
		this.shares = shares;
		this.AikCommitments = commitments;
	}

	/*
	 * computes the left hand side of the equation - namely g^sij * h^sij
	 */
	private BigInteger computeLHS() {
		BigInteger g = parameters.getG();
		BigInteger h = parameters.getH();
		BigInteger p = parameters.getP();

		SDKGShare sij = shares[0]; // sij
		SDKGShare sij_ = shares[1]; // sij'

		BigInteger gPowSij = g.modPow(sij.getValue(), p);

		if (CikCommitments != null && AikCommitments == null) {
			BigInteger hPowSij_ = h.modPow(sij_.getValue(), p);
			lhs = gPowSij.multiply(hPowSij_);
		} else if (CikCommitments == null && AikCommitments != null) {
			lhs = gPowSij;
		}

		lhs = lhs.mod(p);

		return lhs;
	}

	/*
	 * compute the right hand side of the equation - namely prod(k=0 to t) (Cik)^j^k mod p
	 */
	private BigInteger computeRHS() {
		BigInteger p = parameters.getP();
		int t = parameters.getT();

		// TODO: use an abstract class commitment instead!
		if (CikCommitments != null && AikCommitments == null) {
			for (int k = 0; k <= t; k++) {
				rhs = rhs.multiply(CikCommitments[k].getValue().modPow(BigInteger.valueOf((int) Math.pow(powerIndex, k)),p));
			}
		} else if (CikCommitments == null && AikCommitments != null) {
			for (int k = 0; k <= t; k++) {
				rhs = rhs.multiply(AikCommitments[k].getValue().modPow(BigInteger.valueOf((int) Math.pow(powerIndex, k)), p));
			}
		}

		rhs = rhs.mod(p);

		return rhs;
	}

	/*
	 * verifies received shares
	 */
	public boolean satisfy() {
		return computeLHS().equals(computeRHS());
	}

	/*
	 * returns the left hand side of the equation
	 */
	public BigInteger getLeftHandSide() {
		return lhs;
	}

	/*
	 * returns the right hand side of the equation
	 */
	public BigInteger getRightHandSide() {
		return rhs;
	}

	public int getIndexI() {
		return powerIndex;
	}

}
