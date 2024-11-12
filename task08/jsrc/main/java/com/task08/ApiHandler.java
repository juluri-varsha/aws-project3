package com.task08;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.Architecture;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.HttpClient;
import com.google.gson.JsonObject;

@LambdaHandler(
		lambdaName = "api_handler",        // Lambda function name
		roleName = "api_handler-role",     // Role name
		layers = "sdk-layer",              // Specify the Lambda Layer for dependencies
		runtime = DeploymentRuntime.JAVA11, // Set the runtime to JAVA11
		architecture = Architecture.ARM64,  // Set architecture to ARM64
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED  // Set log expiration setting
)
@LambdaLayer(
		layerName = "sdk-layer",                                // Lambda Layer name
		libraries = {"lib/OpenMeteoApi-1.0-SNAPSHOT.jar"},       // List your custom libraries here
		runtime = DeploymentRuntime.JAVA11,                     // Set the runtime to JAVA11
		architectures = {Architecture.ARM64},                   // Set architecture to ARM64
		artifactExtension = ArtifactExtension.ZIP               // Define the artifact extension (ZIP)
)
@LambdaUrlConfig(
		authType = AuthType.NONE,                               // No authentication for URL
		invokeMode = InvokeMode.BUFFERED                        // Set invocation mode
)
public class ApiHandler implements RequestHandler<Object, String> {

	private static final String API_URL = "https://api.open-meteo.com/v1/forecast?latitude=50.4375&longitude=30.5&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m";  // Open-Meteo API URL

	@Override
	public String handleRequest(Object input, Context context) {
		try {
			// Create HTTP client and execute request
			HttpClient client = HttpClients.createDefault();
			HttpGet request = new HttpGet(API_URL);
			org.apache.http.HttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();

			// Convert the response to a string (JSON format)
			String result = EntityUtils.toString(entity);

			// Convert the result to a JsonObject and return as a string
			JsonObject weatherData = new JsonObject();
			weatherData.addProperty("weather_data", result);

			return weatherData.toString();  // Return the weather data as a JSON string
		} catch (Exception e) {
			// Log error to the Lambda context and return an error message
			context.getLogger().log("Error: " + e.getMessage());
			return "Error fetching weather data.";
		}
	}
}
