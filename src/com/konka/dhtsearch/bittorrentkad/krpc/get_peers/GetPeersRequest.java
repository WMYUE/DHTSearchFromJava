package com.konka.dhtsearch.bittorrentkad.krpc.get_peers;

import java.io.Serializable;

import org.yaircc.torrent.bencoding.BEncodedOutputStream;
import org.yaircc.torrent.bencoding.BMap;
import org.yaircc.torrent.bencoding.HashBMap;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadRequest;
import com.konka.dhtsearch.util.Util;

/**
 * A message containing arbitrary data to be used by the
 * KeybasedRouting.sendRequest methods
 * 
 */
public class GetPeersRequest extends KadRequest {

	private static final long serialVersionUID = 918433377540165654L;

	private String info_hash;

	public GetPeersRequest(String transaction, Node src) {
		super(transaction, src);
	}

	public String getInfo_hash() {
		return info_hash;
	}

	public GetPeersRequest setInfo_hash(String info_hash) {
		this.info_hash = info_hash;
		return this;
	}

	@Override
	public GetPeersResponse generateResponse(Node localNode) {
		return new GetPeersResponse(getTransaction(), localNode);
	}

	// get_peers Query = {"t":"aa", "y":"q", "q":"get_peers", "a":
	// {"id":"abcdefghij0123456789", "info_hash":"mnopqrstuvwxyz123456"}}
	// bencoded =
	// d1:ad2:id20:abcdefghij01234567899:info_hash20:mnopqrstuvwxyz123456e1:q9:get_peers1:t2:aa1:y1:qe
	@Override
	public byte[] getBencodeData() {
		BMap bMap = new HashBMap();
		bMap.put(TRANSACTION, Util.HexString2Bytes(transaction));
		bMap.put("y", "q");
		bMap.put("q", "get_peers");
		// ----------------------------------
		BMap a = new HashBMap();
		a.put("id", AppManager.getLocalNode().getKey().getBytes());// 自己的节点id
		a.put("info_hash", info_hash);// 对方的节点id **这里应该是你要查询的id
		bMap.put("a", a);
		// ----------------------------------
		byte[] bb = BEncodedOutputStream.bencode(bMap);
		return bb;
	}

}
