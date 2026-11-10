package com.uhf.dbconn;

import java.util.ArrayList;
import java.util.List;

public class DatabaseConfigManager {

	private static DatabaseConfigManager instance;

	public static synchronized DatabaseConfigManager getInstance() {

		if (instance == null) {
			instance = new DatabaseConfigManager();
		}

		return instance;
	}

	private DatabaseConfigManager () {

		databaseConfigs = new ArrayList<DatabaseConfig>();
	}

	private List<DatabaseConfig> databaseConfigs;

	public DatabaseConfig get(String busUnit) {

		for (DatabaseConfig d : databaseConfigs) {

			if (d.getBusUnit().equals(busUnit)) {

				return d;
			}
		}

		return null;
	}

	public boolean add(DatabaseConfig d) {

		return databaseConfigs.add(d);
	}

}
