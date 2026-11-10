//package com.hqonline.dbconn;
//
//import java.io.Serializable;
//import java.sql.Connection;
//import java.sql.SQLException;
//
//import oracle.jdbc.pool.OracleDataSource;
//
//public class Database implements Serializable {
//
//	/**
//	 * 
//	 */
//	private static final long serialVersionUID = 1L;
//	
//	private static Database instance;
//
//	public static Database getInstance() {
//		if (instance == null) {
//			instance = new Database();
//		}
//
//		return instance;
//	}
//
//	public Connection getConnection() {
//
//		Connection connection = null;
//		if (connection == null) {
//			OracleDataSource ds = null;
//
//			try {
//				ds = new OracleDataSource();
//				ds.setDriverType("thin");
//				ds.setServerName(DatabaseConfig.getInstance().getServerName());
//				ds.setDatabaseName(DatabaseConfig.getInstance().getDatabaseName());
//				ds.setPortNumber(Integer.parseInt(DatabaseConfig.getInstance().getPortNo()));
//				ds.setUser(DatabaseConfig.getInstance().getUser());
//				ds.setPassword(DatabaseConfig.getInstance().getPassword());
//				connection = ds.getConnection();
//			} catch (SQLException var4) {
//				var4.printStackTrace();
//				return null;
//			}
//		}
//
//		return connection;
//	}
//
//}


package com.uhf.dbconn;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

import oracle.jdbc.pool.OracleDataSource;

public class Database implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static Database instance;

	public static Database getInstance() {
		if (instance == null) {
			instance = new Database();
		}

		return instance;
	}

	public Connection getConnection(String busUnit) {

		Connection connection = null;

		OracleDataSource ds = null;

		DatabaseConfigManager m = DatabaseConfigManager.getInstance();

		DatabaseConfig dc = m.get(busUnit);

		if (dc != null) {
			try {
				ds = new OracleDataSource();
				ds.setDriverType("thin");
				ds.setServerName(dc.getServerName());
				ds.setDatabaseName(dc.getDatabaseName());
				ds.setPortNumber(Integer.parseInt(dc.getPortNo()));
				ds.setUser(dc.getUser());
				ds.setPassword(dc.getPassword());

				connection = ds.getConnection();
				connection.setAutoCommit(false);
			} catch (SQLException var4) {
				var4.printStackTrace();
				return null;
			}
		}

		return connection;
	}

}
