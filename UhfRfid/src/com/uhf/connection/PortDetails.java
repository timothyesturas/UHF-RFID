package com.uhf.connection;

import com.fazecast.jSerialComm.SerialPort;

public class PortDetails {

    private static final PortDetails INSTANCE = new PortDetails();

    private PortDetails() {}

    public static PortDetails getInstance() {
        return INSTANCE;
    }

    private SerialPort serialPort = null;
    private int baudRate = 57600;

    public SerialPort findPort(String descriptivePortName) {

        SerialPort[] ports = SerialPort.getCommPorts();

        for (SerialPort port : ports) {

            System.out.println(
                port.getSystemPortName() + " : " +
                port.getDescriptivePortName()
            );

            if (port.getDescriptivePortName().contains(descriptivePortName)
                    || port.getSystemPortName()
                           .equalsIgnoreCase(descriptivePortName)) {

                serialPort = port;
                return port;
            }
        }

        return null;
    }

    public boolean openPort() {

        if (serialPort == null) {
            System.out.println("No serial port selected!");
            return false;
        }

        if (serialPort.isOpen()) {
            System.out.println("Serial port is already open!");
            return true;
        }

        serialPort.setComPortParameters(
            baudRate,
            8,
            SerialPort.ONE_STOP_BIT,
            SerialPort.NO_PARITY
        );

        serialPort.setComPortTimeouts(
            SerialPort.TIMEOUT_READ_SEMI_BLOCKING |
            SerialPort.TIMEOUT_WRITE_BLOCKING,
            1000,
            0
        );

        boolean opened = serialPort.openPort();

        if (!opened) {

            int errorCode = serialPort.getLastErrorCode();
            int errorLocation = serialPort.getLastErrorLocation();

            System.out.println("Open failed!");
            System.out.println("Error code: " + errorCode);
            System.out.println("Error location: " + errorLocation);

            System.out.println("Port name: "
                    + serialPort.getSystemPortName());

            System.out.println("Is open: "
                    + serialPort.isOpen());

        }

        return opened;
    }

    public void closePort() {

        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            System.out.println("Serial port closed.");
        }
    }

    public SerialPort getSerialPort() {
        return serialPort;
    }
}