package org.konkakjb.text;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.KadNet;
import com.konka.dhtsearch.db.exception.DhtException;

public class SearchText {
	private static final InetSocketAddress[] BOOTSTRAP_NODES = { //
	new InetSocketAddress("router.bittorrent.com", 6881), //
			new InetSocketAddress("dht.transmissionbt.com", 6881),//
			new InetSocketAddress("router.utorrent.com", 6881), };

	public static void main(String[] args) throws  DhtException {
		 int size = 3;
		 try {
		 for (int i = 0; i < size; i++) {
		 AppManager.init();// 1---
		 Key key = AppManager.getKeyFactory().generate();
		 Node localNode = new Node(key).setInetAddress(InetAddress.getByName("0.0.0.0")).setPoint(20200 + i);// 这里注意InetAddress.getLocalHost();为空
		 new KadNet(null, localNode).join(BOOTSTRAP_NODES).create();
		 }
		 } catch (Exception e) {
		 e.printStackTrace();
		 }
//		DhtInfoDao dao = DaoFactory.getPersonaDao();
//		DhtInfo dhtinfo = new DhtInfo();
//		dhtinfo.setInfo_hash("dddddddddddddddddddddddddd");
//		for (int i = 0; i < 100; i++) {
//			dao.insert(dhtinfo);
//		}
	}
}
