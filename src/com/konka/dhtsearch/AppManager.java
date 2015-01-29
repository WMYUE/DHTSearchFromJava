package com.konka.dhtsearch;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.konka.dhtsearch.bittorrentkad.net.KadServer;

public class AppManager {
	private static MessageDispatcherManager messageDispatcherManager;
	private static KadServer kadServer;

	public AppManager() {
		super();
		 init();
	}

	public static KadServer getKadServer() {
		return kadServer;
	}

	private void init() {
		messageDispatcherManager = new MessageDispatcherManager();
		DatagramSocket socket;
		try {
			socket = new DatagramSocket(5555, InetAddress.getByName("0.0.0.0"));
			kadServer = new KadServer(socket);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public static MessageDispatcherManager getMessageDispatcherManager() throws Exception {
		if (messageDispatcherManager == null) {
			throw new Exception("必须先初始化");
		}
		return messageDispatcherManager;
	}

}
