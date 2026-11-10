package com.uhf.utils;

import com.uhf.dbconn.DatabaseConfig;
import com.uhf.dbconn.DatabaseConfigManager;

public class MainUtils {
	
    public static boolean initializeDatabaseConnection() {

        try {

            DatabaseConfig config = new DatabaseConfig();

            config.setXmlConfigFilename(
                "C:\\batchfiles\\config\\dbconfig_hq_dev.xml"
            );

            config.loadConfiguration();

            DatabaseConfigManager.getInstance().add(config);

            DatabaseConfig hq =
                    DatabaseConfigManager.getInstance().get("HQ");

            
            
            if (hq == null) {
                System.err.println(
                    "HQ database configuration not found."
                );
                return false;
            }

            System.out.println(
                "HQ database configuration loaded successfully."
            );

            return true;

        } catch (Exception e) {

            System.err.println(
                "Database initialization failed: " + e.getMessage()
            );

            e.printStackTrace();
            return false;
        }
    }

}
