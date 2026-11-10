package com.uhf.utils;

import java.util.ArrayList;
import java.util.List;


public class FrameBuffer {

    private static final boolean CHECK_CHECKSUM = true;

    private static final int MIN_PACKET_SIZE = RfidFrame.HEADER_LEN + 1; 

    private byte[] buffer = new byte[4096]; 
    private int count = 0;                   

    public synchronized List<byte[]> append(byte[] newBytes) {
        List<byte[]> packets = new ArrayList<>();

        if (count + newBytes.length > buffer.length) {
            count = 0;
        }
        if (newBytes.length > buffer.length) {
            return packets;
        }
        for (int i = 0; i < newBytes.length; i++) {
            buffer[count] = newBytes[i];
            count++;
        }

        int pos = 0;
        while (true) {

            while (pos < count && buffer[pos] != RfidFrame.RESP_PREAMBLE) {
                pos++;
            }

            int bytesLeft = count - pos;
            if (bytesLeft < MIN_PACKET_SIZE) {
                break;
            }

            if (!isKnownType(buffer[pos + 4])) {
                pos++;
                continue;
            }

            int payloadLength = buffer[pos + 5] & 0xFF;
            int packetSize = RfidFrame.HEADER_LEN + payloadLength + 1;
            if (bytesLeft < packetSize) {
                break;   
            }

            if (CHECK_CHECKSUM && !checksumIsGood(pos, packetSize)) {
                System.err.println("FrameBuffer: skipped a packet with a bad checksum");
                pos++;
                continue;
            }

            byte[] packet = new byte[packetSize];
            for (int i = 0; i < packetSize; i++) {
                packet[i] = buffer[pos + i];
            }
            packets.add(packet);
            pos = pos + packetSize;
        }

        int unused = count - pos;
        for (int i = 0; i < unused; i++) {
            buffer[i] = buffer[pos + i];
        }
        count = unused;

        return packets;
    }


    private boolean isKnownType(byte type) {
        int t = type & 0xFF;
        return t == 0x00 || t == 0x01 || t == 0x02 || t == 0x05 || t == 0x21 || t == 0x32;
    }

    private boolean checksumIsGood(int start, int size) {
        int sum = 0;
        for (int i = start; i < start + size; i++) {
            sum += buffer[i];
        }
        return (sum & 0xFF) == 0;
    }
}