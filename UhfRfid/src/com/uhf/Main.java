
package com.uhf;

import java.util.List;
import java.util.Scanner;

import com.fazecast.jSerialComm.SerialPort;
import com.uhf.configuration.ReaderConfigDao;
import com.uhf.dao.RfidTagDao;
import com.uhf.dao.TagDao;
import com.uhf.listener.ApplicationListener;
import com.uhf.service.UhfService;
import com.uhf.utils.MainUtils;
import com.uhf.utils.RfidFrame;

public class Main {

    private static final boolean DEBUG = false;

    public static void main(String[] args) {
    	

        ApplicationListener listener = new ApplicationListener();

        listener.onApplicationStart();

        if (!MainUtils.initializeDatabaseConnection()) {
            System.err.println("Cannot start: HQ database configuration failed.");
            return;
        }

        SerialPort port = listener.portDetails.findPort("USB-SERIAL CH340");

        if (port == null) {
            System.err.println("No serial port found.");
            return;
        }

        listener.applicationManager.getSerial().setListener(data -> {
            List<byte[]> packets =
            		listener.applicationManager.getFrameBuffer().append(data);

            for (byte[] raw : packets) {
                if (listener.applicationManager.getReplies().size() < 500) {
                	listener.applicationManager.getReplies().add(RfidFrame.parse(raw));
                }
            }
        });

        if (!listener.applicationManager.getSerial().connect(port)) {
            return;
        }
        
        

        TagDao dao = new RfidTagDao(
        		listener.applicationManager,
                DEBUG ? System.out::println : msg -> { }
        );
        
        ReaderConfigDao readerConfigDao = new ReaderConfigDao(listener.applicationManager);

        UhfService service = new UhfService(dao, readerConfigDao);

        System.out.println("Connected to " + port.getSystemPortName() + ".");

        Scanner in = new Scanner(System.in);

        boolean running = true;

        while (running) {

            System.out.println();
            System.out.println("==== UHF RFID ====");
            System.out.println("1) Read Mode (Read Tag's)");
            System.out.println("2) Write AutoGenerate Mode (Read and Pick a Tag to Generate Control Number)");
//            System.out.println("3) Write Manual Mode (type a value to be write on Tag)");
            System.out.println("S) Reader settings (timing, etc.)");
            System.out.println("Q) Exit");
            System.out.print("Choose: ");

            String choice = in.nextLine().trim();

            switch (choice.toUpperCase()) {
                case "1":
                    service.readMode(in);
                    break;

                case "2":
                    service.writeMode(in);
                    break;

//                case "3":
//                    service.writeManualMode(in);
//                    break;

                case "S":
                case "s":
                    service.readerSettings(in);
                    break;

                case "Q":
                case "q":
                    running = false;
                    break;

                default:
                    System.out.println("Please type 1, 2, 3, S or Q");
            }
        }

        listener.applicationManager.getSerial().disconnect();
    }
        
}
