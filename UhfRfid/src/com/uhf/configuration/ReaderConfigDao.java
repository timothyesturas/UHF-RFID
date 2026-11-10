package com.uhf.configuration;

import com.uhf.manager.ApplicationManager;
import com.uhf.utils.RfidFrame;
import com.uhf.utils.Setting;

public class ReaderConfigDao {

    private static final byte CODE_BASIC_PARAMS = (byte) 0x81;
    private static final byte TYPE_GET = RfidFrame.TYPE_GET;  
    private static final byte TYPE_SET = (byte) 0x31;
    private static final int BASIC_LEN = 27;
    private static final int REPLY_TIMEOUT_MS = 1500;

    private final ApplicationManager app;

    public ReaderConfigDao(ApplicationManager app) {
        this.app = app;
    }

    public byte[] readBasic() {
        RfidFrame f = request(CODE_BASIC_PARAMS, TYPE_GET, null);
        if (f == null || f.type != RfidFrame.TYPE_OK || f.payload.length != BASIC_LEN) {
            return null;
        }
        return f.payload;
    }

    public int get(byte[] block, Setting s) {
        return block[s.index] & 0xFF;
    }

    public boolean set(Setting s, int value) {
        byte[] block = readBasic();
        if (block == null) return false;
        block[s.index] = (byte) value;
        RfidFrame f = request(CODE_BASIC_PARAMS, TYPE_SET, block);
        return f != null && f.type == RfidFrame.TYPE_OK;
    }

    private final byte[] ORIGINAL_BASIC = RfidFrame.hexToBytes("0201020A010B1E0A0F000101000000000102060000000000000000");
    private final byte CODE_RF_PARAMS = (byte) 0x59;  
    private final byte[] ORIGINAL_RF_PARAMS = RfidFrame.hexToBytes("0004060120");

    public boolean restoreOriginalBasic() {
        RfidFrame f = request(CODE_BASIC_PARAMS, TYPE_SET, ORIGINAL_BASIC.clone());
        return f != null && f.type == RfidFrame.TYPE_OK;
    }

    public boolean restoreOriginalRf() {
        RfidFrame f = request(CODE_RF_PARAMS, RfidFrame.TYPE_CMD, ORIGINAL_RF_PARAMS.clone());
        return f != null && f.type == RfidFrame.TYPE_OK;
    }

    private static final byte CODE_GET_POWER = (byte) 0x50;
    private static final byte CODE_SET_POWER = (byte) 0x51;

    public static final int MAX_POWER = 30;

    public int readPower() {
        RfidFrame f = request(CODE_GET_POWER, RfidFrame.TYPE_CMD, null);
        if (f == null || f.type != RfidFrame.TYPE_OK || f.payload.length != 1) return -1;
        return f.payload[0] & 0xFF;
    }

    public boolean setPower(int power) {
        if (power < 0 || power > MAX_POWER) return false;
        RfidFrame f = request(CODE_SET_POWER, RfidFrame.TYPE_CMD, new byte[] { (byte) power });
        return f != null && f.type == RfidFrame.TYPE_OK;
    }

    private RfidFrame request(byte code, byte type, byte[] payload) {
        app.clearReply();
        if (!app.sendSerial(RfidFrame.build(0xFFFF, code, type, payload))) return null;
        long end = System.currentTimeMillis() + REPLY_TIMEOUT_MS;
        while (System.currentTimeMillis() < end) {
            RfidFrame f = app.getReplies().poll();
            if (f == null) {
                try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
                continue;
            }
            if (f.code == code) return f; 
        }
        return null;
    }
}
