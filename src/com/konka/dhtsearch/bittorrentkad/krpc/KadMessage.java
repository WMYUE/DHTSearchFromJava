package com.konka.dhtsearch.bittorrentkad.krpc;

import java.io.Serializable;

import com.konka.dhtsearch.Node;

/**
 * Base class for all openkad messages. All messages must be in this package
 * 
 *kadmessage id也就是bittorrent中的tt
 */
public abstract class KadMessage implements Serializable {

	private static final long serialVersionUID = -6975403100655787398L;
	private final long id;
	private final Node src;

	protected KadMessage(long id, Node src) {
		this.id = id;
		this.src = src;
	}

	public Node getSrc() {
		return src;
	}

	public long getId() {
		return id;
	}

}
