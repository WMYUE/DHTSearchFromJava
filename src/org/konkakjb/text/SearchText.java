package org.konkakjb.text;

import java.io.File;
import java.util.Random;
import java.util.Timer;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.MessageDispatcherManager;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.RandomKeyFactory;
import com.konka.dhtsearch.bittorrentkad.BootstrapNodesSaver;
import com.konka.dhtsearch.bittorrentkad.KadNet;
import com.konka.dhtsearch.bittorrentkad.bucket.KadBuckets;
import com.konka.dhtsearch.bittorrentkad.bucket.StableBucket;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeRequest;
import com.konka.dhtsearch.bittorrentkad.net.MessageDispatcher;

public class SearchText {
	public static void main(String[] args) {

		KadNet kadNet = null;
		try {
			AppManager.init();// 1---
			KadBuckets kadBuckets = new KadBuckets(new RandomKeyFactory(20, new Random(), "SHA-1"), new StableBucket());// 2---
			File nodesFile = new File(SearchText.class.getResource("/").getPath()); // 3---
			BootstrapNodesSaver bootstrapNodesSaver = new BootstrapNodesSaver(kadBuckets, nodesFile);
			// 创建kadNet
			kadNet = new KadNet(AppManager.getKadServer(), kadBuckets, bootstrapNodesSaver);
			kadNet.create();
			dosth(kadNet);
		} catch (Exception e) {
			e.printStackTrace();
			if (kadNet != null) {
				kadNet.shutdown();
			}
		}
		// kadNet.
	}

	private static void dosth(KadNet kadNet) {
		Node to=new Node();
//		FindNodeRequest req=new FindNodeRequest("transaction", src);
		MessageDispatcherManager messageDispatcherManager=AppManager.getMessageDispatcherManager();
		MessageDispatcher messageDispatcher=new MessageDispatcher(new Timer(), AppManager.getKadServer());
//		messageDispatcher.send(to, req);
//		messageDispatcherManager.addMessageDispatcher(messageDispatcher);
//		kadNet.sendMessage(to, tag, msg);
	}
}
