package com.uhf.listener;

import com.uhf.connection.PortDetails;
import com.uhf.manager.ApplicationManager;

public class ApplicationListener {
	
	public ApplicationManager applicationManager = ApplicationManager.getInstance();
	public PortDetails portDetails = PortDetails.getInstance();
	

    static {
        initialize();
    }

    private static void initialize() {
        System.out.println("Initializing application...");
    }
    

    public void onApplicationStart() {
        System.out.println("Application Started!");

        Runtime.getRuntime().addShutdownHook(
            new Thread(() -> onApplicationDestroy())
        );
    }

    public void onApplicationDestroy() {
        System.out.println("Application Destroyed!");
    }
}
