package com.konka.dhtsearch.bittorrentkad.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.yaircc.torrent.bencoding.BDecodingException;
import org.yaircc.torrent.bencoding.BEncodedInputStream;
import org.yaircc.torrent.bencoding.BMap;
import org.yaircc.torrent.bencoding.BTypeException;

import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.ErrorKadResponse;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.KadRequest;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeRequest;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeResponse;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.ReceiveFindNodeResponse;
import com.konka.dhtsearch.bittorrentkad.krpc.get_peers.GetPeersRequest;
import com.konka.dhtsearch.bittorrentkad.krpc.ping.PingRequest;
import com.konka.dhtsearch.util.Util;

/**
 * 守护线程，负责接受和发送消息
 * 
 */
public class KadServer implements Runnable {

	private final DatagramSocket socket;
	private final BlockingQueue<DatagramPacket> pkts = new LinkedBlockingDeque<DatagramPacket>();;
	private final BlockingQueue<Node> nodesqueue;// = new LinkedBlockingDeque<Node>();;
	private final ExecutorService srvExecutor = new ScheduledThreadPoolExecutor(10);
	private final AtomicBoolean isActive = new AtomicBoolean(false);
	private final Thread startThread;// ;=new Thread();

	public KadServer(DatagramSocket socket, BlockingQueue<Node> nodesqueue) {
		this.socket = socket;
		this.nodesqueue = nodesqueue;
		startThread = new Thread(this);
	}

