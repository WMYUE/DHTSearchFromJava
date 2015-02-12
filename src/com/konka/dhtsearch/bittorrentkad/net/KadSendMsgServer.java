package com.konka.dhtsearch.bittorrentkad.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.KadNode;
import com.konka.dhtsearch.bittorrentkad.bucket.Bucket;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeRequest;

/**
 * 发送消息查找node
 * 
 * @author 耳东 (cgp@0731life.com)
 *
 */
public class KadSendMsgServer implements Runnable {

	private final AtomicBoolean isActive = new AtomicBoolean(false);
	private final Thread startThread;
	private final Bucket kadBuckets;
	private final DatagramChannel channel;

	public KadSendMsgServer( Bucket kadBuckets, DatagramChannel channel) {
		startThread = new Thread(this);
		this.kadBuckets = kadBuckets;
		this.channel = channel;
	}

	/**
	 * 只发送findnode操作，其他请求请使用KadSendMsgServer
	 * 
	 * @param msg
	 *            要发送的消息（一般是具体实现）
	 * @throws IOException
	 *             any socket exception
	 */
	public void send(final KadMessage msg) throws IOException {
		try {
			if (msg.getSrc().equals(AppManager.getLocalNode())) {
				return;
			}
			byte[] buf = msg.getBencodeData();
			channel.send(ByteBuffer.wrap(buf), msg.getSrc().getSocketAddress());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 不停发送消息
	 */
	@Override
	public void run() {
		this.isActive.set(true);
		while (this.isActive.get()) {
			try {
				Thread.sleep(1000);
				List<KadNode> nodes = kadBuckets.getAllNodes();
				for (int i = 0; i < nodes.size(); i++) {
					KadNode node = nodes.get(i);
					if (!node.getNode().equals(AppManager.getLocalNode())) {
						send(node.getNode());
						// System.out.println(node.getNode().getKey().toString()+"--"+node.getNode().getSocketAddress());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void send(Node to) throws IOException {
		FindNodeRequest msg = FindNodeRequest.creatLocalFindNodeRequest(to);
		send(msg);
	}

	/**
	 * Shutdown the server and closes the socket 关闭服务
	 * 
	 * @param kadServerThread
	 */
	// @Override
	public void shutdown() {
		this.isActive.set(false);
		startThread.interrupt();
		try {
			startThread.join();
		} catch (final InterruptedException e) {
		}
	}

	public void start() {
		startThread.start();
	}
}
