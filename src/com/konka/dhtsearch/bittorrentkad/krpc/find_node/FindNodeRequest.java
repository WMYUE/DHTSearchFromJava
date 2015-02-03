package com.konka.dhtsearch.bittorrentkad.krpc.find_node;

import org.yaircc.torrent.bencoding.BEncodedOutputStream;
import org.yaircc.torrent.bencoding.BMap;
import org.yaircc.torrent.bencoding.HashBMap;

import com.konka.dhtsearch.AppManager;
import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;
import com.konka.dhtsearch.bittorrentkad.krpc.KadRequest;
import com.konka.dhtsearch.util.Util;

/**
 * A findNode request as defined in the kademlia protocol
 * 
 * 接收请求主要接受两个参数 1，transaction，2 发送者的node（其他参数 对方id）
 */
public class FindNodeRequest extends KadRequest {

	private static final long serialVersionUID = -7084922793331210968L;
	private Key key;
	private boolean searchCache;
	

	public FindNodeRequest(String transaction, Node src) {
		super(transaction, src);
	}

	public static FindNodeRequest creatLocalFindNodeRequest(Node src) {

		FindNodeRequest findNodeRequest = new FindNodeRequest(Util.random_tranctionId(), src);
		return findNodeRequest;
	}

	/**
	 * 
	 * @return the key we are searching
	 */
	public Key getKey() {
		return key;
	}

	public FindNodeRequest setKey(Key key) {
		this.key = key;
		return this;
	}

	@Override
	public FindNodeResponse generateResponse(Node localNode) {// 回复对方请求时候调用
		return new FindNodeResponse(getTransaction(), localNode);
	}

	public FindNodeRequest setSearchCache(boolean searchCache) {
		this.searchCache = searchCache;
		return this;
	}

	public boolean shouldSearchCache() {
		return searchCache;
	}

	/**
	 * 编码
	 */
	@Override
	public byte[] getBencodeData() {
		BMap bMap = new HashBMap();
		bMap.put(TRANSACTION, transaction);
		bMap.put("y", "q");
		bMap.put("q", "find_node");
		// ----------------------------------
		BMap a = new HashBMap();
		a.put("id", AppManager.getLocalNode().getKey().getKeyid());// 自己的节点id
		a.put("target", getSrc().getKey().toString());// 对方的节点id
		bMap.put("a", a);
		// ----------------------------------
		return BEncodedOutputStream.bencode(bMap);
	}

}
