package com.konka.dhtsearch.bittorrentkad.krpc.ping;

import org.yaircc.torrent.bencoding.BEncodedOutputStream;
import org.yaircc.torrent.bencoding.BMap;
import org.yaircc.torrent.bencoding.HashBMap;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadRequest;

/**
 * A ping request as defined in the kademlia protocol
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class PingRequest extends KadRequest {

	private static final long serialVersionUID = 4646089493549742900L;

	public PingRequest(String transaction, Node src) {
		super(transaction, src);
	}

	@Override
	public PingResponse generateResponse(Node localNode) {
		return new PingResponse(getTransaction(), localNode);
	}

	@Override
	public byte[] getBencodeData( ) {// ping 不需要对方的节点
		BMap bMap = new HashBMap();
		bMap.put(TRANSACTION, transaction);
		bMap.put("y", "q");
		bMap.put("q", "ping");
		// ----------------------------------
		BMap a = new HashBMap();
		a.put("id", AppManager.getLocalNode().getKey().toBinaryString());// 自己的节点id
		bMap.put("a", a);
		// ----------------------------------
		return BEncodedOutputStream.bencode(bMap);
	}
//	public byte[] getBencodeData() {// ping 不需要对方的节点
//		return getBencodeData(null);
//	}
	

}
