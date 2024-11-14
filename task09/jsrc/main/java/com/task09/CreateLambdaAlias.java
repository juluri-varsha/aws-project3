package com.task09;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.CreateAliasRequest;
import com.amazonaws.services.lambda.model.CreateAliasResult;

public class CreateLambdaAlias {

    public static void main(String[] args) {
        String functionName = "processor";  // Lambda function name
        String aliasName = "learn";         // Alias name
        String functionVersion = "1";       // Lambda function version (you can specify the version)

        // Initialize AWS Lambda client
        AWSLambda lambdaClient = AWSLambdaClientBuilder.standard()
                .withCredentials(new ProfileCredentialsProvider()) // Ensure this profile has access to Lambda
                .build();

        try {
            // Create alias for the Lambda function
            CreateAliasRequest createAliasRequest = new CreateAliasRequest()
                    .withFunctionName(functionName)
                    .withName(aliasName)
                    .withFunctionVersion(functionVersion);

            CreateAliasResult result = lambdaClient.createAlias(createAliasRequest);
            System.out.println("Alias created successfully: " + result.getAliasArn());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
