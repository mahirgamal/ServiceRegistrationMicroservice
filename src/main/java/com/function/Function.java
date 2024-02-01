package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it
     * using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    private static final Logger logger = Logger.getLogger(Function.class.getName());
    private static final String DB_URL = "jdbc:mysql://leisadb.mysql.database.azure.com:3306/leisa";
    private static final String DB_USER = "lei";
    private static final String DB_PASSWORD = "mahirgamal123#";

    static {
        try {
            // Load the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @FunctionName("createService")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, route = "registration/create", authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<User>> request,
            final ExecutionContext context) {

        // Extract user from request body
        User user = request.getBody().orElse(null);
        if (user == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Invalid user data").build();
        }

        if (isUserExists(user.getUsername(), user.getEmail())) {
            return request.createResponseBuilder(HttpStatus.CONFLICT).body("User already exists").build();
        }

        String queueName = generateRandomQueueName();
        if (rabbitmqCreateUser(user.getUsername() + "_" + queueName, user.getUsername(), user.getPassword())) {
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());

            user.setPassword(hashedPassword);
            user.setQueuename(user.getUsername() + "_" + queueName);
            try (java.sql.Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO users (username, email, password, queuename, admin) VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, user.getUsername());
                    stmt.setString(2, user.getEmail());
                    stmt.setString(3, user.getPassword()); // Consider encrypting the password
                    stmt.setString(4, user.getQueuename());
                    stmt.setBoolean(5, user.getAdmin() == Boolean.TRUE);

                    int affectedRows = stmt.executeUpdate();

                    if (affectedRows > 0) {
                        return request.createResponseBuilder(HttpStatus.OK).body("User created").build();
                    } else {
                        return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Failed to create user").build();
                    }
                }
            } catch (Exception e) {
                context.getLogger().severe("Database connection error: " + e.getMessage());
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Database connection error")
                        .build();
            }
        } else
            return request.createResponseBuilder(HttpStatus.EXPECTATION_FAILED)
                    .body("Couldn't create message user").build();

        // Implement user creation logic here
        // ...

    }

    public String generateRandomQueueName() {
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder queueName = new StringBuilder();

        // Generate a random 10-character queue name
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            int randomIndex = random.nextInt(characters.length());
            queueName.append(characters.charAt(randomIndex));
        }

        return queueName.toString();
    }

    public boolean rabbitmqCreateUser(String randomQueueName, String brokerUsername, String brokerPassword) {
        try {
            // Read the JSON file (e.g., 'config.json')
            // You'll need to use a library like Jackson or Gson for JSON parsing in Java
            // Here, we assume that you have a Config class to represent the configuration
            // object

            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("rabbitmqconfig.json");
            if (inputStream == null) {
                throw new IOException("rabbitmqconfig.json file not found in resources");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String configJSON = reader.lines().collect(Collectors.joining("\n"));
            JSONObject config = new JSONObject(configJSON);

            // Configuration values
            String brokerType = config.getString("brokerType");
            String brokerProtocol = config.getString("brokerProtocol");
            String brokerHost = config.getString("brokerHost");
            int brokerPort = config.getInt("brokerPort");
            String username = config.getString("brokerUsername");
            String password = config.getString("brokerPassword");

            logger.info("Broker Type: " + brokerType);
            logger.info("Broker Protocol: " + brokerProtocol);
            logger.info("Broker Host: " + brokerHost);
            logger.info("Broker Port: " + brokerPort);
            logger.info("Username: " + username);
            logger.info("Password: " + password);

            // Create a connection to RabbitMQ
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(brokerHost);
            factory.setPort(brokerPort);
            factory.setUsername(username);
            factory.setPassword(password);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // Declare the random queue
            channel.queueDeclare(randomQueueName, false, false, false, null);

            // Close the RabbitMQ connection and channel
            channel.close();
            connection.close();
            logger.info("Queue " + randomQueueName + " created successfully.");

            String brokerApiBaseUrl = config.getString("apiUrl");

            logger.info("brokerApiBaseUrl: " + brokerApiBaseUrl);

            HttpClient client = HttpClient.newBuilder()
                    .build();

            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            String userJson = "{\"password\":\"" + brokerPassword + "\",\"tags\":\"management\"}"; // Modify tags as
                                                                                                   // needed

            HttpRequest createUserRequest = HttpRequest.newBuilder()
                    .uri(URI.create(brokerApiBaseUrl + "/users/" + brokerUsername))
                    .timeout(Duration.ofMinutes(1))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + encodedAuth)
                    .PUT(HttpRequest.BodyPublishers.ofString(userJson))
                    .build();

            HttpResponse<String> createUserResponse = client.send(createUserRequest,
                    HttpResponse.BodyHandlers.ofString());

            logger.info("Create user response status: " + createUserResponse.statusCode());
            logger.info("Create user response body: " + createUserResponse.body());

            if (createUserResponse.statusCode() == 201) {
                logger.info("User created successfully");

                // Set permissions
                String permissionsJson = String.format("{\"configure\":\"\",\"write\":\".*\",\"read\":\"^%s$\"}",
                        randomQueueName);

                String setPermissionsUrl = brokerApiBaseUrl + "/permissions/%2F/" + brokerUsername; // %2F is
                                                                                                    // URL-encoded "/"

                HttpRequest setPermissionsRequest = HttpRequest.newBuilder()
                        .uri(URI.create(setPermissionsUrl))
                        .timeout(Duration.ofMinutes(1))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Basic " + encodedAuth)
                        .PUT(HttpRequest.BodyPublishers.ofString(permissionsJson))
                        .build();

                HttpResponse<String> setPermissionsResponse = client.send(setPermissionsRequest,
                        HttpResponse.BodyHandlers.ofString());

                if (setPermissionsResponse.statusCode() == 201 || createUserResponse.statusCode() == 204) {
                    logger.info("Permissions set successfully for user " + brokerUsername);
                    return true;
                } else {
                    logger.info("Failed to set permissions for user " + brokerUsername + ": "
                            + setPermissionsResponse.statusCode());
                    return false;
                }
            } else {
                System.err.println("Failed to create user " + brokerUsername + ". " + createUserResponse.statusCode());
                logger.warning("Failed to create user. Status code: " + createUserResponse.statusCode());

                return false;
            }

            // Additional code for setting permissions would be similar to above

        } catch (IOException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
            logger.warning("Failed to create user. Status code: " + e.getMessage());

            return false;
        }
    }

    public boolean isUserExists(String username, String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";

        try (java.sql.Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                }
            }
        } catch (SQLException e) {
            // Handle exceptions (log it or throw as needed)
            e.printStackTrace();
        }

        return false;
    }

}
