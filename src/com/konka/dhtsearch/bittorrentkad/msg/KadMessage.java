package com.konka.dhtsearch.bittorrentkad.msg;

import java.io.Serializable;

import com.konka.dhtsearch.Node;

/**
 * Base class for all openkad messages.
 * All messages must be in this package
 * 
 *
 */
public abstract class KadMessage implements Serializable {

	private static final long serialVersionUID = -6975403100655787398L;
	private final long id;
	private final Node src;
	KadMessage(long id,  Node src) {
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
