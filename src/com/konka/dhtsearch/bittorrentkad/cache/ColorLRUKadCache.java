package com.konka.dhtsearch.bittorrentkad.cache;

import java.util.List;

import com.konka.dhtsearch.Key;
import com.konka.dhtsearch.Node;

/**
 * Same as LRU cache but only inserts keys with the local node's color
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class ColorLRUKadCache extends LRUKadCache {

	private int nrColors;
	private int myColor;

	ColorLRUKadCache(int size, int kBucketSize, int nrColors, int myColor) {
		super(size, kBucketSize);
		this.nrColors = nrColors;
		this.myColor = myColor;
	}

	@Override
	public void insert(Key key, List<Node> nodes) {
		if (isFull() && key.getColor(nrColors) != myColor)
			return;
		super.insert(key, nodes);
	}
}
