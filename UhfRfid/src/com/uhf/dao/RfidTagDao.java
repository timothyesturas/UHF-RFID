package com.uhf.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.uhf.manager.ApplicationManager;
import com.uhf.utils.RfidFrame;
import com.uhf.utils.TagCommands;
import com.uhf.utils.TagReply;

public class RfidTagDao implements TagDao {

    private static final int REPLY_TIMEOUT_MS = 1500;
    private static final int MORE_TAGS_WAIT_MS = 400;

    private final ApplicationManager app;
    private final Consumer<String> log;
    private volatile String lastReply = "";

    private final Map<String, String> progress = new HashMap<>();

    public RfidTagDao(ApplicationManager app, Consumer<String> log) {
        this.app = app;
        this.log = log;        
    }

    @Override
    public List<TagReply> findAll() {
        List<TagReply> tags = new ArrayList<>();
        app.clearReply();
        byte[] cmd = RfidFrame.build(0xFFFF, RfidFrame.CODE_READ_TAG, RfidFrame.TYPE_CMD, null);
        if (!app.getSerial().send(cmd)) {
            return tags;
        }
        long end = System.currentTimeMillis() + REPLY_TIMEOUT_MS;
        while (System.currentTimeMillis() < end) {
            RfidFrame f = app.getReplies().poll();
            if (f == null) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {  
                    e.printStackTrace();
                }
                continue;
            }
            if (f.code != RfidFrame.CODE_READ_TAG) continue;
            if (f.type == RfidFrame.TYPE_ERR) break;

            log.accept("[raw payload: " + RfidFrame.bytesToHex(f.payload, 0, f.payload.length) + "]");
            TagReply tag = TagReply.parse(f.payload);
            if (tag != null) {
                boolean known = tags.stream().anyMatch(t -> t.epcHex.equalsIgnoreCase(tag.epcHex));
                if (!known) tags.add(tag);
                end = Math.min(end, System.currentTimeMillis() + MORE_TAGS_WAIT_MS);
            }
        }
        return tags;
    }

    @Override
    public WriteResult write(TagReply tag, String newHex) {

        log.accept("========== RFID WRITE ==========");
        log.accept("[OLD EPC] " + tag.epcHex);
        log.accept("[NEW EPC] " + newHex);
        log.accept("[OLD EPC LENGTH] " + tag.epcHex.length() / 2);
        log.accept("[NEW EPC LENGTH] " + newHex.length() / 2);
        log.accept("[PC] " + String.format("%04X", tag.pc));

        if (tag.epcHex.length() == newHex.length()) {
            return writeWordByWord(tag, newHex);
        }

        if (!sendAndCheck(
                "select",
                TagCommands.select(tag.epcHex),
                RfidFrame.CODE_SELECT_TAG)) {

            log.accept("[RESULT] SELECT FAILED");
            return WriteResult.SELECT_FAILED;
        }

        boolean sameLength = tag.epcHex.length() == newHex.length();

        log.accept("[SAME LENGTH] " + sameLength);

        byte[] cmd = sameLength
                ? TagCommands.writeEpc(newHex)
                : TagCommands.writeEpcAndLength(newHex, tag.pc);

        boolean written = sendAndCheck(
                "write",
                cmd,
                RfidFrame.CODE_WRITE_TAG);

        String writeReply = lastReply;   

        sendAndCheck(
                "unselect",
                TagCommands.unselect(),
                RfidFrame.CODE_SELECT_TAG);

        lastReply = writeReply;

        log.accept("[RESULT] " + (written ? "WRITE OK" : "WRITE FAILED"));

        return written ? WriteResult.OK : WriteResult.WRITE_FAILED;
    }

    private WriteResult writeWordByWord(TagReply tag, String newHex) {
        String current = progress.getOrDefault(tag.epcHex, tag.epcHex).toUpperCase();
        String target = newHex.toUpperCase();

        for (int w = 0; w < target.length() / 4; w++) {
            String have = current.substring(w * 4, w * 4 + 4);
            String want = target.substring(w * 4, w * 4 + 4);
            if (have.equals(want)) continue;                      

            if (!sendAndCheck("select", TagCommands.select(current), RfidFrame.CODE_SELECT_TAG)) {
                log.accept("[RESULT] SELECT FAILED");
                return WriteResult.SELECT_FAILED;
            }

            boolean ok = sendAndCheck("write word " + (w + 1), TagCommands.writeEpcWords(w, want),
                    RfidFrame.CODE_WRITE_TAG);
            String writeReply = lastReply;

            sendAndCheck("unselect", TagCommands.unselect(), RfidFrame.CODE_SELECT_TAG);
            lastReply = writeReply;

            if (!ok) {
                log.accept("[RESULT] WRITE FAILED at word " + (w + 1));
                return WriteResult.WRITE_FAILED;
            }
            current = current.substring(0, w * 4) + want + current.substring(w * 4 + 4);
            progress.put(tag.epcHex, current);
        }
        progress.remove(tag.epcHex);
        log.accept("[RESULT] WRITE OK");
        lastReply = "write: OK";
        return WriteResult.OK;
    }

    @Override
    public String getLastReply() {
        return lastReply;
    }

    @Override
    public boolean verify(String expectedHex) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {  
                e.printStackTrace();
            }
            for (TagReply check : findAll()) {
                if (check.epcHex.equalsIgnoreCase(expectedHex)) return true;
            }
        }
        return false;
    }

    private boolean sendAndCheck(String name, byte[] cmd, byte code) {

        app.clearReply();

        log.accept("[" + name + "] Sending command: "
                + RfidFrame.bytesToHex(cmd, 0, cmd.length));

        if (!app.sendSerial(cmd)) {
            log.accept("[" + name + "] Serial send failed.");
            lastReply = name + ": could not send the command to the reader";
            return false;
        }

        long end = System.currentTimeMillis() + REPLY_TIMEOUT_MS;

        while (System.currentTimeMillis() < end) {

            RfidFrame f = app.getReplies().poll();

            if (f == null) {
            	try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {  
                    e.printStackTrace();
                }
                continue;
            }

            log.accept("[" + name + "] Received frame:"
                    + " code=" + String.format("%02X", f.code)
                    + " type=" + String.format("%02X", f.type)
                    + " payload=" + RfidFrame.bytesToHex(
                            f.payload, 0, f.payload.length));

            if (f.code == code) {

                if (f.type == RfidFrame.TYPE_OK) {
                    log.accept("[" + name + "] SUCCESS");
                    lastReply = name + ": OK";
                    return true;
                }

                log.accept("[" + name + "] READER RETURNED ERROR.");
                lastReply = name + ": the reader refused it (type=" + String.format("%02X", f.type)
                        + ", payload=" + RfidFrame.bytesToHex(f.payload, 0, f.payload.length) + ")";
                return false;
            }
        }

        log.accept("[" + name + "] TIMEOUT: No matching response.");
        lastReply = name + ": no answer from the reader (timeout)";
        return false;
    }

@Override
public String getNextReferenceNo(
        String table,
        String column,
        String dateFormat,
        String prefix,
        String businessUnitRefCode,
        Connection connection) {

    String sql =
        "SELECT DEV.GET_NEXT_REF_NO_BU(?, ?, ?, ?, ?) FROM DUAL";

    try {

        if (connection == null) {
            log.accept("[REF ERROR] Connection is NULL.");
            return null;
        }
        
        try (PreparedStatement stmt =
                connection.prepareStatement(sql)) {

            stmt.setString(1, table);
            stmt.setString(2, column);
            stmt.setString(3, dateFormat);
            stmt.setString(4, prefix);
            stmt.setString(5, businessUnitRefCode);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    String result = rs.getString(1);

                    log.accept("[REF RESULT] " + result);

                    if (result == null || result.trim().isEmpty()) {
                        log.accept("[REF ERROR] Function returned NULL.");
                        return null;
                    }

                    return result.trim();
                }
            }
        }

        log.accept("[REF ERROR] No result returned.");
        return null;

    } catch (SQLException e) {
        log.accept("[REF SQL ERROR] " + e.getMessage());
        throw new RuntimeException(
            "Unable to generate RFID reference number.", e
        );
    }
}


}
