package io.macgyver.agent;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.macgyver.agent.MacGyverAgent.Sender;

public class MemorySender implements Sender {

	public List<ObjectNode> eventList = new ArrayList<>();
	
	@Override
	public void sendAppEvent(ObjectNode n) {
		eventList.add(n);

	}

	@Override
	public void sendCheckIn(ObjectNode n) {
		eventList.add(n);
	}

	@Override
	public void sendThreadDump(ObjectNode n) {
		eventList.add(n);
	}

	@Override
	public void sendAppConfigDump(ObjectNode n) {
		eventList.add(n);
	}

	ObjectNode last() {
		return eventList.get(eventList.size()-1);
	}
}
