package com.task09;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.TracingMode;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@LambdaHandler(
		lambdaName = "processor",
		aliasName = "learn",
		roleName = "processor-role",
		isPublishVersion = false,
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED,
		tracingMode = TracingMode.Active
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED)
@EnvironmentVariables(
		@EnvironmentVariable(key = "target_table", value = "${target_table}")
)
public class Processor implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayV2HTTPResponse> {

	private static final Logger logger = Logger.getLogger(Processor.class.getName());
	private static final AmazonDynamoDB amazonDynamoDBClient = AmazonDynamoDBClientBuilder.defaultClient();
	private static final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDBClient);
	private static final String tableName = System.getenv("target_table");

	// Lambda handler function
	@Override
	public APIGatewayV2HTTPResponse handleRequest(APIGatewayProxyRequestEvent request, Context context) {
		APIGatewayV2HTTPResponse response;
		try {
			logger.info("Lambda invocation started");

			// Fetch weather data
			String weatherData = fetchWeatherData();
			logger.info("Weather data fetched successfully");

			ObjectMapper objectMapper = new ObjectMapper();
			WeatherForecast forecast = objectMapper.readValue(weatherData, WeatherForecast.class);

			// Process forecast data
			Map<String, Object> forecastMap = buildForecastMap(forecast);
			String id = UUID.randomUUID().toString();

			// Store forecast data in DynamoDB
			storeDataInDynamoDB(id, forecastMap);

			// Return successful response
			response = buildResponse(weatherData, 200);

		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Error processing weather data", ex);
			response = buildResponse("Internal Server Error", 500);
		}
		return response;
	}

	// Fetches weather data from Open-Meteo API
	private String fetchWeatherData() throws Exception {
		URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=50.4375&longitude=30.5&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");

		try (Scanner scanner = new Scanner(new InputStreamReader(conn.getInputStream()))) {
			StringBuilder response = new StringBuilder();
			while (scanner.hasNext()) {
				response.append(scanner.nextLine());
			}
			return response.toString();
		}
	}

	// Builds the forecast map to store in DynamoDB
	private Map<String, Object> buildForecastMap(WeatherForecast forecast) {
		Map<String, Object> forecastMap = new HashMap<>();
		forecastMap.put("elevation", forecast.getElevation());
		forecastMap.put("generationtime_ms", forecast.getGenerationtime_ms());
		forecastMap.put("latitude", forecast.getLatitude());
		forecastMap.put("longitude", forecast.getLongitude());
		forecastMap.put("timezone", forecast.getTimezone());
		forecastMap.put("timezone_abbreviation", forecast.getTimezone_abbreviation());
		forecastMap.put("utc_offset_seconds", forecast.getUtc_offset_seconds());

		// Hourly data
		Map<String, Object> hourlyMap = new HashMap<>();
		hourlyMap.put("time", forecast.getHourly().getTime());
		hourlyMap.put("temperature_2m", forecast.getHourly().getTemperature_2m());
		forecastMap.put("hourly", hourlyMap);

		// Hourly units
		Map<String, String> hourlyUnitsMap = new HashMap<>();
		hourlyUnitsMap.put("time", forecast.getHourly_units().getTime());
		hourlyUnitsMap.put("temperature_2m", forecast.getHourly_units().getTemperature_2m());
		forecastMap.put("hourly_units", hourlyUnitsMap);

		return forecastMap;
	}

	// Stores forecast data in DynamoDB
	private void storeDataInDynamoDB(String id, Map<String, Object> forecastMap) {
		Table table = dynamoDB.getTable(tableName);
		Item item = new Item()
				.withPrimaryKey("id", id)
				.withMap("forecast", forecastMap);
		table.putItem(item);
		logger.info("Data stored in DynamoDB with ID: " + id);
	}

	// Builds the response object
	private APIGatewayV2HTTPResponse buildResponse(String body, int statusCode) {
		return APIGatewayV2HTTPResponse.builder()
				.withStatusCode(statusCode)
				.withBody(body)
				.build();
	}
}
