package com.konka.dhtsearch.bittorrentkad.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
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
import com.konka.dhtsearch.bittorrentkad.KadNode;
import com.konka.dhtsearch.bittorrentkad.bucket.Bucket;
import com.konka.dhtsearch.bittorrentkad.concurrent.CompletionHandler;
import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;
import com.konka.dhtsearch.bittorrentkad.krpc.KadRequest;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeRequest;
import com.konka.dhtsearch.bittorrentkad.krpc.find_node.FindNodeResponse;
import com.konka.dhtsearch.bittorrentkad.krpc.get_peers.GetPeersRequest;
import com.konka.dhtsearch.bittorrentkad.krpc.get_peers.GetPeersResponse;
import com.konka.dhtsearch.bittorrentkad.krpc.ping.PingRequest;
import com.konka.dhtsearch.bittorrentkad.krpc.ping.PingResponse;
import com.konka.dhtsearch.util.Util;

/**
 * 守护线程，负责接受和发送消息
 * 
 */
public class KadServer implements Runnable, DHTConstant {

	// private final DatagramSocket socket;
	private final BlockingQueue<DatagramPacket> pkts = new LinkedBlockingDeque<DatagramPacket>();;
	// private final BlockingQueue<Node> nodesqueue;// = new
	// LinkedBlockingDeque<Node>();;
	private final ExecutorService srvExecutor = new ScheduledThreadPoolExecutor(1);
	private final AtomicBoolean isActive = new AtomicBoolean(false);
	private final Thread startThread;// ;=new Thread();
	private final Bucket kadBuckets;
	private final Set<String> info_hashset = new HashSet<String>();
	private final Selector selector;
	private final DatagramChannel channel;

