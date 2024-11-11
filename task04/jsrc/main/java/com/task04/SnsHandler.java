package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.events.SnsEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@LambdaHandler(lambdaName = "sns_handler",
		roleName = "sns_handler-role",
		isPublishVersion = false,
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@SnsEventSource(targetTopic = "lambda_topic")
@DependsOn(
		name = "lambda_topic",
		resourceType = ResourceType.SNS_TOPIC
)
public class SnsHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
		LambdaLogger lambdaLogger = context.getLogger();

		// Log the entire event for debugging
		lambdaLogger.log("Received SNS event: " + event);

		// Extract the SNS message (if it's structured as expected)
		List<Map<String, Object>> records = (List<Map<String, Object>>) event.get("Records");

		// If records exist, extract the message
		if (records != null && !records.isEmpty()) {
			Map<String, Object> snsRecord = records.get(0);  // Assuming we're handling a single message
			Map<String, Object> sns = (Map<String, Object>) snsRecord.get("Sns");
			String snsMessage = (String) sns.get("Message");

			// Log the SNS message
			lambdaLogger.log("Received SNS message: " + snsMessage);
			System.out.println("SNS message: " + snsMessage);
		}

		// Prepare a response
		Map<String, Object> result = new HashMap<>();
		result.put("statusCode", 200);
		result.put("body", "SNS message processed successfully");

		return result;
	}
}
