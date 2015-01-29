package com.konka.dhtsearch.bittorrentkad.krpc.find_node;

import java.util.List;

import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadResponse;

/**
 * A findNode response as defined in the kademlia protocol
 */
public class FindNodeResponse1 extends KadResponse {

	private static final long serialVersionUID = 2103126060969733458L;
	private List<Node> nodes;
	private boolean cachedResults;
	// not in openKad - for vision.
	private boolean needed;

	public FindNodeResponse1(String transaction, Node src) {
		super(transaction, src);
	}

	public FindNodeResponse1 setNodes(List<Node> nodes) {
		this.nodes = nodes;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.technion.ewolf.kbr.openkad.msg.FindNodeResponse#getNodes()
	 */
	public List<Node> getNodes() {
		return nodes;
	}

	public FindNodeResponse1 setCachedResults(boolean cachedResults) {
		this.cachedResults = cachedResults;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.technion.ewolf.kbr.openkad.msg.FindNodeResponse#isCachedResults()
	 */
	public boolean isCachedResults() {
		return cachedResults;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.technion.ewolf.kbr.openkad.msg.FindNodeResponse#isNeeeded()
	 */
	public boolean isNeeeded() {
		return needed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see il.technion.ewolf.kbr.openkad.msg.FindNodeResponse#setNeeeded(boolean)
	 */
	public void setNeeeded(boolean neeeded) {
		this.needed = neeeded;
	}

}
