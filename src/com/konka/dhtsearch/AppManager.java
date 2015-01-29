package com.konka.dhtsearch;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.konka.dhtsearch.bittorrentkad.net.KadServer;

public class AppManager {
	private static MessageDispatcherManager messageDispatcherManager;
	private static KadServer kadServer;
	private static AppManager appManager;
	private static final Node localNode = new Node();

	public static Node getLocalNode() {
		return localNode;
	}

	public static AppManager getInstance() {
		if (appManager == null) {
			init();
		}
		return appManager;
	}

	private AppManager() {
		super();
		try {
			localNode.setInetAddress(InetAddress.getByName("0.0.0.0"));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		localNode.setPoint(5555);
	}

	public static void init() {
		appManager = new AppManager();
		messageDispatcherManager = new MessageDispatcherManager();
		DatagramSocket socket;
		try {
			socket = new DatagramSocket(localNode.getSocketAddress());
			kadServer = new KadServer(socket);
		} catch (SocketException e) {
			e.printStackTrace();
		}

	}

	public static KadServer getKadServer() {
		if (kadServer == null) {
			throw new IllegalArgumentException("必须先调用init方法进行初始化");
		}
		return kadServer;
	}

	public static MessageDispatcherManager getMessageDispatcherManager() {
		if (messageDispatcherManager == null) {
			throw new IllegalArgumentException("必须先调用init方法进行初始化");
		}
		return messageDispatcherManager;
	}

}
