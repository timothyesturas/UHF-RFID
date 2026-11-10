package com.uhf.connection;

import java.util.Arrays;

import com.fazecast.jSerialComm.SerialPort;
import com.uhf.listener.RawDataListener;

public class SerialPortConnection {
	
	PortDetails portDetails = PortDetails.getInstance();
	
	
	private volatile boolean running = false;
	
	private SerialPort serialPort;
	
	private RawDataListener listener;

	 public boolean connect(SerialPort port) {
		 
		 if (running) {
	            disconnect();
	        }
		 serialPort = port;
	        
	        
	        if(!portDetails.openPort()) {
	        	System.err.println("Could not open " + serialPort.getSystemPortName() + " (is another program using it?)");
	            return false;
	        }

	        running = true;
            Thread readerThread = new Thread(() -> readLoop());
	        readerThread.setDaemon(true); 
	        readerThread.start();
	        return true;
	    }
	 
	 public void setListener(RawDataListener listener) {
	        this.listener = listener;
	    }
	 
	 
	    public void disconnect() {
	        running = false;
	        if (serialPort != null) {
	        	serialPort.closePort();
	            System.out.println("SerialPort Disconnected");
	        }
	    }
	    
	  public boolean isSerialPortExists() {
		  
		  return serialPort != null;
	  }

	    private void readLoop() {
	        byte[] box = new byte[1024];



	        while (running) {
	            
	            int howMany = serialPort.readBytes(box, box.length);

	            if (howMany < 0) {
	                running = false;
	                break;
	            }

	            if (howMany > 0 && listener != null) {
	                try {
	                    byte[] received = Arrays.copyOf(box, howMany);  
	                    listener.onData(received);
	                } catch (RuntimeException e) {
	                    System.err.println("Problem handling received data: " + e);
	                }
	            }
	        }
	    }
	    
	    public boolean send(byte[] data) {
	        if (serialPort == null || !serialPort.isOpen()) {
	            System.err.println("Not connected - cannot send");
	            return false;
	        }
	        return serialPort.writeBytes(data, data.length) == data.length;
	    }
	 
}
