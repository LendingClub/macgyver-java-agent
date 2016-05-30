package io.macgyver.agent.decorator;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface StatusDecorator {

	public void decorate(ObjectNode
			status);
}
