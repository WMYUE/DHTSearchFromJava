package com.konka.dhtsearch.bittorrentkad;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
import com.konka.dhtsearch.bittorrentkad.net.KadParserTorrentServer;
import com.konka.dhtsearch.bittorrentkad.net.KadReceiveServer;
import com.konka.dhtsearch.bittorrentkad.net.KadSendMsgServer;

/**
 * KadNet
 * 
 * @author 耳东 (cgp@0731life.com)
 *
 */
public class KadNet implements KeybasedRouting {
	private final KadReceiveServer kadServer;// 接受消息
	private final KadSendMsgServer kadSendMsgServer;// 发生消息
	private final KadParserTorrentServer kadParserTorrentServer;//  解析种子
	private final Bucket kadBuckets;// = AppManager.getKadBuckets();// 路由表
	private final int BUCKETSIZE = 8;// 一个k桶大小
	private final BootstrapNodesSaver bootstrapNodesSaver;// 关机后保存到本地，启动时候从本地文件中加载
	private final DatagramChannel channel;
	private final Node localnode;

	/**
	 * @param bootstrapNodesSaver
	 * @param localnode
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public KadNet(BootstrapNodesSaver bootstrapNodesSaver, Node localnode) throws NoSuchAlgorithmException, IOException {
		this.bootstrapNodesSaver = bootstrapNodesSaver;
		DatagramSocket socket = null;
		Selector selector = null;
		this.localnode = localnode;
		// -----------------------------------------------------------------------
		channel = DatagramChannel.open();
		socket = channel.socket();
		channel.configureBlocking(false);
		socket.bind(localnode.getSocketAddress());
		selector = Selector.open();
		channel.register(selector, SelectionKey.OP_READ);
		// -----------------------------------------------------------------------

		this.kadBuckets = new SlackBucket(1000);

		this.kadSendMsgServer = new KadSendMsgServer(this);
		this.kadServer = new KadReceiveServer(selector, this);
		this.kadParserTorrentServer = new KadParserTorrentServer();
//		Thread.currentThread().setDaemon(true);
	}

	public void addNodeToBuckets(Node node) {
		if (!node.equals(localnode)) {
			kadBuckets.insert(new KadNode().setNode(node).setNodeWasContacted());// 插入一个节点
		}
	}

	@Override
	public void create() throws IOException {

//		kadServer.start();
//		kadSendMsgServer.start();
		kadParserTorrentServer.start();
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

	public List<KadNode> getAllNodes() {
		return kadBuckets.getAllNodes();
	}

	public KadNet join(InetSocketAddress... inetSocketAddresses) {
		for (InetSocketAddress socketAddress : inetSocketAddresses) {
			Key key = AppManager.getKeyFactory().generate();
			Node node = new Node(key).setSocketAddress(socketAddress);
			join(new KadNode().setNode(node).setNodeWasContacted());
		}
		return this;
	}

	@Override
	public List<Node> findNode(Key k) {// 根据k返回相似节点
		List<Node> result = kadBuckets.getClosestNodesByKey(k, BUCKETSIZE);

		List<Node> $ = new ArrayList<Node>(result);

		if ($.size() > BUCKETSIZE)
			$.subList(BUCKETSIZE, $.size()).clear();

		return result;
	}

	@Override
	public void sendMessage(KadMessage msg) throws IOException {
		if (msg.getSrc().equals(localnode)) {
			return;
		}
		try {
			byte[] buf = msg.getBencodeData(localnode);
			channel.send(ByteBuffer.wrap(buf), msg.getSrc().getSocketAddress());
		} catch (Exception e) {
			e.printStackTrace();
		}
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
