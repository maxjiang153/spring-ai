/*
 * Copyright 2023 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.ai.bedrock;

import java.util.List;

import org.springframework.ai.bedrock.api.BedrockConverseApiUtils;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;

import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockDeltaEvent;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockStartEvent;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamOutput;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.MessageStopEvent;

/**
 * Amazon Bedrock Chat model converse interface generation metadata, encapsulating
 * information on the completion.
 *
 * @author Wei Jiang
 * @since 1.0.0
 */
public class BedrockConverseChatGenerationMetadata implements ChatGenerationMetadata {

	private String stopReason;

	private Message message;

	private ConverseStreamOutput event;

	public BedrockConverseChatGenerationMetadata(String stopReason, ConverseStreamOutput event) {
		super();

		this.stopReason = stopReason;
		this.event = event;
	}

	public BedrockConverseChatGenerationMetadata(String stopReason, Message message) {
		super();

		this.stopReason = stopReason;
		this.message = message;
	}

	public static BedrockConverseChatGenerationMetadata from(ConverseResponse response, Message message) {
		return new BedrockConverseChatGenerationMetadata(response.stopReasonAsString(), message);
	}

	public static BedrockConverseChatGenerationMetadata from(ConverseStreamOutput event) {
		String stopReason = null;

		if (event instanceof MessageStopEvent messageStopEvent) {
			stopReason = messageStopEvent.stopReasonAsString();
		}

		return new BedrockConverseChatGenerationMetadata(stopReason, event);
	}

	@Override
	public <T> T getContentFilterMetadata() {
		return null;
	}

	@Override
	public String getFinishReason() {
		return stopReason;
	}

	public void setFinishReason(String stopReason) {
		this.stopReason = stopReason;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public ConverseStreamOutput getEvent() {
		return event;
	}

	public boolean isToolUseEvent() {
		if (event instanceof ContentBlockStartEvent startEvent) {
			if (startEvent.start().toolUse() != null) {
				return false;
			}
		}

		if (event instanceof ContentBlockDeltaEvent deltaEvent) {
			if (deltaEvent.delta().toolUse() != null) {
				return false;
			}
		}

		return true;
	}

	public void generateEventMessage() {
		if (this.event == null) {
			return;
		}

		if (event instanceof ContentBlockDeltaEvent deltaEvent) {
			Message message = BedrockConverseApiUtils.createMessage(
					List.of(ContentBlock.builder().text(deltaEvent.delta().text()).build()),
					ConversationRole.ASSISTANT);
			setMessage(message);
		}
		else {
			setMessage(BedrockConverseApiUtils.EMPTY_MESSAGE);
		}
	}

}
