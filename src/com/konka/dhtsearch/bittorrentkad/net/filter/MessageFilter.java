package com.konka.dhtsearch.bittorrentkad.net.filter;

import com.konka.dhtsearch.bittorrentkad.krpc.KadMessage;

/**
 * Interface for all message filters
 * @author eyal.kibbar@gmail.com
 *
 */
public interface MessageFilter {

	boolean shouldHandle(KadMessage m);
	
}
