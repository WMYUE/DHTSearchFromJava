package com.konka.dhtsearch.bittorrentkad.bucket;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.KadNode;
import com.konka.dhtsearch.bittorrentkad.concurrent.CompletionHandler;
import com.konka.dhtsearch.bittorrentkad.msg.KadMessage;
import com.konka.dhtsearch.bittorrentkad.msg.PingRequest;
import com.konka.dhtsearch.bittorrentkad.msg.PingResponse;
import com.konka.dhtsearch.bittorrentkad.net.MessageDispatcher;
import com.konka.dhtsearch.bittorrentkad.net.filter.IdMessageFilter;
import com.konka.dhtsearch.bittorrentkad.net.filter.TypeMessageFilter;

/**
 * A bucket with the following policy:具有下列政策斗： when inserting a node do the following:当插入一个节点做如下： 1. if the node is already in the bucket, move it to be the last 1。如果该节点已经在斗，移动它是最后一次 2. if the node is not in the bucket and the bucket is not full, move it to be the last in the bucket 2。如果节点不在铲斗、斗是不充分的，将其移动到桶上 3. if the node is not in the bucket and the bucket is full, ping the first node in the bucket: 3。如果节点不在桶和桶是满的，平中的第一个节点的桶： a. if it returned a ping, move it to be the last in bucket and don't insert the given node 如果它返回一个平，把它在桶上，不要插入节点 b. if it did not returned a ping, remove it from the bucket and insert the given node as last 如果它没有返回平，把它从桶中插入给定的节点上
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class StableBucket implements Bucket {

	// state
	private final List<KadNode> bucket;

	// dependencies
	private final int maxSize;
	private final long validTimespan;
	private final  PingRequest  pingRequestProvider;
	private final  MessageDispatcher<Void > msgDispatcherProvider;
	private final ExecutorService pingExecutor;

	public StableBucket(int maxSize,  long validTimespan, ExecutorService pingExecutor, PingRequest  pingRequestProvider,  MessageDispatcher<Void> msgDispatcherProvider) {

		this.maxSize = maxSize;
		this.bucket = new LinkedList<KadNode>();
		this.validTimespan = validTimespan;
		this.pingExecutor = pingExecutor;
		this.pingRequestProvider = pingRequestProvider;
		this.msgDispatcherProvider = msgDispatcherProvider;
	}

	@Override
	public synchronized void insert(final KadNode n) {
		int i = bucket.indexOf(n);// 查找这个节点
		if (i != -1) {// 找到了
			// found node in bucket

			// if heard from n (it is possible to insert n i never had
			// contact with simply by hearing about from another node)
			if (bucket.get(i).getLastContact() < n.getLastContact()) {// old的插入时间小于new的
				KadNode s = bucket.remove(i);
				s.setNodeWasContacted(n.getLastContact());
				bucket.add(s);
				// 移除旧的，添加新的
			}
		} else if (bucket.size() < maxSize) {// 没有找到,并且没有满 直接添加
			// not found in bucket and there is enough room for n
			bucket.add(n);

		} else {// 没有找到，但是满了
			// n is not in bucket and bucket is full

			// don't bother to insert n if I never recved a msg from it
			// 如果重来没有冲这个node发来信息，不要插入
			if (n.hasNeverContacted())
				return;

			// check the first node, ping him if no one else is currently pinging
			KadNode inBucketReplaceCandidate = bucket.get(0);// 取最老的一个

			// the first node was only inserted indirectly (meaning, I never recved
			// a msg from it !) and I did recv a msg from n.
			if (inBucketReplaceCandidate.hasNeverContacted()) {// 检测这个有没有返回过信息，没有就删除他，他新的插入到最后
				bucket.remove(inBucketReplaceCandidate);
				bucket.add(n);
				return;
			}

			// ping is still valid, don't replace检测是否超过有效时间了，没有就直接返回
			if (inBucketReplaceCandidate.isPingStillValid(validTimespan))
				return;

			// send ping and act accordingly
			if (inBucketReplaceCandidate.lockForPing()) {
				sendPing(bucket.get(0), n);
			}
		}
	}

	/**
	 * ping通就不处理，没有就替换
	 * 
	 * @param inBucket
	 * @param replaceIfFailed
	 */
	private void sendPing(final KadNode inBucket, final KadNode replaceIfFailed) {

		final PingRequest pingRequest = pingRequestProvider;

		final MessageDispatcher<Void> dispatcher = msgDispatcherProvider.setConsumable(true)//
				.addFilter(new IdMessageFilter(pingRequest.getId()))//
				.addFilter(new TypeMessageFilter(PingResponse.class))//
				.setCallback(null, new CompletionHandler<KadMessage, Void>() {
					@Override
					public void completed(KadMessage msg, Void nothing) {
						// ping was recved
						inBucket.setNodeWasContacted();
						inBucket.releasePingLock();
						synchronized (StableBucket.this) {
							if (bucket.remove(inBucket)) {
								bucket.add(inBucket);
							}
						}
					}

					@Override
					public void failed(Throwable exc, Void nothing) {
						// ping was not recved
						synchronized (StableBucket.this) {
							// try to remove the already in bucket and
							// replace it with the new candidate that we
							// just heard from.
							if (bucket.remove(inBucket)) {
								// successfully removed the old node that
								// did not answer my ping

								// try insert the new candidate
								if (!bucket.add(replaceIfFailed)) {
									// candidate was already in bucket
									// return the inBucket to be the oldest node in
									// the bucket since we don't want our bucket
									// to shrink unnecessarily
									bucket.add(0, inBucket);
								}
							}
						}
						inBucket.releasePingLock();
					}
				});

		try {
			pingExecutor.execute(new Runnable() {

				@Override
				public void run() {
					dispatcher.send(inBucket.getNode(), pingRequest);
				}
			});
		} catch (Exception e) {
			inBucket.releasePingLock();
		}
	}

	/**
	 * 把当前k桶中的n节点删除
	 */
	@Override
	public synchronized void markDead(Node n) {
		for (int i = 0; i < bucket.size(); ++i) {
			KadNode kadNode = bucket.get(i);
			if (kadNode.getNode().equals(n)) {
				// mark dead an move to front
				kadNode.markDead();
				bucket.remove(i);
				bucket.add(0, kadNode);
			}
		}
	}

	@Override
	/**
	 * 把这个k桶中的节点放在集合中
	 */
	public synchronized void addNodesTo(Collection<Node> c) {
		for (KadNode n : bucket) {
			c.add(n.getNode());
		}
	}

	@Override
	public synchronized String toString() {
		return bucket.toString();
	}

}
