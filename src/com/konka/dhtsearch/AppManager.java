package com.konka.dhtsearch;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

import com.konka.dhtsearch.bittorrentkad.bucket.KadBuckets;
import com.konka.dhtsearch.bittorrentkad.bucket.StableBucket;
import com.konka.dhtsearch.bittorrentkad.net.KadServer;

public class AppManager {
	private static MessageDispatcherManager messageDispatcherManager;
//	private static KadServer kadServer;
	private static AppManager appManager;
	private static Node localNode;
//	private static KadBuckets kadBuckets;//路由

//	public static KadBuckets getKadBuckets() {
//		return kadBuckets;
//	}

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
			KeyFactory keyFactory=new RandomKeyFactory(20, new Random(), "SHA-1");
			Key key = keyFactory.generate();
			localNode = new Node(key);
			localNode.setInetAddress(InetAddress.getByName("0.0.0.0"));//这里注意InetAddress.getLocalHost();为空
//			localNode.setInetAddress(InetAddress.getByName("0.0.0.0"));//这里注意InetAddress.getLocalHost();为空
			localNode.setPoint(9500);
			
//			kadBuckets=new KadBuckets(keyFactory,  new StableBucket());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void init() {
		appManager = new AppManager();
		messageDispatcherManager = new MessageDispatcherManager();
		DatagramSocket socket;
		try {
//			socket = new DatagramSocket(9500);
			socket = new DatagramSocket(localNode.getSocketAddress());
//			kadServer = new KadServer(socket);
		} catch (SocketException e) {
			e.printStackTrace();
		}

	}
//
//	public static KadServer getKadServer() {
//		if (kadServer == null) {
//			throw new IllegalArgumentException("必须先调用init方法进行初始化");
//		}
//		return kadServer;
//	}

	public static MessageDispatcherManager getMessageDispatcherManager() {
		if (messageDispatcherManager == null) {
			throw new IllegalArgumentException("必须先调用init方法进行初始化");
		}
		return messageDispatcherManager;
	}

}
