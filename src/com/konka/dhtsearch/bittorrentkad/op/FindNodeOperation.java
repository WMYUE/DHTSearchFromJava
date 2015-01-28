package com.konka.dhtsearch.bittorrentkad.op;

import java.util.List;

import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.msg.KadMessage;

public interface FindNodeOperation {

	/**
	 * Sets the key to be found.
	 * Do not change this value after invoking doFindNode.
	 * 
	 * @param key the key to be found
	 * @return this for fluent interface
	 */
	public abstract FindNodeOperation setKey(Key key);

	public abstract int getNrQueried();

	/**
	 * Do the find node recursive operation
	 * @return a list of nodes closest to the set key
	 */
	public abstract List<Node> doFindNode();

	public abstract void completed(KadMessage msg, Node n);

	public abstract void failed(Throwable exc, Node n);

}