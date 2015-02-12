package com.konka.dhtsearch.bittorrentkad;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.KeybasedRouting;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.bucket.Bucket;
import com.konka.dhtsearch.bittorrentkad.bucket.SlackBucket;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.net.KadSendMsgServer;
import com.konka.dhtsearch.bittorrentkad.net.KadReceiveServer;

/**
 * KadNet
 * 
 * @author 耳东 (cgp@0731life.com)
 *
 */
public class KadNet implements KeybasedRouting {
	// private final KadBuckets findValueOperation;// 查找相识节点用

	private final KadReceiveServer kadServer;// = AppManager.getKadServer();// Runnable 主要是TODO KadServer
	private final KadSendMsgServer kadSendMsgServer;// = AppManager.getKadServer();// Runnable 主要是TODO KadServer
	private final Bucket kadBuckets;// = AppManager.getKadBuckets();// 路由表
	private final int bucketSize = 8;// 一个k桶大小
	private final BootstrapNodesSaver bootstrapNodesSaver;// 关机后保存到本地，启动时候从本地文件中加载

	/**
	 * @param findValueOperation
	 *            查找相识节点用
	 * @param kadServer
	 *            服务
	 * @param kadBuckets
	 *            路由表
	 * @param bootstrapNodesSaver
	 *            保存数据用
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public KadNet(BootstrapNodesSaver bootstrapNodesSaver) throws NoSuchAlgorithmException, IOException {
		this.bootstrapNodesSaver = bootstrapNodesSaver;
		DatagramSocket socket = null;
		Selector selector = null;
		// -----------------------------------------------------------------------
		DatagramChannel channel = DatagramChannel.open();
		socket = channel.socket();
		channel.configureBlocking(false);
		socket.bind(AppManager.getLocalNode().getSocketAddress());
		selector = Selector.open();
		channel.register(selector, SelectionKey.OP_READ);
		// -----------------------------------------------------------------------

		this.kadBuckets = new SlackBucket(1000);

		this.kadSendMsgServer = new KadSendMsgServer(kadBuckets, channel);// 111111111
		this.kadServer = new KadReceiveServer(kadBuckets, selector, this);// 2222
		socket.getRemoteSocketAddress();

	}

	public KadReceiveServer getKadServer() {
		return kadServer;
	}

	public KadSendMsgServer getKadSendMsgServer() {
		return kadSendMsgServer;
	}

	@Override
	public void create() throws IOException {

		// kadBuckets.registerIncomingMessageHandler();

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
	public void join(KadNode... kadNodes) {
		for (KadNode kadNode : kadNodes) {
			kadBuckets.insert(kadNode);
		}
	}

	public void join(InetSocketAddress... inetSocketAddresses) {
		for (InetSocketAddress socketAddress : inetSocketAddresses) {
			Key key = AppManager.getKeyFactory().generate();
			Node localNode = new Node(key).setSocketAddress(socketAddress);
			join(new KadNode().setNode(localNode).setNodeWasContacted());
		}
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

	@Override
	public String toString() {
		return getLocalNode().toString() + "\n" + kadBuckets.toString();
	}

	public Bucket getKadBuckets() {
		return kadBuckets;
	}

	@Override
	public void sendMessage(KadMessage msg) throws IOException {
		kadSendMsgServer.send(msg);
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
