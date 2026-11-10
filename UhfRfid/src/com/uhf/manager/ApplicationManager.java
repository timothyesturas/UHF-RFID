package com.uhf.manager;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.uhf.connection.SerialPortConnection;
import com.uhf.utils.FrameBuffer;
import com.uhf.utils.RfidFrame;

public class ApplicationManager {

    private static final ApplicationManager INSTANCE = new ApplicationManager();
	
    private SerialPortConnection serial = new SerialPortConnection();

    private FrameBuffer frameBuffer = new FrameBuffer();

    private ConcurrentLinkedQueue<RfidFrame> replies = new ConcurrentLinkedQueue<>();
    
	private ApplicationManager() {	}

	public void clearReply() {
		replies.clear();
	}

	public boolean sendSerial(byte[] data) {
		return serial.send(data);
	}
	
	public static ApplicationManager getInstance() {
		return INSTANCE;
	}

	public SerialPortConnection getSerial() {
		return serial;
	}

	public FrameBuffer getFrameBuffer() {
		return frameBuffer;
	}


	public ConcurrentLinkedQueue<RfidFrame> getReplies() {
		return replies;
	}




	
	
	
	
	
    
    
	
}