	/**
	 * 真正发送网络数据报消息
	 * 
	 * @param to
	 *            目的地的节点
	 * @param msg
	 *            要发送的消息（一般是具体实现）
	 * @throws IOException
	 *             any socket exception
	 */
	// @Override
	public void send(final KadMessage msg) throws IOException {
		try {
			byte[] buf = msg.getBencodeData();
			final DatagramPacket pkt = new DatagramPacket(buf, 0, buf.length);

			pkt.setSocketAddress(msg.getSrc().getSocketAddress());
			this.socket.send(pkt);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 收到信息后处理
	private void handleIncomingPacket(final DatagramPacket pkt) {
		this.srvExecutor.execute(new Runnable() {// 交给线程池处理
					@Override
					public void run() {
						try {// 这里处理消息的方法需要重写
							BMap bMap = (BMap) BEncodedInputStream.bdecode(pkt.getData());
							Node src = new Node().setInetAddress(pkt.getAddress());// InetAddress;//对方的node信息
							String transaction = bMap.getString("t");// 交互用的识别id
							if (bMap.containsKey("y")) {
								String y = bMap.getString("y");
								if ("q".equals(y)) {// 对方请求
									handleRequestMsg(pkt, bMap, src, transaction);
								} else if ("r".equals(y)) {// 对方的响应（由于值爬数据，不用处理太复杂）
									handleResponseMsg(bMap, src, transaction);
								}
							}
						} catch (final Exception e) {
							e.printStackTrace();
							return;
						} finally {
							KadServer.this.pkts.offer(pkt);// 如果可以，将ptk加入到队列
						}
					}

				});
	}

	/**
	 * 回复Ping请求
	 * 
	 * @param bMap
	 */
	protected void hanldePingRequest(BMap bMap) {
	}

	/**
	 * 回复Get_Peers请求
	 * 
	 * @param bMap
	 */
	protected void handleGet_PeersRequest(BMap bMap) {
	}

	/**
	 * 处理响应信息
	 * 
	 * @param bMap
	 * @param src
	 * @param transaction
	 * @param messageDispatcher
	 * @throws BTypeException
	 * @throws BDecodingException
	 * @throws UnknownHostException
	 */
	private synchronized void handleResponseMsg(BMap bMap, Node src, String transaction) throws BTypeException, BDecodingException, UnknownHostException {

		
	
		
		
		
		MessageDispatcher messageDispatcher = MessageDispatcher.findMessageDispatcherByTag(transaction);// 取出之前的请求对象
		if (messageDispatcher != null) {// 有记录
			KadRequest kadRequest = messageDispatcher.getKadRequest();
			if (kadRequest.getClass() == FindNodeRequest.class) {
				// FindNodeResponse findNodeResponse = receiveFind_Node(transaction, bMap, src);
				ReceiveFindNodeResponse receiveFindNodeResponse = new ReceiveFindNodeResponse(transaction, bMap, src);
				messageDispatcher.handle(receiveFindNodeResponse);

				handlehindNodeResponse(bMap);//*****

			} else if (kadRequest.getClass() == PingRequest.class) {
				if (bMap.containsKey("r")) {
					BMap bmap_r=bMap.getMap("r");
					if(bmap_r.containsKey("id")){
						String id=bmap_r.getString("id");
						messageDispatcher.handle(kadRequest);
					}
				}
			} else if (kadRequest.getClass() == GetPeersRequest.class) {

			} else {
				// TODO 响应的操作应该根据请求的id t判断是哪个响应，t清楚一次必须改变
			}
			// messageDispatcher.handle(msg);
		} else {// 没有记录就按照大众处理
			handlehindNodeResponse(bMap);//*****
		}
	}
	/**
	 * 只接受findNode的响应
	 * @param bMap
	 * @throws UnknownHostException
	 * @throws BTypeException
	 */
	private void handlehindNodeResponse(BMap bMap) throws UnknownHostException, BTypeException{
		if (bMap.containsKey("r")) {
			BMap bmap_r=bMap.getMap("r");
			if(bmap_r.containsKey("nodes")){
				byte[] bb = (byte[])bmap_r.get("nodes");
				passNodes(bb);
			}
		}
	}

	private void passNodes(byte[] bb) throws UnknownHostException {
		// byte[] bb = (byte[]) bMap.getMap("r").get("nodes");
		int count = bb.length / 26;
		if (count != 0) {
			return;
		}
		for (int i = 0; i < count; i++) {
			byte[] nid = Arrays.copyOfRange(bb, i * 26, i * 26 + 20);
			byte[] ip = Arrays.copyOfRange(bb, i * 26 + 20, i * 26 + 24);
			byte[] p = Arrays.copyOfRange(bb, i * 26 + 24, i * 26 + 26);

			InetAddress inet4Address = InetAddress.getByAddress(ip);
			Node node = new Node(new Key(nid));
			node.setInetAddress(inet4Address).setPoint(Util.bytesToInt(p));
			addToQueue(node);
		}
	}

	/**
	 * 回复find_node请求
	 * 
	 * @param bMap
	 */
	private void handleFind_NodeRequest(BMap bMap) {

	}

	private void addToQueue(Node node) {
		if (!nodesqueue.contains(node)) {
			nodesqueue.add(node);
		}
	}

	/**
	 * 处理请求信息
	 * 
	 * @param bMap
	 * @throws BTypeException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */

	protected synchronized void handleRequestMsg(DatagramPacket pkt, BMap bMap, Node src, String transaction) throws BTypeException, NoSuchAlgorithmException, IOException {
		if (bMap.containsKey("q")) {
			String q = bMap.getString("q");// find_node or getpeers===
			switch (q) {
				case "find_node":
					handleFind_NodeRequest(bMap);
					break;
				case "get_peers":
					handleGet_PeersRequest(bMap);
					break;
				case "ping"://
					hanldePingRequest(bMap);
					break;
				default:
					break;
			}

			String id = bMap.getMap("a").getString("id");
			Key key = new Key(id.getBytes());
			final Node to = new Node(key);
			to.setInetAddress(pkt.getAddress());
			to.setPoint(pkt.getPort());

			final ErrorKadResponse errorKadResponse = new ErrorKadResponse(transaction, to);
			addToQueue(to);
			srvExecutor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						send(errorKadResponse);// 都回复错误的响应
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		} else {
			return;
		}
	}

	/**
	 * The server loop:
	 * 
	 * @category accept a message from socket
	 * @category parse message
	 * @category handle the message in a thread pool 这个线程用来接受信息
	 */
	@Override
	public void run() {
		this.isActive.set(true);

		while (this.isActive.get()) {
			DatagramPacket pkt = null;
			try {
				// System.out.println("等待数据");
				pkt = this.pkts.poll();

				if (pkt == null)
					pkt = new DatagramPacket(new byte[1024 * 64], 1024 * 64);

				this.socket.receive(pkt);// 堵塞
				// System.out.println("已经拿到数据可");
				handleIncomingPacket(pkt);// 收到信息后处理

			} catch (final Exception e) {
				// insert the taken pkt back
				if (pkt != null)
					this.pkts.offer(pkt);

				e.printStackTrace();
			}

		}
	}

	/**
	 * Shutdown the server and closes the socket 关闭服务
	 * 
	 * @param kadServerThread
	 */
	// @Override
	public void shutdown() {
		this.isActive.set(false);
		this.socket.close();
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
