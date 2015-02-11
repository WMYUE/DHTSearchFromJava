package com.konka.dhtsearch.bittorrentkad.net;

import java.io.IOException;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.yaircc.torrent.bencoding.BDecodingException;
import org.yaircc.torrent.bencoding.BEncodedInputStream;
import org.yaircc.torrent.bencoding.BList;
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
	protected void hanldePingRequest(String transaction, Node src) throws BTypeException, IOException {
		PingResponse pingResponse = new PingResponse(transaction, src);
		send(pingResponse);
		addNodeToQueue(src);
	}

	/**
	 * 回复Get_Peers请求
	 * 
	 * @param decodedData
	 * @throws BTypeException
	 *             info_hash
	 * @throws IOException
	 */
	protected void handleGet_PeersRequest(String transaction, BMap decodedData, Node src) throws BTypeException, IOException {
		byte[] bytesFromInfohash = (byte[]) decodedData.getMap(A).get(INFO_HASH);
		String infoHash = Util.hex(bytesFromInfohash);
		if (!info_hashset.contains(infoHash)) {
			info_hashset.add(infoHash);
		}
		GetPeersRequest getPeersRequest = new GetPeersRequest(transaction, src);
		getPeersRequest.setInfo_hash(Util.hex(bytesFromInfohash));

		sendGet_Peers(transaction, bytesFromInfohash, src);// 发送请求。查找infohash
		GetPeersResponse getPeersResponse = new GetPeersResponse(transaction, src);
		List<Node> nodes = kadBuckets.getClosestNodesByKey(new Key(bytesFromInfohash), CLOSEST_GOOD_NODES_COUNT);
		getPeersResponse.setNodes(nodes);
		addNodeToQueue(src);
		send(getPeersResponse);
	}

	private void sendGet_Peers(String transaction, byte[] infohash, Node src) {
		GetPeersRequest getPeersRequest = new GetPeersRequest(transaction, src);
		getPeersRequest.setInfo_hash(Util.hex(infohash));
		sendGet_Peers(getPeersRequest);
	}

	int fail = 0;
	int countcount = 0;

	private void sendGet_Peers(KadRequest getPeersRequest) {
		MessageDispatcher dispatcher = new MessageDispatcher(KadServer.this, getPeersRequest.getTransaction());
		dispatcher.setConsumable(true)//
				// .addFilter(new
				// IdMessageFilter(findNodeResponse.getTransaction()))// 只接受的类型
				// .addFilter(new TypeMessageFilter(FindNodeResponse.class))//
				.setCallback(null, new CompletionHandler<KadMessage, BMap>() {
					@Override
					public void completed(KadMessage msg, BMap decodedData) {
						// System.out.println(decodedData);
						BMap responseMap;
						try {
							responseMap = decodedData.getMap(R);
							if (responseMap.containsKey(NODES)) {
								byte[] bytesFromNodes = (byte[]) responseMap.get(NODES);
								parsergetpeersNodes(msg, bytesFromNodes);
							} else if (responseMap.containsKey(VALUES)) {
								// System.out.println("收到相应value==========" + decodedData);
								BList bList = responseMap.getList(VALUES);
								byte[] bytes = (byte[]) bList.get(0);
//								System.out.println("value长度=" + bytes.length);

								byte[] ip = Arrays.copyOfRange(bytes, 0, 4);
								byte[] p = Arrays.copyOfRange(bytes, 4, 6);

								InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getByAddress(ip), Util.bytesToInt(p));
								System.out.println(inetSocketAddress);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					@Override
					public void failed(Throwable exc, BMap nothing) {
						// System.out.println("相响应错误了==" + ++fail);
					}
				});
		try {
			dispatcher.send(getPeersRequest);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void parsergetpeersNodes(KadMessage msg, byte[] bytesFromNodes) throws UnknownHostException {
		List<Node> nodes = Util.passNodes(bytesFromNodes);
		for (int i = 0; i < nodes.size(); i++) {
			KadRequest kadRequest = (KadRequest) msg;
			kadRequest.setNode(nodes.get(i));
			sendGet_Peers(kadRequest);
		}

	}

	int count = 0;

	/**
	 * 处理请求信息
	 * 
	 * @param decodedData
	 * @throws BTypeException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */

	protected synchronized void handleRequestMsg(InetSocketAddress inetSocketAddress, BMap decodedData, String transaction) throws BTypeException, NoSuchAlgorithmException, IOException {

		if (decodedData.containsKey(Q)) {
			String q_value = decodedData.getString(Q);// find_node or getpeers===

			Key key = new Key((byte[]) decodedData.getMap(A).get(ID));

			final Node to = new Node(key).setSocketAddress(inetSocketAddress);

			switch (q_value) {
				case FIND_NODE://
					handleFind_NodeRequest(transaction, decodedData, to);
					break;
				case GET_PEERS://
					// byte[] bytesFromInfohash = (byte[]) decodedData.getMap(A).get(INFO_HASH);
					handleGet_PeersRequest(transaction, decodedData, to);
					break;
				case PING://
					hanldePingRequest(transaction, to);
					break;
				case ANNOUNCE_PEER://
					hanldeAnnounce_PeerRequest(transaction, decodedData);// 不回复
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

	private void hanldeAnnounce_PeerRequest(String transaction, BMap decodedData) throws BTypeException {
		String info_hash = Util.hex((byte[]) decodedData.getMap(A).get(INFO_HASH));
		// System.out.println("获取到announce_peer=" + info_hash);
		if (!info_hashset.contains(info_hash)) {
			info_hashset.add(info_hash);
			// System.out.println("收到种子=" + info_hashset.size());
			// for (String s : info_hashset) {
			// System.out.println(s);
			// }
			// System.out.println("正在种子="+Util.hex((byte[])
			// bMap.getMap("a").get("info_hash")));
		}
	}

	/**
	 * 回复find_node请求
	 * 
	 * @param decodedData
	 * @throws BTypeException
	 * @throws IOException
	 */
	private void handleFind_NodeRequest(String transaction, BMap decodedData, Node src) throws BTypeException, IOException {
		byte[] target = (byte[]) decodedData.getMap(A).get(TARGET);
		List<Node> lists = kadBuckets.getClosestNodesByKey(new Key(target), 8);

		FindNodeResponse findNodeResponse = new FindNodeResponse(transaction, src);
		findNodeResponse.setNodes(lists);
		send(findNodeResponse);
		addNodeToQueue(src);

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
	private void handlefindNodeResponse(BMap decodedData) throws UnknownHostException, BTypeException {
		if (decodedData.containsKey(R)) {
			BMap respondData = decodedData.getMap(R);
			if (respondData.containsKey(NODES)) {
				byte[] nodesbyteArray = (byte[]) respondData.get(NODES);
				List<Node> nodes = Util.passNodes(nodesbyteArray);
				addNodesToQueue(nodes);
			}
		}
	}

	private void addNodesToQueue(List<Node> nodes) {
		for (Node node : nodes) {
			addNodeToQueue(node);
		}
	}

	private void addNodeToQueue(Node node) {
		if (!node.equals(AppManager.getLocalNode())) {
			kadBuckets.insert(new KadNode().setNode(node).setNodeWasContacted());// 插入一个节点
		}
	}

	private void handleIncomingData(final InetSocketAddress target, final byte[] dst) {
		this.srvExecutor.execute(new Runnable() {// 交给线程池处理
					@Override
					public void run() {

						BMap decodedData;
						try {
							decodedData = (BMap) BEncodedInputStream.bdecode(dst);
							String transaction = Util.hex((byte[]) (decodedData.get(T)));
							if (decodedData.containsKey(Y)) {
								String y = decodedData.getString(Y);
								if (Q.equals(y)) {// 对方请求
									handleRequestMsg(target, decodedData, transaction);// 处理响应时候不需要解析令牌
								} else if (R.equals(y)) {// 对方的响应
									handleResponseMsg(decodedData, transaction);
								}
							}
						} catch (BDecodingException e) {
							// System.out.println("解析报错="+new String(dst));
							// System.out.println("长度="+dst.length);
							// e.printStackTrace();
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (BTypeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
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
