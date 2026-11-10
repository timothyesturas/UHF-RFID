package com.uhf.utils;

public class RfidFrame {
	
	public static final byte CODE_WRITE_TAG = (byte) 0x22;  // write data to a tag
	public static final byte CODE_SELECT_TAG = (byte) 0x2D; // select / unselect a tag before writing

    // ---------- Start bytes ----------
    public static final byte CMD_PREAMBLE = (byte) 0x7C;    // packets we send
    public static final byte RESP_PREAMBLE = (byte) 0xCC;   // packets the reader sends

    // ---------- Codes (what the packet is about) ----------
    public static final byte CODE_READ_TAG = (byte) 0x20;   // scan for tags ("Identify")
    public static final byte CODE_GET_INFO = (byte) 0x82;   // ask for reader information

    // ---------- Types (kind of message) ----------
    public static final byte TYPE_CMD = (byte) 0x00;        // we send: a command
    public static final byte TYPE_OK = (byte) 0x00;         // reader answers: success
    public static final byte TYPE_ERR = (byte) 0x01;        // reader answers: error (for example "no tag")
    public static final byte TYPE_NOTIFY = (byte) 0x02;     // reader reports: a tag was read
    public static final byte TYPE_AUTO = (byte) 0x05;       // reader reports: a tag was read (automatic)
    public static final byte TYPE_GET = (byte) 0x32;        // we send: "get" a setting

    // Number of bytes before the payload: start(1) + address(2) + code(1) + type(1) + length(1)
    public static final int HEADER_LEN = 6;

    // ---------- The parts of a packet we received ----------
    public int address;
    public byte code;
    public byte type;
    public byte[] payload;

    // ====================================================================
    // Make a packet to SEND to the reader
    // ====================================================================
    public static byte[] build(int address, byte code, byte type, byte[] payload) {
        int payloadLength = 0;
        if (payload != null) {
            payloadLength = payload.length;
        }

        // header + payload + 1 byte for the checksum
        byte[] packet = new byte[HEADER_LEN + payloadLength + 1];

        packet[0] = CMD_PREAMBLE;
        packet[1] = (byte) (address & 0xFF);          // address, low byte first
        packet[2] = (byte) ((address >> 8) & 0xFF);   // address, high byte second
        packet[3] = code;
        packet[4] = type;
        packet[5] = (byte) payloadLength;

        for (int i = 0; i < payloadLength; i++) {
            packet[HEADER_LEN + i] = payload[i];
        }

        // the checksum goes in the last byte
        packet[packet.length - 1] = calculateChecksum(packet, packet.length - 1);
        return packet;
    }

    /**
     * Checksum = add up all the bytes, then take the negative.
     * (This way, adding ALL bytes of a good packet, checksum included, gives 0.)
     */
    private static byte calculateChecksum(byte[] data, int howManyBytes) {
        int sum = 0;
        for (int i = 0; i < howManyBytes; i++) {
            sum += data[i];
        }
        return (byte) -sum;
    }

    // ====================================================================
    // Take apart a packet we RECEIVED (FrameBuffer already checked it is complete)
    // ====================================================================
    public static RfidFrame parse(byte[] data) {
        RfidFrame frame = new RfidFrame();

        // address is stored low byte first
        frame.address = (data[1] & 0xFF) | ((data[2] & 0xFF) << 8);
        frame.code = data[3];
        frame.type = data[4];

        // "& 0xFF" turns Java's signed byte (-128..127) into a normal number (0..255)
        int payloadLength = data[5] & 0xFF;
        frame.payload = new byte[payloadLength];
        for (int i = 0; i < payloadLength; i++) {
            frame.payload[i] = data[HEADER_LEN + i];
        }
        return frame;
    }

    // ====================================================================
    // Show bytes as text, for example {0x12, 0xAB} becomes "12AB"
    // ====================================================================
    public static String bytesToHex(byte[] bytes, int start, int count) {
        String result = "";
        for (int i = start; i < start + count; i++) {
            result = result + String.format("%02X", bytes[i]);
        }
        return result;
    }
    
    public static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }
}