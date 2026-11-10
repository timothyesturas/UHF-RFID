package com.uhf.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.uhf.configuration.ReaderConfigDao;
import com.uhf.dao.TagDao;
import com.uhf.dao.WriteResult;

public class ServiceUtils {
	
	private static final int MAX_WRITE_ATTEMPTS = 20;
	private static final int RETRY_DELAY_MS = 500;
	
	public static void readMode(Scanner in, TagDao dao) {
        System.out.println("-- Read mode -- new tags are added to the list as they are detected.");
        System.out.println("(type Q + Enter to go back, R + Enter to Refreshed the list)");
        List<TagReply> found = new ArrayList<>();
        while (true) {
            for (TagReply tag : dao.findAll()) {
                if (isKnown(found, tag)) continue;
                found.add(tag);
                beep();
                String text = decodeText(tag.epcHex);
                System.out.println("  " + found.size() + ") " + tag.epcHex + (text != null ? "  [" + text + "]" : ""));
            }

            try {
                if (System.in.available() > 0) {
                    String answer = in.nextLine().trim();
                    if (answer.equalsIgnoreCase("Q")) return;
                    if (answer.equalsIgnoreCase("q")) return;
                    if (answer.equalsIgnoreCase("R"))
                    if (answer.equalsIgnoreCase("r")) {
                        found.clear();
                        System.out.println("List Refreshed.");
                    }
                }
            } catch (java.io.IOException e) {
                return;
            }
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    }
	
	 public static boolean writeOnce(Scanner in, TagDao dao, Connection connection) throws SQLException {
	        TagReply tag = chooseTagLive(in, dao);
	        if (tag == null) return false;                       

	        System.out.println();
	        System.out.println("Selected: " + tag.epcHex);

	        tag = waitForSelectedTag(tag, in, dao);
	        if (tag == null) return true;                        

	        System.out.println("Generating RFID reference number...");

	        String text;
	        try {
	            text = dao.getNextReferenceNo("UHFRFID_HD", "UHFRFID_HD_SEQ", "yyyyMMdd", "RF", "HQ", connection);
	        } catch (Exception e) {
	        	connection.rollback();
	            System.out.println("Reference generation failed: " + e.getMessage());
	            return true;
	        }

	        if (text == null || text.isBlank()) {
	            System.out.println("Reference number generation returned empty.");
	            return true;
	        }

	        System.out.println("Generated Reference: " + text);

	        if (!TagCommands.isValidText(text)) {
	            System.out.println("Invalid generated reference.");
	            return true;
	        }

	        String newHex = TagCommands.isPackable(text)
	                ? TagCommands.packText(text)
	                : TagCommands.textToHex(text);

	        System.out.println("Encoded HEX: " + newHex);

	        if (tag.epcHex.equalsIgnoreCase(newHex)) {
	            System.out.println("Tag already contains this value.");
	            return true;
	        }

	        writeAndVerify(in, text, tag, newHex, dao, connection);
	        return true;
	    }

	    public static boolean writeManualOnce(Scanner in, TagDao dao, Connection connection) throws SQLException {
	        TagReply tag = chooseTagLive(in, dao);
	        if (tag == null) return false;

	        System.out.println();
	        System.out.println("Selected: " + tag.epcHex);

	        tag = waitForSelectedTag(tag, in, dao);
	        if (tag == null) return true;

	        System.out.print("Value to write, for example RFHQ2026110910001 (Q = cancel): ");
	        String text = in.nextLine().trim();
	        if (text.equalsIgnoreCase("Q")) return true;
	        if (text.equalsIgnoreCase("q")) return true;
	        if (!TagCommands.isValidText(text)) {
	            System.out.println("Use 1 to " + TagCommands.MAX_EPC_BYTES + " normal keyboard characters.");
	            return true;
	        }

	        String newHex = TagCommands.isPackable(text) ? TagCommands.packText(text) : TagCommands.textToHex(text);
	        System.out.println("Encoded HEX: " + newHex);

	        if (tag.epcHex.equalsIgnoreCase(newHex)) {
	            System.out.println("Tag already contains this value.");
	            return true;
	        }
	        writeAndVerify(in, text, tag, newHex, dao, connection);
	        return true;
	    }
	    
	    
	    public static void readerSettings(Scanner in, ReaderConfigDao config) {
	        while (true) {
	            byte[] block = config.readBasic();
	            if (block == null) {
	                System.out.println("Could not read the reader settings (no valid reply).");
	                return;
	            }
	            Setting[] all = Setting.values();
	            int powerChoice = all.length + 1;
	            int power = config.readPower();

	            System.out.println();
	            System.out.println("-- Reader settings --");
	            for (int i = 0; i < all.length; i++) {
	                Setting s = all[i];
	                System.out.println("  " + (i + 1) + ") " + s.label + " = " + config.get(block, s)
	                        + (s.unit.isEmpty() ? "" : " (" + s.unit + ")"));
	            }
	            System.out.println("  " + powerChoice + ") RF power (detection range, dBm) = "
	                    + (power < 0 ? "unknown" : power + " (max " + ReaderConfigDao.MAX_POWER + ")"));
	            System.out.print("Change which setting (number, R = restore original settings, Q = back): ");
	            String answer = in.nextLine().trim();
	            if (answer.equalsIgnoreCase("R")) return;
	            if (answer.equalsIgnoreCase("q")) return;
	            if (answer.equalsIgnoreCase("R"))
	            if (answer.equalsIgnoreCase("r")){
	                restoreOriginal(in, config);
	                continue;
	            }

	            int n;
	            try {
	                n = Integer.parseInt(answer);
	            } catch (NumberFormatException e) {
	                System.out.println("Please type a number or q.");
	                continue;
	            }
	            if (n < 1 || n > powerChoice) {
	                System.out.println("No such setting.");
	                continue;
	            }

	            if (n == powerChoice) {
	                changePower(in, config);
	            } else {
	                changeSetting(in, all[n - 1], config);
	            }
	        }
	    }



    public static List<TagReply> scanUntilFoundOrStopped(Scanner in, TagDao dao) {
        System.out.println("Scanning... put a tag near the reader. Press Enter to stop.");
        while (true) {
            List<TagReply> tags = dao.findAll();
            if (!tags.isEmpty()) return tags;
            try {
                if (System.in.available() > 0) {
                    in.nextLine();
                    System.out.println("Scan stopped.");
                    return tags;
                }
            } catch (java.io.IOException e) {
                return tags;
            }
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return tags; }
        }
    }

