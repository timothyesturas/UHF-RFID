package com.uhf.utils;

import com.fazecast.jSerialComm.SerialPort;

public class Utils {

	  public static void showDetails(SerialPort serialPort) {
		  
		  System.out.println("serialPort.getDescriptivePortName: "+ serialPort.getDescriptivePortName());
	      System.out.println("serialPort.getBaudRate: " + serialPort.getBaudRate());
	      System.out.println("serialPort.getNumDataBits: " + serialPort.getNumDataBits());
	      System.out.println("serialPort.getDeviceReadBufferSize: " + serialPort.getDeviceReadBufferSize());
	      System.out.println("serialPort.getDeviceWriteBufferSize: " + serialPort.getDeviceWriteBufferSize());
	      System.out.println("serialPort.getFlowControlSettings: "+ serialPort.getFlowControlSettings());
		  
	  }
}
