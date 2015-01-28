package com.konka.dhtsearch.bittorrentkad.net;


import java.io.IOException;

import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.msg.KadMessage;

public interface Communicator extends Runnable {

//	public void bind();
	public void send(Node to, KadMessage msg) throws IOException;
	public void shutdown(Thread serverThread);
	
}