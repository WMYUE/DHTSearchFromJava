package com.konka.dhtsearch.bittorrentkad.krpc;

import java.io.Serializable;

import com.konka.dhtsearch.Node;

/**
 * A message containing arbitrary data to be used by the KeybasedRouting.sendMessage method
 * 
 *
 */
public class ContentMessage extends KadMessage {

	private static final long serialVersionUID = -57547778613163861L;

	private String tag;
	private Serializable content;

	ContentMessage(long id, Node src) {   
		super(id, src);
	}

	/**
	 * Every content request has a tag associated with it. This is the same tag given in the KeybasedRouting.sendMessage or sendRequest methods.
	 * 
	 * @return the message's tag
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * Any arbitrary data
	 * 
	 * @return the data
	 */
	public Serializable getContent() {
		return content;
	}

	public ContentMessage setContent(Serializable content) {
		this.content = content;
		return this;
	}

	public ContentMessage setTag(String tag) {
		this.tag = tag;
		return this;
	}

}
