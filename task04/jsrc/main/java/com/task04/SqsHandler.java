package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.events.SqsTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;

import java.util.HashMap;
import java.util.Map;

@LambdaHandler(lambdaName = "sqs_handler",
		roleName = "sqs_handler-role",
		isPublishVersion = false,
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@DependsOn(
		name = "async_queue",
		resourceType = ResourceType.SQS_QUEUE
)
@SqsTriggerEventSource(queueName = "async_queue")  // Add the trigger here
public class SqsHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
		LambdaLogger lambdaLogger = context.getLogger();
		// Assuming the event is a map and contains the 'body' key as the message
		Object message = event.get("body");

		// Log the message content
		lambdaLogger.log("Received message: " + message);

		System.out.println("Message content: " + message);

		// Return a simple result
		Map<String, Object> result = new HashMap<>();
		result.put("statusCode", 200);
		result.put("body", "Message processed successfully");

		return result;
	}
}
