package com.konka.dhtsearch.bittorrentkad.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.yaircc.torrent.bencoding.BDecodingException;
import org.yaircc.torrent.bencoding.BEncodedInputStream;
import org.yaircc.torrent.bencoding.BMap;
import org.yaircc.torrent.bencoding.BTypeException;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.KadNet;
import com.konka.dhtsearch.bittorrentkad.KadNode;
import com.konka.dhtsearch.bittorrentkad.bucket.Bucket;
import com.konka.dhtsearch.bittorrentkad.concurrent.CompletionHandler;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.KadRequest;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeRequest;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeResponse;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.ReceiveFindNodeResponse;
import com.konka.dhtsearch.bittorrentkad.krpc.get_peers.GetPeersRequest;
import com.konka.dhtsearch.bittorrentkad.krpc.get_peers.GetPeersResponse;
import com.konka.dhtsearch.bittorrentkad.krpc.ping.PingRequest;
import com.konka.dhtsearch.bittorrentkad.krpc.ping.PingResponse;
import com.konka.dhtsearch.util.Util;

/**
 * 守护线程，负责接受和发送消息
 * 
 */
public class KadServer implements Runnable {

	private final DatagramSocket socket;
	private final BlockingQueue<DatagramPacket> pkts = new LinkedBlockingDeque<DatagramPacket>();;
	// private final BlockingQueue<Node> nodesqueue;// = new
	// LinkedBlockingDeque<Node>();;
	private final ExecutorService srvExecutor = new ScheduledThreadPoolExecutor(
			1);
	private final AtomicBoolean isActive = new AtomicBoolean(false);
	private final Thread startThread;// ;=new Thread();
	private final Bucket kadBuckets;
	private final Set<String> info_hashset = new HashSet<String>();

