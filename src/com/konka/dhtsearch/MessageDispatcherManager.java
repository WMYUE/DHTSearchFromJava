package com.konka.dhtsearch;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;

import com.konka.dhtsearch.bittorrentkad.net.KadServer;
import com.konka.dhtsearch.bittorrentkad.net.MessageDispatcher;

public class MessageDispatcherManager {
	Set<MessageDispatcher> messageDispatchers = new HashSet<MessageDispatcher>();

	public MessageDispatcherManager() {
		super();
	}

	public MessageDispatcher createMessageDispatcher(Timer timer, KadServer kadServer) {
		MessageDispatcher dispatcher = new MessageDispatcher(timer, kadServer);
		messageDispatchers.add(dispatcher);
		return dispatcher;
	}

	public void addMessageDispatcher(MessageDispatcher messageDispatcher) {
		messageDispatchers.add(messageDispatcher);
	}

	public MessageDispatcher findMessageDispatcherByTag(String tag) {
		for (MessageDispatcher messageDispatcher : messageDispatchers) {
			if (messageDispatcher.getTag() != null && messageDispatcher.getTag().equals(tag)) {
				return messageDispatcher;
			}
		}
		return null;
	}

	public void removeMessageDispatcher(MessageDispatcher messageDispatcher) {
		messageDispatchers.remove(messageDispatcher);
	}
}
