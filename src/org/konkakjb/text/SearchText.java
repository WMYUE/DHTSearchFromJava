package org.konkakjb.text;

import java.net.InetSocketAddress;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.bittorrentkad.KadNet;

public class SearchText {
	private static final InetSocketAddress[] BOOTSTRAP_NODES = { //
		InetSocketAddress.createUnresolved("router.bittorrent.com", 6881), //
		InetSocketAddress.createUnresolved("dht.transmissionbt.com", 6881), //
		InetSocketAddress.createUnresolved("router.utorrent.com", 6881) };
	public static void main(String[] args) {
 
		KadNet kadNet = null;
		try {
			AppManager.init();// 1---
			kadNet = new KadNet(null);
			kadNet.create();
			kadNet.join(BOOTSTRAP_NODES);
		} catch (Exception e) {
			e.printStackTrace();
			if (kadNet != null) {
				kadNet.shutdown();
			}
		}
	}
 
}