	public KadServer(DatagramSocket socket, Bucket kadBuckets) {
		this.socket = socket;
		startThread = new Thread(this);
		this.kadBuckets = kadBuckets;

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
			if (msg.getSrc().equals(AppManager.getLocalNode())) {
				return;
			}
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
							BMap bMap = (BMap) BEncodedInputStream.bdecode(pkt
									.getData());

							// System.out.println(bMap);
							Node src = new Node().setInetAddress(pkt
									.getAddress());// InetAddress;//对方的node信息
							// bMap.get
							// String transaction =Util.hex((byte[])
							// bMap.get("t"));
							// Hash hash = new Hash((byte[]) bMap.get("t"),
							// Type.ID);
							String transaction = Util.hex((byte[]) (bMap
									.get("t")));
							// System.out.println("transaction-hash=" +
							// transaction);
							// System.out.println("transaction-hex==" +
							// Util.hex((byte[]) (bMap.get("t"))));
							// System.out.println();
							// String transaction = bMap.getString("t");//
							// 交互用的识别id 收到的是二进制数组，要转码

							if (bMap.containsKey("y")) {
								String y = bMap.getString("y");
								if ("q".equals(y)) {// 对方请求
									// System.out.println("收到请求 " + bMap + "---"
									// + pkt.getSocketAddress());
									handleRequestMsg(pkt, bMap, transaction);
								} else if ("r".equals(y)) {// 对方的响应（由于值爬数据，不用处理太复杂）
									// Map<Thread, StackTraceElement[]> maps =
									// Thread.getAllStackTraces();
									// System.out.println("收到响应+线程数" + "-----" +
									// bMap + "---" + pkt.getSocketAddress());
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
	 * @throws BTypeException
	 * @throws IOException
	 */
	protected void hanldePingRequest(String transaction, BMap bMap, Node src)
			throws BTypeException, IOException {
		// byte[] keybyte = (byte[]) bMap.getMap("a").get("id");
		// Node src = new Node(new Key(keybyte));
		PingResponse pingResponse = new PingResponse(transaction, src);
		// pingResponse.getBencodeData();
		send(pingResponse);
		addToQueue(src);
	}

	/**
	 * 回复find_node请求
	 * 
	 * @param bMap
	 * @throws BTypeException
	 * @throws IOException
	 */
	private void handleFind_NodeRequest(String transaction, BMap bMap, Node src)
			throws BTypeException, IOException {
		byte[] target = (byte[]) bMap.getMap("a").get("target");
		List<Node> lists = kadBuckets.getClosestNodesByKey(new Key(target), 8);

		// String transaction = Util.hex((byte[]) (bMap.get("t")));

		byte[] tt = (byte[]) (bMap.get("t"));
		FindNodeResponse findNodeResponse = new FindNodeResponse(transaction,
				src);
		findNodeResponse.setTt(tt);
		findNodeResponse.setNodes(lists);
		send(findNodeResponse);
		addToQueue(src);

		// ErrorKadResponse errorKadResponse=new ErrorKadResponse(transaction,
		// src);
		// send(errorKadResponse);
	}

	/**
	 * 回复Get_Peers请求
	 * 
	 * @param bMap
	 * @throws BTypeException
	 *             info_hash
	 * @throws IOException
	 */
	protected void handleGet_PeersRequest(String transaction, BMap bMap,
			Node src) throws BTypeException, IOException {
		byte[] infohash = (byte[]) bMap.getMap("a").get("info_hash");
		// final ErrorKadResponse errorKadResponse = new
		// ErrorKadResponse(transaction, src);
		// System.out.println("收到请求===Get_Peers=" + Util.hex(infohash) + "---"
		// + count++);
		String hh = Util.hex(infohash);
		if (!info_hashset.contains(hh)) {
			info_hashset.add(hh);
			// System.out.println("总共=" + info_hashset.size());
			// for (String ss : info_hashset) {
			// System.out.println(ss);
			// }
		}
		GetPeersRequest getPeersRequest = new GetPeersRequest(transaction, src);
		getPeersRequest.setInfo_hash(Util.hex(infohash));

		sendGet_Peers(transaction, infohash, src);
		GetPeersResponse getPeersResponse = new GetPeersResponse(transaction,
				src);
		List<Node> nodes = kadBuckets
				.getClosestNodesByKey(new Key(infohash), 8);
		getPeersResponse.setNodes(nodes);
		addToQueue(src);
		send(getPeersResponse);
	}

	private void sendGet_Peers(String transaction, byte[] infohash, Node src) {
		GetPeersRequest getPeersRequest = new GetPeersRequest(transaction, src);
		getPeersRequest.setInfo_hash(Util.hex(infohash));
		sendGet_Peers(getPeersRequest);
	}

	int fail = 0;
	int countcount = 0;

	private void sendGet_Peers(KadRequest findNodeResponse) {
		Timer timer = new Timer();
		// KadSendMsgServer kadServer = kadNet.getKadSendMsgServer();
		// try {
		// kadServer.send(localNode, findNodeResponse);
		// } catch (IOException e1) {
		// e1.printStackTrace();
		// }
		MessageDispatcher dispatcher = new MessageDispatcher(timer,
				KadServer.this, findNodeResponse.getTransaction());
		dispatcher.setConsumable(true)//
				// .addFilter(new
				// IdMessageFilter(findNodeResponse.getTransaction()))// 只接受的类型
				// .addFilter(new TypeMessageFilter(FindNodeResponse.class))//
				.setCallback(null, new CompletionHandler<KadMessage, BMap>() {
					@Override
					public void completed(KadMessage msg, BMap bMap) {
						System.out.println(bMap);
						BMap bb;
						try {
							bb = bMap.getMap("r");
							if (bb.containsKey("nodes")) {
								byte[] ddd=(byte[]) bb.get("nodes");
								passgetpeersNodes(msg,ddd);
							}else if( bb.containsKey("values")){
								System.out.println("收到相应value=========="+bMap);
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}// .getn
					
//						System.out.println("收到正确的响应" + ++countcount);
					}

					@Override
					public void failed(Throwable exc, BMap nothing) {
//						System.out.println("相响应错误了==" + ++fail);
					}
				});
		try {
			dispatcher.send(findNodeResponse);
			// System.out.println("发送getpeer请求");
		} catch (Exception e) {
			e.printStackTrace();
		}
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
	private void handleResponseMsg(BMap bMap, Node src, String transaction)
			throws BTypeException, BDecodingException, UnknownHostException {

		MessageDispatcher messageDispatcher = MessageDispatcher
				.findMessageDispatcherByTag(transaction);// 取出之前的请求对象
		if (messageDispatcher != null) {// 有记录
			KadRequest kadRequest = messageDispatcher.getKadRequest();
			messageDispatcher.handle(kadRequest,bMap);
			if (kadRequest.getClass() == FindNodeRequest.class) {
				// FindNodeResponse findNodeResponse =
				// receiveFind_Node(transaction, bMap, src);
				ReceiveFindNodeResponse receiveFindNodeResponse = new ReceiveFindNodeResponse(
						transaction, bMap, src);
				messageDispatcher.handle(receiveFindNodeResponse,bMap);
				// System.out.println("t=" + transaction);
				handlehindNodeResponse(bMap);// *****

			} else if (kadRequest.getClass() == PingRequest.class) {
				if (bMap.containsKey("r")) {
					BMap bmap_r = bMap.getMap("r");
					if (bmap_r.containsKey("id")) {
						String id = bmap_r.getString("id");
						// System.out.println("连接="+id);
						messageDispatcher.handle(kadRequest,bMap);
				
					}
				}
			} else if (kadRequest.getClass() == GetPeersRequest.class) {
				messageDispatcher.handle(kadRequest,bMap);
				
			} else {
				// TODO 响应的操作应该根据请求的id t判断是哪个响应，t清楚一次必须改变
			}
			// messageDispatcher.handle(msg);
		} else {// 没有记录就按照大众处理
			handlehindNodeResponse(bMap);// *****
		}
	}

	/**
	 * 只接受findNode的响应
	 * 
	 * @param bMap
	 * @throws UnknownHostException
	 * @throws BTypeException
	 */
	private void handlehindNodeResponse(BMap bMap) throws UnknownHostException,
			BTypeException {
		if (bMap.containsKey("r")) {
			BMap bmap_r = bMap.getMap("r");
			if (bmap_r.containsKey("nodes")) {
				byte[] bb = (byte[]) bmap_r.get("nodes");
				// System.out.println("连接="+Util.hex((byte[])
				// bmap_r.get("id")));
				passNodes(bb);
			}
		}
		// System.out.println("handlehindNodeResponse");
	}

	int nodescount = 0;

	private void passgetpeersNodes(KadMessage msg,byte[] bb) throws UnknownHostException {
		// byte[] bb = (byte[]) bMap.getMap("r").get("nodes");
		int count = bb.length / 26;
		// if (count != 0) {
		// return;
		// }
//		 System.out.println("解析node");
		nodescount += count;
		// System.out.println("收到nodes数量="+nodescount);
		for (int i = 0; i < count; i++) {
			byte[] nid = Arrays.copyOfRange(bb, i * 26, i * 26 + 20);
			byte[] ip = Arrays.copyOfRange(bb, i * 26 + 20, i * 26 + 24);
			byte[] p = Arrays.copyOfRange(bb, i * 26 + 24, i * 26 + 26);
			
			InetAddress inet4Address = InetAddress.getByAddress(ip);
			Node node = new Node(new Key(nid));
			node.setInetAddress(inet4Address).setPoint(Util.bytesToInt(p));
//			addToQueue(node);
			
			KadRequest kadRequest=(KadRequest) msg;
			kadRequest.setNode(node);
			sendGet_Peers(kadRequest);
			// System.out.println(inet4Address.getHostAddress()+":"+Util.bytesToInt(p));
		}
		
	}
	private void passNodes(byte[] bb) throws UnknownHostException {
		// byte[] bb = (byte[]) bMap.getMap("r").get("nodes");
		int count = bb.length / 26;
		// if (count != 0) {
		// return;
		// }
		// System.out.println("解析node");
		nodescount += count;
		// System.out.println("收到nodes数量="+nodescount);
		for (int i = 0; i < count; i++) {
			byte[] nid = Arrays.copyOfRange(bb, i * 26, i * 26 + 20);
			byte[] ip = Arrays.copyOfRange(bb, i * 26 + 20, i * 26 + 24);
			byte[] p = Arrays.copyOfRange(bb, i * 26 + 24, i * 26 + 26);

			InetAddress inet4Address = InetAddress.getByAddress(ip);
			Node node = new Node(new Key(nid));
			node.setInetAddress(inet4Address).setPoint(Util.bytesToInt(p));
			addToQueue(node);
			// System.out.println(inet4Address.getHostAddress()+":"+Util.bytesToInt(p));
		}

	}

	int count = 0;

	private void addToQueue(Node node) {
		if (!node.equals(AppManager.getLocalNode())) {
			// nodesqueue.add(node);

			// System.out.println("key=" + node.getKey().toString());

			// nodes.add(node);
			// System.out.println("节点数     ="+nodesqueue.size());
			// System.out.println("这里就是空吗="+node.getKey());
			kadBuckets
					.insert(new KadNode().setNode(node).setNodeWasContacted());// 插入一个节点
			// System.out.println("队列数量=" + nodesqueue.size());
			// System.out.println("计数=" + (++count));
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

	protected synchronized void handleRequestMsg(DatagramPacket pkt, BMap bMap,
			String transaction) throws BTypeException,
			NoSuchAlgorithmException, IOException {

		if (bMap.containsKey("q")) {
			String q = bMap.getString("q");// find_node or getpeers===

			// System.out.println("收到请求=" + q);
			// String id = bMap.getMap("a").getString("id");
			// System.out.println("id长度=="+bMap.getMap("a").get("id"));
			// System.out.println("id长度"+id.length());
			// Key key = new Key(Util.StringId2bytes(id));
			Key key = new Key((byte[]) bMap.getMap("a").get("id"));
			// System.out.println(key.toString());
			// int idd=Integer.parseInt(id, 16);
			final Node to = new Node(key);
			to.setInetAddress(pkt.getAddress());
			to.setPoint(pkt.getPort());

			switch (q) {
			case "find_node":
				// ByteArrayInputStream arrayInputStream = new
				// ByteArrayInputStream(pkt.getData());
				// BDecoder bDecoder = new BDecoder(arrayInputStream);
				// Map<String, Object> map = bDecoder.decodeMap();
				// System.out.println("收到findnode_map="+map);
				// Map<String, Object> map_a=(Map<String, Object>) map.get("a");
				// Object t= map.get("t");
				// System.out.println("t----="+t.getClass());
				// System.out.println("收到findnode_map_a="+map_a);
				// byte[] bb=(byte[]) t;

				// System.out.println("字符串=" + new String(pkt.getData()));
				// System.out.println("收到消息的解码=" + map + "----" +
				// pkt.getSocketAddress());
				// System.out.println("transaction-hash=" + transaction);
				// System.out.println("收到请求===find_node=" +
				// pkt.getSocketAddress() + "--tttt=" + transaction + "-key=" +
				// key.toString());
				handleFind_NodeRequest(transaction, bMap, to);
				break;
			case "get_peers":
				handleGet_PeersRequest(transaction, bMap, to);
				break;
			case "ping"://
				// System.out.println("收到请求===ping=" + pkt.getSocketAddress());
				hanldePingRequest(transaction, bMap, to);
				break;
			case "announce_peer"://
				hanldeAnnounce_PeerRequest(transaction, bMap, to);
				// System.out.println("收到请求===announce_peer="
				// + pkt.getSocketAddress());
				break;

			default:
				// System.out.println("收到请求===其他=" + q + "==="
				// + pkt.getSocketAddress());
				break;
			}

		} else {
			return;
		}
	}

	private void hanldeAnnounce_PeerRequest(String transaction, BMap bMap,
			Node to) throws BTypeException {
		String info_hash = Util.hex((byte[]) bMap.getMap("a").get("info_hash"));
		System.out.println("获取到announce_peer=" + info_hash);
		if (!info_hashset.contains(info_hash)) {
			info_hashset.add(info_hash);
			System.out.println("收到种子=" + info_hashset.size());
			// for (String s : info_hashset) {
			// System.out.println(s);
			// }
			// System.out.println("正在种子="+Util.hex((byte[])
			// bMap.getMap("a").get("info_hash")));
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
					pkt = new DatagramPacket(new byte[1024], 1024);

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
