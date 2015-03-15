package com.konka.dhtsearch.bittorrentkad.bucket;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.KadNode;

/**
 * A bucket with the following policy: Any new node is inserted, if the bucket has reached its max size the oldest node in the bucket is removed.
 * 
 */
public class SlackBucket implements Bucket {

	private final List<KadNode> bucket;
	private final int maxSize;

	public SlackBucket(int maxSize) {
		this.maxSize = maxSize;
		bucket = new LinkedList<KadNode>();
	}

	@Override
	public void insert(KadNode n) {
		// dont bother with other people wrong information
		if (n.hasNeverContacted())
			return;

		synchronized (bucket) {
			if (bucket.contains(n))
				return;
			if (bucket.size() == maxSize)
				bucket.remove(0);
			bucket.add(n);
		}
	}

	@Override
	public void markDead(Node n) {
		// nothing to do
	}

	@Override
	public List<KadNode> getAllNodes() {

		return bucket;
	}

	@Override
	public List<Node> getClosestNodesByKey(Key key, int i) {
		List<Node> nodes = new ArrayList<Node>();
		int j = 0;
		for (KadNode kadNode : bucket) {
			nodes.add(kadNode.getNode());
			if (j >= i)
				break;
		}
		return nodes;
	}
}
