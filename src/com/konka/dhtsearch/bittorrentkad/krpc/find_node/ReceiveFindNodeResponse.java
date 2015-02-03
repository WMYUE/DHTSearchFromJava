package com.konka.dhtsearch.bittorrentkad.krpc.find_node;

import java.util.List;

import org.yaircc.torrent.bencoding.BEncodedOutputStream;
import org.yaircc.torrent.bencoding.BMap;
import org.yaircc.torrent.bencoding.BTypeException;
import org.yaircc.torrent.bencoding.HashBMap;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadResponse;

/**
 * A findNode response as defined in the kademlia protocol
 * 
 */
public class ReceiveFindNodeResponse extends KadResponse {

	private static final long serialVersionUID = 2103126060969733458L;
	private List<Node> nodes;
//	private BMap bMap;

	public ReceiveFindNodeResponse(String transaction, Node src) {
		super(transaction, src);
	}

	// {"t":"aa", "y":"r", "r": {"id":"0123456789abcdefghij", "nodes": "def456..."}}
	public ReceiveFindNodeResponse(String transaction, BMap bMap, Node src) throws BTypeException {
		super(transaction, src);
//		this.bMap = bMap;
		String y = bMap.getString("y");
		if ("r".equals(y)) {
			BMap r = bMap.getMap("r");
			String id=r.getString("id");
			String nodes=r.getString("nodes");
			System.out.println(nodes);
		}
	}

	public List<Node> getNodes() {
		return nodes;
	}

	@Override
	public byte[] getBencodeData() {

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
