package com.konka.dhtsearch.bittorrentkad.krpc.find_node;

import java.util.List;

import org.yaircc.torrent.bencoding.BEncodedOutputStream;
import org.yaircc.torrent.bencoding.BMap;
import org.yaircc.torrent.bencoding.HashBMap;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadResponse;

/**
 * A findNode response as defined in the kademlia protocol
 * 
 * @author eyal.kibbar@gmail.com we extend the find node response in order for nodes to be able to indicate if they are interested in the value for their cache.
 */
public class FindNodeResponse extends KadResponse {

	private static final long serialVersionUID = 2103126060969733458L;
	private List<Node> nodes;
	private boolean cachedResults;
	// not in openKad - for vision.
	private boolean needed;

	public FindNodeResponse(String transaction, Node src) {
		super(transaction, src);
	}

	public FindNodeResponse setNodes(List<Node> nodes) {
		this.nodes = nodes;
		return this;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public FindNodeResponse setCachedResults(boolean cachedResults) {
		this.cachedResults = cachedResults;
		return this;
	}

	public boolean isCachedResults() {
		return cachedResults;
	}

	public boolean isNeeeded() {
		return needed;
	}

	public void setNeeeded(boolean neeeded) {
		this.needed = neeeded;
	}

	@Override
	public byte[] getBencodeData(Node to) {
	 
		BMap bMap = new HashBMap();
		bMap.put(TRANSACTION, transaction);
		bMap.put("y", "r");
		// ----------------------------------
		BMap a = new HashBMap();
		a.put("id", AppManager.getLocalNode().getKey().toBinaryString());// 自己的节点id
		a.put("nodes", "nodesnodes");// 对方的节点id
		bMap.put("r", a);
		// ----------------------------------
		return BEncodedOutputStream.bencode(bMap);
		
	}
}
