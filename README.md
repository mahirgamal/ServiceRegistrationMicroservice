
# Service Registration Microservice

## Overview

The `ServiceRegistrationMicroservice` project is a Java-based microservice designed to handle the registration of services and manage related user interactions. This microservice leverages database integration, message queues, and RESTful services to provide robust service registration functionality. It is an essential part of the broader system architecture, enabling efficient and scalable service management.

## Features

- **Service Registration**: Manages the registration of new services with validation and persistence.
- **User Management**: Handles user-related operations, possibly including authentication, user data management, and authorization.
- **Database Integration**: Uses MySQL for storing service and user information.
- **Message Queuing**: Integrates with RabbitMQ for handling asynchronous message processing.
- **REST API**: Exposes RESTful endpoints for interacting with the service registration and user management features.

## Project Structure

```
/ServiceRegistrationMicroservice
│
├── .git                      # Git configuration directory
├── .gitignore                # Git ignore file
├── host.json                 # Configuration file for hosting on Azure Functions
├── local.settings.json       # Local environment settings file
├── pom.xml                   # Project Object Model file for Maven
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       ├── domain
│   │   │       │   └── Registration.java        # Handles service registration logic
│   │   │       └── function
│   │   │           ├── Function.java             # Core functions related to service operations
│   │   │           └── User.java                 # User management logic
│   │   │
│   │   └── resources
│   │       ├── mysqlconfig.json                 # MySQL database configuration
│   │       └── rabbitmqconfig.json              # RabbitMQ configuration
│   │
│   └── test
│       └── java
│           └── com
│               └── function
│                   ├── FunctionTest.java         # Unit tests for Function class
│                   └── HttpResponseMessageMock.java # Mocking HTTP responses for tests
│
└── target                      # Directory for compiled classes and build artifacts
```

## Requirements

- **Java 8** or higher
- **Maven** for building the project and managing dependencies
- **MySQL** for database operations
- **RabbitMQ** for message queuing

## Setup

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/yourusername/ServiceRegistrationMicroservice.git
   ```
2. **Navigate to the Project Directory:**
   ```bash
   cd ServiceRegistrationMicroservice
   ```
3. **Configure Database and Message Queue:**
   - Update the `mysqlconfig.json` file with your MySQL connection details.
   - Update the `rabbitmqconfig.json` file with your RabbitMQ server details.

4. **Build the Project using Maven:**
   ```bash
   mvn clean install
   ```
5. **Run the Application:**
   ```bash
   java -jar target/ServiceRegistrationMicroservice-1.0-SNAPSHOT.jar
   ```

## Usage

1. **Service Registration:**
   - Use the `Registration` class to register new services.
   - Example usage:
     ```java
     Registration.registerService(serviceDetails);
     ```

2. **User Management:**
   - Use the `User` class to manage user-related operations.
   - Example usage:
     ```java
     User.createUser(username, password);
     ```

## Contributing

Contributions are welcome! Please open an issue or submit a pull request for any improvements or bug fixes.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](https://github.com/mahirgamal/ServiceRegistrationMicroservice/blob/main/LICENSE) file for details.

## Contact

If you have any questions, suggestions, or need assistance, please don't hesitate to contact us at [mhabib@csu.edu.au](mailto:mhabib@csu.edu.au) or [akabir@csu.edu.au](mailto:akabir@csu.edu.au).
