package com.uhf.dbconn;

import java.io.File;
import java.util.Scanner;

import com.uhf.encryptor.Encryptor;


public class OracleDatabaseConfigSetup {

	public static void main (String[] args) {
		
		Encryptor encryptor = new Encryptor();
		
		OracleDatabaseConfig config = new OracleDatabaseConfig();
		
		Scanner scanner = new Scanner(System.in);
		
		// database name
		System.out.println("Enter database name");
		System.out.println();
		String enteredDatabaseName = scanner.nextLine();
		encryptor.encrypt(enteredDatabaseName);
		config.setDatabaseName(encryptor.getEncryptedText());
		config.setDsk(encryptor.getEncodedKey(encryptor.getSecretKey()));
		config.setDs(encryptor.getEncodedIV(encryptor.getIv()));
		
		// port no
		System.out.println("Enter port no");
		System.out.println();
		String enteredPortNo = scanner.nextLine();
		encryptor.encrypt(enteredPortNo);
		config.setPortNo(encryptor.getEncryptedText());
		config.setPsk(encryptor.getEncodedKey(encryptor.getSecretKey()));
		config.setPs(encryptor.getEncodedIV(encryptor.getIv()));
		
		// user
		System.out.println("Enter user");
		System.out.println();
		String enteredUser = scanner.nextLine();
		encryptor.encrypt(enteredUser);
		config.setUser(encryptor.getEncryptedText());
		config.setUsk(encryptor.getEncodedKey(encryptor.getSecretKey()));
		config.setUs(encryptor.getEncodedIV(encryptor.getIv()));
		
		// password
		System.out.println("Enter password");
		System.out.println();
		String enteredPassword = scanner.nextLine();
		encryptor.encrypt(enteredPassword);
		config.setPassword(encryptor.getEncryptedText());
		config.setPssk(encryptor.getEncodedKey(encryptor.getSecretKey()));
		config.setPsss(encryptor.getEncodedIV(encryptor.getIv()));
		
		// server name
		System.out.println("Enter server name");
		System.out.println();
		String enteredServername = scanner.nextLine();
		encryptor.encrypt(enteredServername);
		config.setServerName(encryptor.getEncryptedText());
		config.setSnk(encryptor.getEncodedKey(encryptor.getSecretKey()));
		config.setSns(encryptor.getEncodedIV(encryptor.getIv()));
		
		System.out.println("Enter business unit");
		System.out.println();

		String enteredBusUnit = scanner.nextLine();

		encryptor.encrypt(enteredBusUnit);

		config.setBusUnit(encryptor.getEncryptedText());
		config.setBuk(encryptor.getEncodedKey(encryptor.getSecretKey()));
		config.setBus(encryptor.getEncodedIV(encryptor.getIv()));
		
		// filename
		System.out.println("Enter file name");
		System.out.println();
		String filename = scanner.nextLine();
		
		OracleConfigFileManager oracleConfigFileManager = new OracleConfigFileManager();
		oracleConfigFileManager.save(config, new File("c:\\config\\" + filename));
		
		scanner.close();
	}
}