	public KadServer(DatagramSocket socket, Bucket kadBuckets, Selector selector, DatagramChannel channel) {
		// this.socket = socket;
		startThread = new Thread(this);
		this.kadBuckets = kadBuckets;
		this.selector = selector;
		this.channel = channel;

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
			// final DatagramPacket pkt = new DatagramPacket(buf, 0,
			// buf.length);

			// pkt.setSocketAddress(msg.getSrc().getSocketAddress());
			// this.socket.send(pkt);
			channel.send(ByteBuffer.wrap(buf), msg.getSrc().getSocketAddress());
			// channel.send(src, target);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 回复Ping请求
	 * 
	 * @param bMap
	 * @throws BTypeException
	 * @throws IOException
	 */
	protected void hanldePingRequest(String transaction, BMap bMap, Node src) throws BTypeException, IOException {
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
	private void handleFind_NodeRequest(String transaction, BMap bMap, Node src) throws BTypeException, IOException {
		byte[] target = (byte[]) bMap.getMap(A).get(TARGET);
		List<Node> lists = kadBuckets.getClosestNodesByKey(new Key(target), 8);

		// String transaction = Util.hex((byte[]) (bMap.get("t")));

		byte[] tt = (byte[]) (bMap.get(T));
		FindNodeResponse findNodeResponse = new FindNodeResponse(transaction, src);
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
	protected void handleGet_PeersRequest(String transaction, BMap bMap, Node src) throws BTypeException, IOException {
		byte[] infohash = (byte[]) bMap.getMap(A).get(INFO_HASH);
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
		GetPeersResponse getPeersResponse = new GetPeersResponse(transaction, src);
		List<Node> nodes = kadBuckets.getClosestNodesByKey(new Key(infohash), 8);
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
		MessageDispatcher dispatcher = new MessageDispatcher(timer, KadServer.this, findNodeResponse.getTransaction());
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
							bb = bMap.getMap(R);
							if (bb.containsKey(NODES)) {
								byte[] ddd = (byte[]) bb.get(NODES);
								passgetpeersNodes(msg, ddd);
							} else if (bb.containsKey(VALUES)) {
								System.out.println("收到相应value==========" + bMap);
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}// .getn

						// System.out.println("收到正确的响应" + ++countcount);
					}

					@Override
					public void failed(Throwable exc, BMap nothing) {
						// System.out.println("相响应错误了==" + ++fail);
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
	 * @param decodedData
	 * @param src
	 * @param transaction
	 * @param messageDispatcher
	 * @throws BTypeException
	 * @throws BDecodingException
	 * @throws UnknownHostException
	 */
	private void handleResponseMsg(BMap decodedData, String transaction) throws BTypeException, BDecodingException, UnknownHostException {

		MessageDispatcher messageDispatcher = MessageDispatcher.findMessageDispatcherByTag(transaction);// 取出之前的请求对象
		if (messageDispatcher != null) {// 有记录
			KadRequest kadRequest = messageDispatcher.getKadRequest();
			messageDispatcher.handle(kadRequest, decodedData);
			if (kadRequest.getClass() == FindNodeRequest.class) {
				handlefindNodeResponse(decodedData);// *****
			} else if (kadRequest.getClass() == PingRequest.class) {

			} else if (kadRequest.getClass() == GetPeersRequest.class) {

			} else {
				// TODO 响应的操作应该根据请求的id t判断是哪个响应，t清楚一次必须改变
			}
			// messageDispatcher.handle(msg);
		} else {// 没有记录就按照大众处理
			handlefindNodeResponse(decodedData);// *****
		}
	}

	/**
	 * 只接受findNode的响应
	 * 
	 * @param bMap
	 * @throws UnknownHostException
	 * @throws BTypeException
	 */
	private void handlefindNodeResponse(BMap bMap) throws UnknownHostException, BTypeException {
		if (bMap.containsKey(R)) {
			BMap bmap_r = bMap.getMap(R);
			if (bmap_r.containsKey(NODES)) {
				byte[] bb = (byte[]) bmap_r.get(NODES);
				passNodes(bb);
			}
		}
	}

	private void passgetpeersNodes(KadMessage msg, byte[] bb) throws UnknownHostException {
		int bytelength = bb.length;
		if (bytelength % 26 != 0) {
			return;
		}
		int count = bytelength / 26;
		for (int i = 0; i < count; i++) {
			byte[] nid = Arrays.copyOfRange(bb, i * 26, i * 26 + 20);
			byte[] ip = Arrays.copyOfRange(bb, i * 26 + 20, i * 26 + 24);
			byte[] p = Arrays.copyOfRange(bb, i * 26 + 24, i * 26 + 26);

			InetAddress inet4Address = InetAddress.getByAddress(ip);
			Node node = new Node(new Key(nid));
			node.setInetAddress(inet4Address).setPoint(Util.bytesToInt(p));
			// addToQueue(node);

			KadRequest kadRequest = (KadRequest) msg;
			kadRequest.setNode(node);
			sendGet_Peers(kadRequest);
			// System.out.println(inet4Address.getHostAddress()+":"+Util.bytesToInt(p));
		}

	}

	private void passNodes(byte[] bb) throws UnknownHostException {
		int bytelength = bb.length;
		if (bytelength % 26 != 0) {
			return;
		}
		int count = bytelength / 26;
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
			kadBuckets.insert(new KadNode().setNode(node).setNodeWasContacted());// 插入一个节点
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

	protected synchronized void handleRequestMsg(InetSocketAddress pkt, BMap bMap, String transaction) throws BTypeException, NoSuchAlgorithmException, IOException {

		if (bMap.containsKey(Q)) {
			String q = bMap.getString(Q);// find_node or getpeers===

			Key key = new Key((byte[]) bMap.getMap(A).get(ID));

			final Node to = new Node(key).setSocketAddress(pkt);

			switch (q) {
				case FIND_NODE:
					handleFind_NodeRequest(transaction, bMap, to);
					break;
				case GET_PEERS:
					handleGet_PeersRequest(transaction, bMap, to);
					break;
				case PING://
					// System.out.println("收到请求===ping=" + pkt.getSocketAddress());
					hanldePingRequest(transaction, bMap, to);
					break;
				case ANNOUNCE_PEER://
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

	private void hanldeAnnounce_PeerRequest(String transaction, BMap bMap, Node to) throws BTypeException {
		String info_hash = Util.hex((byte[]) bMap.getMap(A).get(INFO_HASH));
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
			// DatagramPacket pkt = null;
			// try {
			// // System.out.println("等待数据");
			// pkt = this.pkts.poll();
			// if (pkt == null)
			// pkt = new DatagramPacket(new byte[1024], 1024);
			//
			// this.socket.receive(pkt);// 堵塞
			// // System.out.println("已经拿到数据可");
			// handleIncomingPacket(pkt);// 收到信息后处理
			//
			// } catch (final Exception e) {
			// // insert the taken pkt back
			// if (pkt != null)
			// this.pkts.offer(pkt);
			//
			// e.printStackTrace();
			// }
			ByteBuffer byteBuffer = ByteBuffer.allocate(65536);
			try {
				int eventsCount = selector.select();// 这里堵塞
				if (eventsCount > 0) {
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> iterator = selectedKeys.iterator();
					while (iterator.hasNext()) {
						SelectionKey sk = (SelectionKey) iterator.next();
						iterator.remove();
						if (sk.isReadable()) {
							DatagramChannel datagramChannel = (DatagramChannel) sk.channel();
							SocketAddress target = datagramChannel.receive(byteBuffer);// 接受数据

							byteBuffer.flip();
							byte[] dst = new byte[byteBuffer.limit()];
							byteBuffer.get(dst);
							byteBuffer.clear();
							handleIncomingData((InetSocketAddress) target, dst);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			//
		}
	}

	private void handleIncomingData(final InetSocketAddress target, final byte[] dst) throws SocketException {
		this.srvExecutor.execute(new Runnable() {// 交给线程池处理
					@Override
					public void run() {
						try {
							BMap decodedData = (BMap) BEncodedInputStream.bdecode(dst);
							String transaction = Util.hex((byte[]) (decodedData.get(T)));
							if (decodedData.containsKey(Y)) {
								String y = decodedData.getString(Y);
								if ("q".equals(y)) {// 对方请求
									handleRequestMsg(target, decodedData, transaction);
								} else if ("r".equals(y)) {// 对方的响应
									handleResponseMsg(decodedData, transaction);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

				});
	}

	/**
	 * Shutdown the server and closes the socket 关闭服务
	 * 
	 * @param kadServerThread
	 */
	// @Override
	public void shutdown() {
		this.isActive.set(false);
		// this.socket.close();
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
