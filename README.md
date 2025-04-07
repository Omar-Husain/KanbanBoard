# Kanban Board Application

Name : Omar Husain 100491847

## Project Overview

This Kanban Board application is a multi-user task management system that enables teams to collaborate on projects in real-time. It follows the client-server architecture using sockets for communication and multi-threading to handle multiple client connections.

![Kanban Board Screenshot](screenshot.png)

### Features

- Real-time task management with multiple users
- Moving tasks between columns
- Task creation, editing, and deletion
- Chat functionality for team communication
- Persistent storage of the board state

## How to Run

### Prerequisites

- Java 11 or higher
- Maven (for building)

### Building the Application

1. Clone the repository
2. Navigate to the project directory
3. Build the project using Maven:

```
mvn clean package
```

This will create a JAR file with all dependencies in the `target` directory.

### Running the Application

You can run the application using the JAR file:

```
java -jar target/KanbanBoard-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Alternatively, you can run it directly from Maven:

```
mvn exec:java -Dexec.mainClass="main.java.Main"
```

When you start the application, you'll be prompted to choose whether to start a server, a client, or both:

1. **Start Server**: This will start the Kanban board server that manages the board state and handles client connections.
2. **Start Client**: This will start a client application that connects to the server.

For a multi-user setup:
- Start one instance of the server
- Start multiple instances of the client (one for each user)

By default, the server runs on localhost port 5000. All clients must be on the same network as the server.

## Architecture

The application follows a client-server architecture:

- **Server**: Manages the Kanban board state and handles client connections
  - Uses multi-threading to handle multiple client connections
  - Synchronizes board updates between clients
  - Persists the board state to disk

- **Client**: Provides a GUI for interacting with the Kanban board
  - Connects to the server via sockets
  - Updates the UI in real-time based on server messages
  - Allows users to create, edit, move, and delete tasks

## Implementation Details

The application implements several concepts from the CSCI 2020U course:

- **Socket Programming**: Used for client-server communication
- **Multi-threading**: Used to handle multiple client connections
- **GUI Development**: Using Java Swing for the client interface
- **File I/O**: Used for persisting the board state
- **Serialization**: Used for transmitting data between client and server

## Project Structure

- `src/main/java/Main.java`: Entry point for the application
- `src/main/java/server/`: Server-side code
  - `KanbanServer.java`: Main server class
  - `KanbanColumn.java`: Represents a column in the Kanban board
  - `Task.java`: Represents a task in the Kanban board
- `src/main/java/client/`: Client-side code
  - `KanbanClient.java`: Main client class with GUI

## Acknowledgments

This project was developed as part of the CSCI 2020U course, incorporating concepts such as socket programming, multi-threading, and GUI development.

## Changelog