    public static String decodeText(String hex) {
        String packedText = TagCommands.unpackText(hex);
        return packedText != null ? packedText : TagCommands.hexToText(hex);
    }

    public static TagReply chooseTagLive(Scanner in, TagDao dao) {
        System.out.println("Choose a tag to write (type its number, q = cancel):");
        List<TagReply> found = new ArrayList<>();
        while (true) {
            for (TagReply t : dao.findAll()) {
                if (isKnown(found, t)) continue;
                found.add(t);
                beep();
                String text = decodeText(t.epcHex);
                System.out.println("  " + found.size() + ") " + t.epcHex + (text != null ? "  [" + text + "]" : ""));
            }

            try {
                if (System.in.available() > 0) {
                    String answer = in.nextLine().trim();
                    if (answer.equalsIgnoreCase("q")) {
                        System.out.println("Cancelled.");
                        return null;
                    }
                    if (!answer.isEmpty()) {
                        try {
                            int n = Integer.parseInt(answer);
                            if (n >= 1 && n <= found.size()) return found.get(n - 1);
                        } catch (NumberFormatException ignored) { }
                        System.out.println("  (type a number from the list, or q)");
                    }
                }
            } catch (java.io.IOException e) {
                return null;
            }
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
        }
    }

    public static boolean isKnown(List<TagReply> list, TagReply tag) {
        for (TagReply t : list) {
            if (t.epcHex.equalsIgnoreCase(tag.epcHex)) return true;
        }
        return false;
    }

    public static void beep() {
        try {
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Throwable ignored) {
            System.out.print('\007');
            System.out.flush();
        }
    }
    
