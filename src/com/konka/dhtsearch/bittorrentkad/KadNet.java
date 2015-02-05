package com.konka.dhtsearch.bittorrentkad;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.KeyFactory;
import com.konka.dhtsearch.KeybasedRouting;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.RandomKeyFactory;
import com.konka.dhtsearch.bittorrentkad.bucket.Bucket;
import com.konka.dhtsearch.bittorrentkad.bucket.SlackBucket;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.net.KadSendMsgServer;
import com.konka.dhtsearch.bittorrentkad.net.KadServer;

public class KadNet implements KeybasedRouting {
	// private final KadBuckets findValueOperation;// 查找相识节点用

	private final KadServer kadServer;// = AppManager.getKadServer();// Runnable 主要是TODO KadServer
	private final KadSendMsgServer kadSendMsgServer;// = AppManager.getKadServer();// Runnable 主要是TODO KadServer
	private final Bucket kadBuckets;// = AppManager.getKadBuckets();// 路由表
	private final int bucketSize = 8;// 一个k桶大小
	private final BootstrapNodesSaver bootstrapNodesSaver;// 关机后保存到本地，启动时候从本地文件中加载
	// private final BlockingQueue<Node> nodesqueue = new LinkedBlockingDeque<Node>();
	private final KeyFactory keyFactory;
//	private final SlackBucket stableBucket;

	// KadBuckets kadBuckets=new KadBuckets(keyFactory, new StableBucket());
	/**
	 * @param findValueOperation
	 *            查找相识节点用
	 * @param kadServer
	 *            服务
	 * @param kadBuckets
	 *            路由表
	 * @param bootstrapNodesSaver
	 *            保存数据用
	 * @throws SocketException
	 * @throws NoSuchAlgorithmException
	 */
	public KadNet(BootstrapNodesSaver bootstrapNodesSaver) throws SocketException, NoSuchAlgorithmException {
		this.bootstrapNodesSaver = bootstrapNodesSaver;
		DatagramSocket socket = null;
		this.keyFactory = new RandomKeyFactory(20, new Random(), "SHA-1");
		socket = new DatagramSocket(AppManager.getLocalNode().getSocketAddress());
//		stableBucket = new SlackBucket(200);// 每一个
		this.kadBuckets = new SlackBucket(1000);// 这里要换成kadSendMsgServer

		this.kadSendMsgServer = new KadSendMsgServer(socket, kadBuckets);// 111111111
		this.kadServer = new KadServer(socket, kadBuckets);// 2222
		// 123顺序不能变
	}

	public KadServer getKadServer() {
		return kadServer;
	}

	public KadSendMsgServer getKadSendMsgServer() {
		return kadSendMsgServer;
	}

	@Override
	public void create() throws IOException {

//		kadBuckets.registerIncomingMessageHandler();

		kadServer.start();
		kadSendMsgServer.start();

		if (bootstrapNodesSaver != null) {
			bootstrapNodesSaver.load();
			bootstrapNodesSaver.start();
		}
	}

	/**
	 * 加入已知节点uri
	 */
	@Override
	public void join(Collection<URI> bootstraps) {
		// joinOperation.addBootstrap(bootstraps).doJoin();
	}

	@Override
	public List<Node> findNode(Key k) {// 根据k返回相似节点
		List<Node> result = kadBuckets.getClosestNodesByKey(k, 8);

		List<Node> $ = new ArrayList<Node>(result);

		if ($.size() > bucketSize)
			$.subList(bucketSize, $.size()).clear();

		return result;
	}

 

	@Override
	public Node getLocalNode() {
		return AppManager.getLocalNode();
	}

	// public SlackBucket getStableBucket() {
	// return stableBucket;
	// }

	@Override
	public String toString() {
		return getLocalNode().toString() + "\n" + kadBuckets.toString();
	}

	public  Bucket  getKadBuckets() {
		return kadBuckets;
	}

	@Override
	public void sendMessage(KadMessage msg) throws IOException {
		kadServer.send(msg);
	}

	@Override
	public void shutdown() {
		try {
			if (bootstrapNodesSaver != null) {
				bootstrapNodesSaver.saveNow();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		kadServer.shutdown();
		kadSendMsgServer.shutdown();
	}
}
