package com.konka.dhtsearch.bittorrentkad.krpc;

import java.io.Serializable;

import org.yaircc.torrent.bencoding.BMap;

import com.konka.dhtsearch.Node;

/**
 * Base class for all openkad messages. All messages must be in this package
 * 
 * kadmessage id也就是bittorrent中的tt
 */
public abstract class KadMessage implements Serializable {
	public static final String TRANSACTION = "t";
	private static final long serialVersionUID = -6975403100655787398L;
	private final String transaction;
	private final Node src;
	private BMap bMap;

	public BMap getbMap() {
		return bMap;
	}

	public KadMessage setbMap(BMap bMap) {
		this.bMap = bMap;
		return this;
	}

	protected KadMessage(String transaction, Node src) {
		this.transaction = transaction;
		this.src = src;
	}

	public Node getSrc() {
		return src;
	}

	public String getTransaction() {
		return transaction;
	}

}
