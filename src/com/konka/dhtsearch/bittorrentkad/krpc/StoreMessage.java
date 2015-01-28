package com.konka.dhtsearch.bittorrentkad.krpc;

import java.util.List;

import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;

/**
 * A store results message to be inserted to the destination node's cache
 * 一个存储结果信息被插入到目标节点的缓存
 */
public class StoreMessage extends KadMessage {

	private static final long serialVersionUID = 3908967205635902724L;

	private Key key;
	private List<Node> nodes;

	StoreMessage(long id, Node src) {
		super(id, src);
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public Key getKey() {
		return key;
	}

	public StoreMessage setKey(Key key) {
		this.key = key;
		return this;
	}

	public StoreMessage setNodes(List<Node> nodes) {
		this.nodes = nodes;
		return this;
	}

}
