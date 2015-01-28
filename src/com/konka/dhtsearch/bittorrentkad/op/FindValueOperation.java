package com.konka.dhtsearch.bittorrentkad.op;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;

/**
 * Base class for all the find value operations
 * @author eyal.kibbar@gmail.com
 *
 */
public abstract class FindValueOperation {

	protected Key key;
	
	private Collection<Node> bootstrap;
	
	protected FindValueOperation() {
		bootstrap = Collections.emptySet();
	}
	
	public FindValueOperation setKey(Key key) {
		this.key = key;
		return this;
	}
	
	public FindValueOperation setBootstrap(Collection<Node> bootstrap) {
		this.bootstrap = bootstrap;
		return this;
	}
	
	protected Collection<Node> getBootstrap() {
		return bootstrap;
	}
	
	public abstract int getNrQueried();
	
	public abstract List<Node> doFindValue();
	
}
