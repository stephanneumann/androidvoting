package com.androidvoting.oksana.application;

import java.math.BigInteger;

public class UserData {
	private static BigInteger decryptionKey;


	// Galaxy, player index = 2

	private static final String username = "hansw1415@gmail.com";

	private static final String password = "testtest1";
	
	// Nexus, player index = 1
	

//	private static final String username = "rolfm879@gmail.com";
//
//	private static final String password = "testtest3";


	public static BigInteger getDecryptionKey() {
		return decryptionKey;
	}

	public static String getUsername() {
		return username;
	}

	public static String getPassword() {
		return password;
	}
	
	public static void initDecryptionKey(BigInteger decryptionKey) {
		if (UserData.getDecryptionKey() == null)
			UserData.decryptionKey = decryptionKey;
	}

}