    public static void writeAndVerify(Scanner in, String text, TagReply tag, String newHex, TagDao dao, Connection connection) throws SQLException {
        int newBytes = newHex.length() / 2;
        int oldBytes = tag.epcHex.length() / 2;
        if (newBytes > oldBytes) {
            System.out.println("Note: the new value is " + newBytes + " bytes but this tag holds "
                    + oldBytes + " bytes. Many tags cannot be made longer.");
        }

        System.out.println("Writing to RFID tag... (type q + Enter to cancel)");

        for (int attempt = 1; attempt <= MAX_WRITE_ATTEMPTS; attempt++) {
            WriteResult result = dao.write(tag, newHex);

            if (result == WriteResult.OK) {
                System.out.println("Write accepted. Verifying...");
                if (dao.verify(newHex)) {
                    System.out.println("Write OK!");
                    System.out.println("New RFID Reference: " + text);
                } else {
                    System.out.println("Verification failed.");
                }
                return;
            }

            System.out.println("  Attempt " + attempt + " of " + MAX_WRITE_ATTEMPTS
                    + " failed. Reader: " + dao.getLastReply());

            long end = System.currentTimeMillis() + RETRY_DELAY_MS;
            while (System.currentTimeMillis() < end) {
                try {
                    if (System.in.available() > 0 && in.nextLine().trim().equalsIgnoreCase("q")) {
                        System.out.println("Cancelled. " + text + " was issued but not written to a tag.");
                        return;
                    }
                } catch (java.io.IOException e) {
    	        	connection.rollback();
                    return;
                }
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
        System.out.println("Could not write the tag after " + MAX_WRITE_ATTEMPTS + " attempts. "
                + text + " was issued but not written to a tag.");
    }
    
    public static TagReply waitForSelectedTag(
            TagReply selected,
            Scanner in,
            TagDao dao) {

        System.out.println();
        System.out.println("Waiting for selected tag...");
        System.out.println("Present the selected tag near the reader.");
        System.out.println("Press q + Enter to cancel.");

        while (true) {

            List<TagReply> tags = dao.findAll();

            for (TagReply detected : tags) {

                if (detected.epcHex.equalsIgnoreCase(selected.epcHex)) {

                    System.out.println("Selected tag detected again!");
                    System.out.println("EPC: " + detected.epcHex);

                    return detected;
                }
            }

            try {

                if (System.in.available() > 0) {

                    String answer = in.nextLine().trim();

                    if (answer.equalsIgnoreCase("q")) {
                        System.out.println("Cancelled.");
                        return null;
                    }
                }

            } catch (java.io.IOException e) {
                return null;
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }
    
    public static void restoreOriginal(Scanner in, ReaderConfigDao config) {
        System.out.println("This puts the reader back to its original settings: Work mode Active, buzzer on,");
        System.out.println("intervals 10 / 1 / 1, and the RF parameters the demo's Set button overwrote.");
        System.out.println("(RF power is not touched.)");
        System.out.print("Continue? (y/N): ");
        if (!in.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.println("Cancelled.");
            return;
        }
        System.out.println("  Basic settings : " + (config.restoreOriginalBasic() ? "restored" : "FAILED"));
        System.out.println("  RF parameters  : " + (config.restoreOriginalRf() ? "restored" : "FAILED"));
    }
    

    public static void changeSetting(Scanner in, Setting s, ReaderConfigDao config) {
        Integer value = askNumber(in, s.label, s.min, s.max);
        if (value == null) return;
        if (!config.set(s, value)) {
            System.out.println("Reader did not accept the change.");
            return;
        }
        byte[] after = config.readBasic();
        if (after != null && config.get(after, s) == value) {
            System.out.println("Saved. " + s.label + " is now " + value + ".");
        } else {
            System.out.println("Command accepted, but the value read back is different.");
        }
    }

    public static void changePower(Scanner in, ReaderConfigDao config) {
        Integer value = askNumber(in, "RF power in dBm (lower = shorter range)", 0, ReaderConfigDao.MAX_POWER);
        if (value == null) return;
        if (!config.setPower(value)) {
            System.out.println("Reader did not accept the change.");
            return;
        }
        int after = config.readPower();
        if (after == value) {
            System.out.println("Saved. RF power is now " + value + ".");
        } else {
            System.out.println("Command accepted, but the value read back is " + after + ".");
        }
    }

    public static Integer askNumber(Scanner in, String label, int min, int max) {
        System.out.print("New value for " + label + " (" + min + "-" + max + ", Enter = cancel): ");
        String text = in.nextLine().trim();
        if (text.isEmpty()) return null;
        try {
            int v = Integer.parseInt(text);
            if (v >= min && v <= max) return v;
        } catch (NumberFormatException ignored) { }
        System.out.println("Value must be a number from " + min + " to " + max + ".");
        return null;
    }
    
}
