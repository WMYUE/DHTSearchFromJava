package com.konka.dhtsearch.bittorrentkad.bucket;

import java.util.Collection;

import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.KadNode;

/**
 * A default stupid implementation of Bucket that does nothing
 * 
 * @author eyal.kibbar@gmail.com
 */
public class DummyBucket implements Bucket {

	@Override
	public void insert(KadNode n) {
	}

	@Override
	public void addNodesTo(Collection<Node> c) {
	}

	@Override
	public void markDead(Node n) {
	}
}
