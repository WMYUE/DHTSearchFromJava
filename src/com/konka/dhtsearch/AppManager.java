package com.konka.dhtsearch;

import java.net.InetAddress;
import java.util.Random;

public class AppManager {
	private  static AppManager appManager;
	private static Node localNode;

	private static KeyFactory keyFactory;

	public static Node getLocalNode() {
		return localNode;
	}

	public static AppManager getInstance() {
		if (appManager == null) {
			init();
		}
		return appManager;
	}

	public static KeyFactory getKeyFactory() {
		return keyFactory;
	}

	private AppManager() {
		super();
		try {
			keyFactory = new RandomKeyFactory(20, new Random(), "SHA-1");
			Key key = keyFactory.generate();
			localNode = new Node(key);
			localNode.setInetAddress(InetAddress.getByName("0.0.0.0"));// 这里注意InetAddress.getLocalHost();为空
			localNode.setPoint(9500);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void init() {
		appManager = new AppManager();
	}

}
