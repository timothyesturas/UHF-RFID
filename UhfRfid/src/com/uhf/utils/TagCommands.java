package com.uhf.utils;

/**
 * Builds the packets needed to change the EPC (the tag's ID number).
 *
 * The reader needs these steps, in this order:
 *   Identify -> Select (pick the tag) -> Write -> Unselect -> Identify (check)
 */
public class TagCommands {

    private static final int ANY_READER = 0xFFFF;
    private static final byte MEMORY_EPC = 0x01;   // the EPC memory bank

    /** The standard allows at most 31 words = 62 bytes in the EPC. */
    public static final int MAX_EPC_BYTES = 62;

    // ====================================================================
    // Checking and converting values
    // ====================================================================

    /** True when the text is only 0-9 / A-F and has an even number of characters. */
    public static boolean isValidHex(String text) {
        return text.length() > 0 && text.length() % 2 == 0 && text.matches("[0-9A-Fa-f]+");
    }

    /** True for a hex EPC the tag can store: whole words (4 characters each), 2 to 62 bytes. */
    public static boolean isValidEpcHex(String text) {
        return isValidHex(text) && text.length() % 4 == 0 && text.length() <= MAX_EPC_BYTES * 2;
    }

    /** True for normal keyboard characters only (no accents, no emoji), 1 to 62 characters. */
    public static boolean isValidText(String text) {
        if (text.length() == 0 || text.length() > MAX_EPC_BYTES) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 0x20 || c > 0x7E) {
                return false;
            }
        }
        return true;
    }

    /** "AB1" becomes "41423100": every character as its ASCII code, padded with 00 to whole words. */
    public static String textToHex(String text) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            hex.append(String.format("%02X", (int) text.charAt(i)));
        }
        if (text.length() % 2 != 0) {
            hex.append("00");                   // an EPC is made of 2-byte words
        }
        return hex.toString();
    }

    /** The reverse. Returns null when the bytes are not plain readable text. */
    public static String hexToText(String hex) {
        byte[] bytes = RfidFrame.hexToBytes(hex);
        int end = bytes.length;
        while (end > 0 && bytes[end - 1] == 0x00) {
            end--;                              // ignore the 00 padding at the end
        }
        if (end == 0) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < end; i++) {
            int b = bytes[i] & 0xFF;
            if (b < 0x20 || b > 0x7E) {
                return null;
            }
            text.append((char) b);
        }
        return text.toString();
    }

    // ====================================================================
    // Packed code: fits a code like DRHQ2026110500001 into 12 bytes (96 bits)
    //
    // Layout A (serial is all digits)            DRHQ2026110500001 -> 4452485120261105000001DC
    //   4 letters  -> 4 bytes, ASCII               D R H Q   = 44 52 48 51
    //   8 digits   -> 4 bytes, 2 digits per byte   20261105  = 20 26 11 05
    //   5 digits   -> 3 bytes, padded with a 0     000001    = 00 00 01
    //   marker     -> 1 byte, DC
    //
    // Layout B (serial has letters, e.g. 0000L)  RFHQ202611090000L -> 52464851 20261109 FC000015
    //   4 letters  -> 4 bytes, ASCII
    //   8 digits   -> 4 bytes, 2 digits per byte
    //   serial     -> 4 bytes: the 5 characters 0-9/A-Z counted as one base-36 number (26 bits),
    //                 with the top 6 bits all set to 1 (first byte is FC..FF) as the marker
    // ====================================================================
    private static final String PACKED_MARKER = "DC";
    private static final String SERIAL_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final long SERIAL_COUNT = 60466176L;       // 36 ^ 5
    private static final long SERIAL_MARKER = 0xFC000000L;

    /** True for 4 capital letters, 8 digits, then 5 characters that are digits or capital letters. */
    public static boolean isPackable(String text) {
        return text.matches("[A-Z]{4}[0-9]{8}[0-9A-Z]{5}");
    }

    /** Text -> 24 hex characters (12 bytes). Only call this when isPackable(text) is true. */
    public static String packText(String text) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            hex.append(String.format("%02X", (int) text.charAt(i)));
        }
        hex.append(text.substring(4, 12));                  // 8 digits, two per byte
        String serial = text.substring(12);
        if (serial.matches("[0-9]{5}")) {
            hex.append("0").append(serial);                 // layout A
            hex.append(PACKED_MARKER);
        } else {
            long value = 0;
            for (int i = 0; i < 5; i++) {
                value = value * 36 + SERIAL_CHARS.indexOf(serial.charAt(i));
            }
            hex.append(String.format("%08X", SERIAL_MARKER | value));   // layout B
        }
        return hex.toString();
    }

    /** The reverse. Returns null when the hex is not a packed code. */
    public static String unpackText(String hex) {
        hex = hex.toUpperCase();
        if (hex.length() != 24) {
            return null;
        }
        String letters = unpackLetters(hex);
        String date = hex.substring(8, 16);
        if (letters == null || !date.matches("[0-9]{8}")) {
            return null;
        }
        String serial = unpackSerialA(hex);
        if (serial == null) {
            serial = unpackSerialB(hex);
        }
        return serial == null ? null : letters + date + serial;
    }

    private static String unpackLetters(String hex) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 8; i += 2) {
            int b = Integer.parseInt(hex.substring(i, i + 2), 16);
            if (b < 'A' || b > 'Z') {
                return null;
            }
            text.append((char) b);
        }
        return text.toString();
    }

    private static String unpackSerialA(String hex) {
        String serial = hex.substring(16, 22);
        if (!hex.endsWith(PACKED_MARKER) || !serial.matches("0[0-9]{5}")) {
            return null;
        }
        return serial.substring(1);
    }

    private static String unpackSerialB(String hex) {
        long v = Long.parseLong(hex.substring(16, 24), 16);
        if ((v & SERIAL_MARKER) != SERIAL_MARKER) {
            return null;
        }
        long value = v & ~SERIAL_MARKER;
        if (value >= SERIAL_COUNT) {
            return null;
        }
        char[] serial = new char[5];
        for (int i = 4; i >= 0; i--) {
            serial[i] = SERIAL_CHARS.charAt((int) (value % 36));
            value /= 36;
        }
        return new String(serial);
    }

    // ====================================================================
    // Packets
    // ====================================================================

    /** Select the tag whose EPC is epcHex. */
    public static byte[] select(String epcHex) {
        byte[] epc = RfidFrame.hexToBytes(epcHex);
        byte[] payload = new byte[epc.length + 2];
        payload[0] = 0x02;                  // mode: match on EPC
        payload[1] = (byte) epc.length;     // how many EPC bytes follow
        System.arraycopy(epc, 0, payload, 2, epc.length);
        return RfidFrame.build(ANY_READER, RfidFrame.CODE_SELECT_TAG, RfidFrame.TYPE_CMD, payload);
    }

    /**
     * Write newEpcHex into the EPC memory of the selected tag (access password 00000000).
     * Use this when the new EPC has the SAME length as the old one.
     */
    public static byte[] writeEpc(String newEpcHex) {
        byte[] epc = RfidFrame.hexToBytes(newEpcHex);
        byte[] payload = new byte[7 + epc.length];
        // payload[0..3] = access password, left as 00 00 00 00
        payload[4] = MEMORY_EPC;
        payload[5] = 0x02;                        // words 0 and 1 hold CRC and PC, the EPC starts at word 2
        payload[6] = (byte) (epc.length / 2);     // length counted in words (2 bytes each)
        System.arraycopy(epc, 0, payload, 7, epc.length);
        return RfidFrame.build(ANY_READER, RfidFrame.CODE_WRITE_TAG, RfidFrame.TYPE_CMD, payload);
    }

    /**
     * Write only some words of the EPC, starting at EPC word firstWord (0 = the first 2 bytes).
     * wordsHex holds 4 hex characters per word. Same length as the tag's EPC is assumed.
     */
    public static byte[] writeEpcWords(int firstWord, String wordsHex) {
        byte[] data = RfidFrame.hexToBytes(wordsHex);
        byte[] payload = new byte[7 + data.length];
        payload[4] = MEMORY_EPC;
        payload[5] = (byte) (0x02 + firstWord);   // word 2 of the EPC bank is the first EPC word
        payload[6] = (byte) (data.length / 2);
        System.arraycopy(data, 0, payload, 7, data.length);
        return RfidFrame.build(ANY_READER, RfidFrame.CODE_WRITE_TAG, RfidFrame.TYPE_CMD, payload);
    }

    /**
     * Same, but for a new EPC with a DIFFERENT length. The tag keeps its length in the PC word
     * (word 1), so the PC is written together with the EPC, in one command starting at word 1.
     * The top 5 bits of the PC are the EPC length in words; the other 11 bits are kept as they were.
     */
    public static byte[] writeEpcAndLength(String newEpcHex, int oldPc) {

        byte[] epc = RfidFrame.hexToBytes(newEpcHex);

        int newPc = ((epc.length / 2) << 11) | (oldPc & 0x07FF);

        byte[] payload = new byte[9 + epc.length];

        payload[4] = MEMORY_EPC;
        payload[5] = 0x01;
        payload[6] = (byte) (1 + epc.length / 2);

        payload[7] = (byte) (newPc >> 8);
        payload[8] = (byte) newPc;

        System.arraycopy(epc, 0, payload, 9, epc.length);

        return RfidFrame.build(
            ANY_READER,
            RfidFrame.CODE_WRITE_TAG,
            RfidFrame.TYPE_CMD,
            payload
        );
    }

    /** Release the tag again. */
    public static byte[] unselect() {
        return RfidFrame.build(ANY_READER, RfidFrame.CODE_SELECT_TAG, RfidFrame.TYPE_CMD, new byte[] { 0x00, 0x00 });
    }
}