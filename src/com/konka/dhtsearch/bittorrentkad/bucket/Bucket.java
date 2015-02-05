package com.konka.dhtsearch.bittorrentkad.bucket;

import java.util.Collection;
import java.util.List;

import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.KadNode;

/**
 * Represents A finate container for nodes
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public interface Bucket {

	/**
	 * Adds a new node to the bucket
	 * @param n the new node
	 */
	public void insert(KadNode n);

	/**
	 * Adds all nodes in bucket to the given collection
	 * @param c the collection the nodes will be added to
	 */
	void addNodesTo(Collection<Node> c);

	/**
	 * Marks a node as dead: the dead node will be replace if 
	 * insert was invoked 
	 * @param n the dead node
	 */
	public void markDead(Node n);

	public List<KadNode> getAllNodes();

	public List<Node> getClosestNodesByKey(Key key, int i);
}
