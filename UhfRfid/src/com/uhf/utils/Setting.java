package com.uhf.utils;

/**
 * One adjustable field inside the reader's 27-byte "basic parameters" block (command 0x81).
 * Every position here was matched against the demo program's screen values.
 */
public enum Setting {
    WORK_MODE         ("Work mode (1 = Active, 2 = Passive)", 1,  "",        1, 2),
    AUTO_READ_INTERVAL("Auto read interval",                  3,  "x10 ms",  0, 255),
    AUTO_READ_DELAY   ("Auto read delay",                     4,  "s",       0, 255),
    BYTE_OFFSET       ("Byte offset",                         5,  "byte",    0, 255),
    OUT_INTERVAL      ("Out interval",                        6,  "x10 ms",  0, 255),
    PULSE_WIDTH       ("Pulse width",                         7,  "x10 us",  0, 255),
    PULSE_PERIOD      ("Pulse period",                        8,  "x100 us", 0, 255),
    SAME_ID_INTERVAL  ("Same ID interval",                    10, "x0.5 s",  0, 255),
    BUZZER            ("Buzzer (1 = on, 0 = off)",            11, "",        0, 1);

    public final String label;
    public final int index;       // position in the 27-byte block
    public final String unit;
    public final int min;
    public final int max;

    Setting(String label, int index, String unit, int min, int max) {
        this.label = label;
        this.index = index;
        this.unit = unit;
        this.min = min;
        this.max = max;
    }
}
