//package com.konka.dhtsearch.bittorrentkad.bucket;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import com.konka.dhtsearch.AppManager;
//import com.konka.dhtsearch.Key;
//import com.konka.dhtsearch.KeyFactory;
//import com.konka.dhtsearch.Node;
//import com.konka.dhtsearch.bittorrentkad.KadNode;
//
///**
// * This is a data structures that holds all the known nodes It sorts them into buckets according to their keys common prefix with the local node's key.
// * 
// * A node with a different MSB in its key than the local node's MSB will be inserted to the last bucket. A node with ONLY the LSB different will be inserted into the first bucket. Generally, a node with a common prefix the length of k bits with the local node will be inserted to the KeyLengthInBit - k bucket
// * 
// * @author eyal.kibbar@gmail.com
// *
// */
//// 这个相当是路由表
//public class KadBuckets implements KBuckets {
//
//	// private final MessageDispatcher<Object> msgDispatcherProvider;
//	private final Bucket[] kbuckets;// 默认160
//	// protected final Node localNode;
//	private final KeyFactory keyFactory;
//
//	public KadBuckets(KeyFactory keyFactory, Bucket kBucket) {
//		this.keyFactory = keyFactory;
//		// this.msgDispatcherProvider = msgDispatcherProvider;
//
//		kbuckets = new Bucket[keyFactory.getBitLength()];
//		for (int i = 0; i < kbuckets.length; ++i) {
//			kbuckets[i] = kBucket;
//		}
//	}
//
//	/**
//	 * Uses the keyFactory to generate keys which will fit to different buckets
//	 * 
//	 * @return a list of random keys where no 2 keys will fit into the same bucket
//	 */
//	@Override
//	public List<Key> randomKeysForAllBuckets() {
//		List<Key> $ = new ArrayList<Key>();
//		for (int i = 0; i < kbuckets.length; ++i) {
//			Key key = keyFactory.generate(i).xor(AppManager.getLocalNode().getKey());
//			$.add(key);
//		}
//		return $;
//	}
//
//	/**
//	 * Register this data structure to listen to incoming messages and update itself accordingly. Invoke this method after creating the entire system
//	 */
//	@Override
//	public synchronized void registerIncomingMessageHandler() {
//		// msgDispatcherProvider.setConsumable(false)
//		// // do not add PingResponse since it might create a loop
//		//
//		// .addFilter(new TypeExcluderMessageFilter(PingResponse.class))//过滤ping的响应
//		// .addFilter(new SrcExcluderMessageFilter(localNode))//过滤本地节点
//		// .setCallback(null, new CompletionHandler<KadMessage, Object>() {
//		//
//		// @Override
//		// public void failed(Throwable exc, Object attachment) {
//		// // should never be here
//		// exc.printStackTrace();
//		// }
//		//
//		// @Override
//		// public void completed(KadMessage msg, Object attachment) {
//		// //路由表收到节点后插入到路由表中(主要是更新时间操作)
//		// KadNode kadNode=new KadNode();
//		// kadNode.setNode(msg.getSrc()).setNodeWasContacted();
//		// KadBuckets.this.insert(kadNode);
//		//
//		// //有可能返回的信息中有nodes,必须从里面提取，然后插入到路由表中
//		// List<Node> nodes = null;
//		// if (msg instanceof FindNodeResponse) {//bittorrent 协议中定义
//		// nodes = ((FindNodeResponse) msg).getNodes();
//		// // } else if (msg instanceof ForwardResponse) {
//		// // nodes = ((ForwardResponse) msg).getNodes();
//		// // } else if (msg instanceof ForwardMessage) {
//		// // nodes = ((ForwardMessage) msg).getNodes();
//		// // } else if (msg instanceof ForwardRequest) {
//		// // nodes = ((ForwardRequest) msg).getBootstrap();
//		// }
//		//
//		// if (nodes != null) {//
//		// for (int i = 0; i < nodes.size(); i++) {
//		// KadBuckets.this.insert(kadNode.setNode(nodes.get(i)));
//		// }
//		// }
//		// }
//		// }).register();
//	}
//
//	private int getKBucketIndex(Key key) {
//		return key.xor(AppManager.getLocalNode().getKey()).getFirstSetBitIndex();
//	}
//
//	private List<Node> getClosestNodes(Key k, int n, int index, Bucket[] buckets) {
//		Set<Node> emptySet = Collections.emptySet();
//		return getClosestNodes(k, n, index, buckets, emptySet);
//	}
//
//	private List<Node> getClosestNodes(Key k, int n, int index, Bucket[] buckets, Collection<Node> exclude) {
//
//		final List<Node> $ = new ArrayList<Node>();
//		final Set<Node> t = new HashSet<Node>();
//		if (index < 0)
//			index = 0;
//
//		buckets[index].addNodesTo($);// 集合中已经有数据了
//
//		if ($.size() < n) {// 不够n个
//			// look in other buckets
//			for (int i = 1; $.size() < n; ++i) {
//				if (index + i < buckets.length) {// 没有超出
//					buckets[index + i].addNodesTo(t);
//					t.removeAll(exclude);// exclude本来是空的？？？
//					$.addAll(t);
//					t.clear();
//				}
//
//				if (0 <= index - i) {
//					buckets[index - i].addNodesTo(t);
//					t.removeAll(exclude);
//					$.addAll(t);
//					t.clear();
//				}
//
//				if (buckets.length <= index + i && index - i < 0)
//					break;
//			}
//		}
//
//		return $;// 返回时候有可能超过n个了
//	}
//
//	/**
//	 * Inserts a node to the data structure The can be rejected, depending on the bucket policy
//	 * 
//	 * @param node
//	 */
//	@Override
//	public void insert(KadNode node) {
//		// 这里需要操作数据库
//		int i = getKBucketIndex(node.getNode().getKey());// 产生一个0-159之间的数，由key决定
//		// 其实i就代表是哪一层
//		System.out.println("插入id="+ i);
//		if (i == -1)
//			return;
//
//		kbuckets[i].insert(node);// 插入代指定的楼层
//	}
//
//	/**
//	 * 获取所有的节点
//	 * 
//	 * @return a list containing all the nodes in the data structure
//	 */
//	@Override
//	public List<Node> getAllNodes() {
//		List<Node> $ = new ArrayList<Node>();
//		for (int i = 0; i < kbuckets.length; ++i) {
//			kbuckets[i].addNodesTo($);
//		}
//		return $;
//	}
//
//	@Override
//	public void markAsDead(Node n) {
//		int i = getKBucketIndex(n.getKey());
//		if (i == -1)
//			return;
//
//		kbuckets[i].markDead(n);
//	}
//
//	/**
//	 * Returns a single bucket's content. The bucket number is calculated using the given key according to its prefix with the local node's key as explained above.
//	 * 
//	 * @param k
//	 *            key to calculate the bucket from
//	 * @return a list of nodes from a particular bucket
//	 */
//	// 返回指定桶中的内容
//	@Override
//	public List<Node> getAllFromBucket(Key k) {
//		int i = getKBucketIndex(k);
//		if (i == -1)
//			return Collections.emptyList();
//		List<Node> $ = new ArrayList<Node>();
//		kbuckets[i].addNodesTo($);
//		return $;
//	}
//
//	/**
//	 * Gets all nodes with keys closest to the given k. The size of the list will be MIN(n, total number of nodes in the data structure)
//	 * 
//	 * @param k
//	 *            the key which the result's nodes are close to
//	 * @param n
//	 *            the maximum number of nodes expected
//	 * @return a list of nodes sorted by proximity to k
//	 */
//	/**
//	 * 返回相似的n个节点
//	 */
//
//	@Override
//	public List<Node> getClosestNodesByKey(Key k, int n) {
//		List<Node> $ = getClosestNodes(k, n, getKBucketIndex(k), kbuckets);
//		if ($.isEmpty())
//			return $;
//		Collections.sort($);
//		Collections.sort($, new KeyComparator(k));
//		$ = sort($, on(Node.class).getKey(), new KeyComparator(k));// 排序 越相似越靠前 越小越靠前
//		if ($.size() > n)
//			$.subList(n, $.size()).clear();// 裁剪多余的部分，然后清空他们
//		return $;//
//	}
//
//	@Override
//	public String toString() {
//		String $ = "";
//		for (int i = 0; i < kbuckets.length; ++i)
//			$ += kbuckets[i].toString() + "\n";
//		return $;
//	}
//
//}
