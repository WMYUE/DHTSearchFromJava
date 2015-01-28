package com.konka.dhtsearch.bittorrentkad.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;

/**
 * Serialize a message into a gun-ziped json message
 * 
 * @author eyal.kibbar@gmail.com
 * 
 */
public class JsonKadSerializer extends KadSerializer  {

	@Override
	public KadMessage read(InputStream in) throws IOException, ClassCastException, ClassNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(KadMessage msg, OutputStream out) throws IOException {
		// TODO Auto-generated method stub
		
	}
}
