package org.konkakjb.text;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.RandomKeyFactory;
import com.konka.dhtsearch.bittorrentkad.BootstrapNodesSaver;
import com.konka.dhtsearch.bittorrentkad.KadNet;
import com.konka.dhtsearch.bittorrentkad.concurrent.CompletionHandler;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeRequest;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeResponse;
import com.konka.dhtsearch.bittorrentkad.net.KadServer;
import com.konka.dhtsearch.bittorrentkad.net.MessageDispatcher;
import com.konka.dhtsearch.bittorrentkad.net.filter.IdMessageFilter;
import com.konka.dhtsearch.bittorrentkad.net.filter.TypeMessageFilter;
import com.konka.dhtsearch.util.Util;

public class SearchText {
	public static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

	public static void main(String[] args) {
		KadNet kadNet = null;
		try {
			AppManager.init();// 1---
			File nodesFile = new File(SearchText.class.getResource("/").getPath()); // 2---
			BootstrapNodesSaver bootstrapNodesSaver = new BootstrapNodesSaver(nodesFile);// 3---
			kadNet = new KadNet(bootstrapNodesSaver);
			kadNet.create();
			doth();
		} catch (Exception e) {
			e.printStackTrace();
			if (kadNet != null) {
				kadNet.shutdown();
			}
		}
	}

	public static void doth() throws Exception {
		InetAddress[] inetAddresss = { //
		Inet4Address.getByName("router.bittorrent.com"), //
				Inet4Address.getByName("dht.transmissionbt.com"), //
				Inet4Address.getByName("router.utorrent.com"), //
				Inet4Address.getByName("127.0.0.1") //
		};
		try {
			for (InetAddress inetAddress : inetAddresss) {
				Key key = new RandomKeyFactory(20, new Random(), "SHA-1").getZeroKey();
				Node localNode = new Node(key).setInetAddress(inetAddress).setPoint(5555);
				String t=Util.random_tranctionId();
				System.out.println("ddd"+t);
				FindNodeRequest findNodeResponse = new FindNodeRequest(t, localNode);
				sendFindNode(localNode, findNodeResponse);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void sendFindNode(final Node localNode, final FindNodeRequest findNodeResponse) {
		Timer timer = new Timer();
		KadServer kadServer = AppManager.getKadServer();
		final MessageDispatcher dispatcher = new MessageDispatcher(timer, kadServer, findNodeResponse.getTransaction());
		dispatcher.setConsumable(true)//
//				.addFilter(new IdMessageFilter(findNodeResponse.getTransaction()))// 只接受的类型
//				.addFilter(new TypeMessageFilter(FindNodeResponse.class))//
				.setCallback(null, new CompletionHandler<KadMessage, String>() {
					@Override
					public void completed(KadMessage msg, String nothing) {
						System.out.println("收到请求的响应"+msg);
					}
					@Override
					public void failed(Throwable exc, String nothing) {
						
					}
				});
		try {
			executor.execute(new Runnable() {
				@Override
				public void run() { 
					dispatcher.send(localNode, findNodeResponse);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
