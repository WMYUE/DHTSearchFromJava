package org.konkakjb.text;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.RandomKeyFactory;
import com.konka.dhtsearch.bittorrentkad.KadNet;
import com.konka.dhtsearch.bittorrentkad.KadNode;
import com.konka.dhtsearch.bittorrentkad.concurrent.CompletionHandler;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeRequest;
import com.konka.dhtsearch.bittorrentkad.net.KadSendMsgServer;
import com.konka.dhtsearch.bittorrentkad.net.MessageDispatcher;
import com.konka.dhtsearch.util.Util;

public class SearchText {
	public static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(3);

	public static void main(String[] args) {
		KadNet kadNet = null;
		try {
			AppManager.init();// 1---
			// File nodesFile = new File(SearchText.class.getResource("/").getPath()); // 2---
			// BootstrapNodesSaver bootstrapNodesSaver = new BootstrapNodesSaver(nodesFile);// 3---
			// kadNet = new KadNet(bootstrapNodesSaver);
			kadNet = new KadNet(null);
			kadNet.create();
			doth(kadNet);
		} catch (Exception e) {
			e.printStackTrace();
			if (kadNet != null) {
				kadNet.shutdown();
			}
		}
	}

	public static void doth(KadNet kadNet) throws Exception {
		InetAddress[] inetAddresss = { //
		InetAddress.getByName("router.bittorrent.com"), //
				InetAddress.getByName("dht.transmissionbt.com"), //
				InetAddress.getByName("router.utorrent.com"), //
//				Inet4Address.getByName("127.0.0.1") //
		};
		try {
			for (InetAddress inetAddress : inetAddresss) {
				Key key = new RandomKeyFactory(20, new Random(), "SHA-1").generate();
//				key.setKeyid("dddd");
				Node localNode = new Node(key).setInetAddress(inetAddress).setPoint(6881);
//				String t = Util.random_tranctionId();
//				System.out.println("ddd" + t);
//				FindNodeRequest findNodeResponse = new FindNodeRequest(t, localNode);
				kadNet.getKadBuckets().insert(new KadNode().setNode(localNode).setNodeWasContacted());
//				sendFindNode(localNode, findNodeResponse, kadNet);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void sendFindNode(final Node localNode, final FindNodeRequest findNodeResponse, KadNet kadNet) {
		Timer timer = new Timer();
		KadSendMsgServer kadServer = kadNet.getKadSendMsgServer();
		// try {
		// kadServer.send(localNode, findNodeResponse);
		// } catch (IOException e1) {
		// e1.printStackTrace();
		// }
		final MessageDispatcher dispatcher = new MessageDispatcher(timer, kadServer, findNodeResponse.getTransaction());
		dispatcher.setConsumable(true)//
				// .addFilter(new IdMessageFilter(findNodeResponse.getTransaction()))// 只接受的类型
				// .addFilter(new TypeMessageFilter(FindNodeResponse.class))//
				.setCallback(null, new CompletionHandler<KadMessage, String>() {
					@Override
					public void completed(KadMessage msg, String nothing) {
//						System.out.println("收到请求的响应" + msg);
					}

					@Override
					public void failed(Throwable exc, String nothing) {

					}
				});
		try {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					dispatcher.send(findNodeResponse);
					
				}
			});
			// dispatcher.f
			// ExecutorCompletionService d;
			// d.
			// executor.submit(task);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
