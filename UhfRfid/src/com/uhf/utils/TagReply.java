package com.uhf.utils;

public class TagReply {

    public final String epcHex;  
    public final int pc;          

    public TagReply(String epcHex, int pc) {
        this.epcHex = epcHex;
        this.pc = pc;
    }

    private static final int[][] LAYOUTS = {
        {1, 1}, {1, 0}, {1, 2}, {0, 1}, {0, 0}, {0, 2},
        {2, 1}, {2, 0}, {2, 2}, {3, 1}, {3, 0}, {3, 2}
    };

    public static TagReply parse(byte[] payload) {
        for (int[] layout : LAYOUTS) {
            int offset = layout[0];
            int tail = layout[1];
            if (payload.length < offset + 2) {
                continue;
            }
            int epcStart = offset + 2;
            int available = payload.length - epcStart - tail;
            if (available < 2) {
                continue;
            }
            int pc = ((payload[offset] & 0xFF) << 8) | (payload[offset + 1] & 0xFF);
            int epcBytes = ((pc >> 11) & 0x1F) * 2;
            if (epcBytes == available) {
                return new TagReply(RfidFrame.bytesToHex(payload, epcStart, epcBytes), pc);
            }
        }
        return null;
    }
}
