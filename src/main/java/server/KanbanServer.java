package main.java.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server for the Kanban Board application.
 * Manages client connections and synchronizes the board state between clients.
 */
public class KanbanServer {
    private static final int PORT = 5000;
    private static final Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    
    // The shared Kanban board data structure
    private static final List<KanbanColumn> kanbanBoard = Collections.synchronizedList(new ArrayList<>());
    
    // File to save/load the board state
    private static final String BOARD_FILE = "kanban_board.dat";
    
    public static void main(String[] args) {
        // Initialize the board with default columns
        initializeBoard();
        
        // Load saved board state if available
        loadBoardState();
        
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Kanban Board Server started on port " + PORT);
            System.out.println("Waiting for clients to connect...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);
                
                // Create and start a new client handler thread
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Initialize the Kanban board with default columns
     */
    private static void initializeBoard() {
        kanbanBoard.add(new KanbanColumn("To Do"));
        kanbanBoard.add(new KanbanColumn("In Progress"));
        kanbanBoard.add(new KanbanColumn("Done"));
        kanbanBoard.add(new KanbanColumn("Completed"));
        
        System.out.println("Initialized board with columns:");
        for (KanbanColumn column : kanbanBoard) {
            System.out.println("- " + column.getName());
        }
    }
    
    /**
     * Load the board state from file
     */
    private static void loadBoardState() {
        try {
            File file = new File(BOARD_FILE);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);
                
                @SuppressWarnings("unchecked")
                List<KanbanColumn> savedBoard = (List<KanbanColumn>) ois.readObject();
                
                if (savedBoard != null && !savedBoard.isEmpty()) {
                    kanbanBoard.clear();
                    kanbanBoard.addAll(savedBoard);
                    System.out.println("Board state loaded from file.");
                }
                
                ois.close();
                fis.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading board state: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Save the current board state to file
     */
    private static void saveBoardState() {
        try {
            FileOutputStream fos = new FileOutputStream(BOARD_FILE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(kanbanBoard);
            oos.close();
            fos.close();
            System.out.println("Board state saved to file.");
        } catch (IOException e) {
            System.err.println("Error saving board state: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Broadcast a message to all connected clients
     */
    private static void broadcastMessage(String message) {
        synchronized (clientHandlers) {
            for (ClientHandler handler : clientHandlers) {
                handler.sendMessage(message);
            }
        }
    }
    
    /**
     * Broadcast the current board state to all clients
     */
    private static void broadcastBoardState() {
        String boardState = serializeBoardState();
        broadcastMessage("BOARD_UPDATE|" + boardState);
        
        // Save the board state to file after each update
        saveBoardState();
    }
    
    /**
     * Serialize the board state to a string for transmission
     */
    private static String serializeBoardState() {
        StringBuilder sb = new StringBuilder();
        
        System.out.println("Serializing board with columns:");
        synchronized (kanbanBoard) {
            for (int i = 0; i < kanbanBoard.size(); i++) {
                KanbanColumn column = kanbanBoard.get(i);
                System.out.println("- " + column.getName() + " (tasks: " + column.getTasks().size() + ")");
                
                sb.append(column.getName()).append(":");
                
                List<Task> tasks = column.getTasks();
                for (int j = 0; j < tasks.size(); j++) {
                    Task task = tasks.get(j);
                    sb.append(task.getId()).append(",")
                      .append(task.getTitle()).append(",")
                      .append(task.getDescription()).append(",")
                      .append(task.getAssignee()).append(",")
                      .append(task.getDueDate());
                    
                    if (j < tasks.size() - 1) {
                        sb.append(";");
                    }
                }
                
                if (i < kanbanBoard.size() - 1) {
                    sb.append("|");
                }
            }
        }
        
        System.out.println("Serialized board state: " + sb.toString());
        return sb.toString();
    }
    
    /**
     * Add a new task to the specified column
     */
    private static void addTask(String columnName, Task task) {
        synchronized (kanbanBoard) {
            for (KanbanColumn column : kanbanBoard) {
                if (column.getName().equals(columnName)) {
                    column.addTask(task);
                    break;
                }
            }
        }
        
        broadcastBoardState();
    }
    
    /**
     * Move a task from one column to another
     */
    private static void moveTask(String taskId, String fromColumn, String toColumn) {
        Task taskToMove = null;
        
        synchronized (kanbanBoard) {
            // Find and remove the task from the source column
            for (KanbanColumn column : kanbanBoard) {
                if (column.getName().equals(fromColumn)) {
                    for (Task task : column.getTasks()) {
                        if (task.getId().equals(taskId)) {
                            taskToMove = task;
                            column.removeTask(task);
                            break;
                        }
                    }
                    break;
                }
            }
            
            // Add the task to the destination column
            if (taskToMove != null) {
                for (KanbanColumn column : kanbanBoard) {
                    if (column.getName().equals(toColumn)) {
                        column.addTask(taskToMove);
                        break;
                    }
                }
            }
        }
        
        broadcastBoardState();
    }
    
    /**
     * Update an existing task
     */
    private static void updateTask(String columnName, Task updatedTask) {
        synchronized (kanbanBoard) {
            for (KanbanColumn column : kanbanBoard) {
                if (column.getName().equals(columnName)) {
                    for (int i = 0; i < column.getTasks().size(); i++) {
                        Task task = column.getTasks().get(i);
                        if (task.getId().equals(updatedTask.getId())) {
                            column.getTasks().set(i, updatedTask);
                            break;
                        }
                    }
                    break;
                }
            }
        }
        
        broadcastBoardState();
    }
    
    /**
     * Delete a task from the board
     */
    private static void deleteTask(String columnName, String taskId) {
        synchronized (kanbanBoard) {
            for (KanbanColumn column : kanbanBoard) {
                if (column.getName().equals(columnName)) {
                    column.getTasks().removeIf(task -> task.getId().equals(taskId));
                    break;
                }
            }
        }
        
        broadcastBoardState();
    }
    
    /**
     * Clear all tasks from the "Done" and "Completed" columns
     */
    private static void clearCompletedTasks() {
        synchronized (kanbanBoard) {
            for (KanbanColumn column : kanbanBoard) {
                if (column.getName().equals("Done") || column.getName().equals("Completed")) {
                    column.getTasks().clear();
                }
            }
        }
        
        broadcastBoardState();
    }
    
    /**
     * Handler for client connections
     */
    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                // Set up input and output streams
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                // Get username from client
                username = in.readLine().split("\\|")[1];
                System.out.println("User connected: " + username);
                
                // Add client to the map
                synchronized (clients) {
                    clients.put(username, this);
                }
                
                // Send the current board state to the new client
                sendMessage("BOARD_UPDATE|" + serializeBoardState());
                
                // Notify all clients about the new user
                broadcastMessage("USER_JOINED|" + username);
                
                // Process client messages
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    processClientMessage(inputLine);
                }
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                // Clean up when client disconnects
                try {
                    if (socket != null) socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                // Remove client from collections
                clientHandlers.remove(this);
                if (username != null) {
                    synchronized (clients) {
                        clients.remove(username);
                    }
                    
                    // Notify all clients about the user leaving
                    broadcastMessage("USER_LEFT|" + username);
                }
                
                System.out.println("Client disconnected: " + username);
            }
        }
        
    /**
     * Process a message from the client
     */
    private void processClientMessage(String message) {
        String[] parts = message.split("\\|");
        String command = parts[0];
        
        switch (command) {
            case "ADD_TASK":
                // Format: ADD_TASK|columnName|taskId|title|description|assignee|dueDate
                String columnName = parts[1];
                Task newTask = new Task(
                    parts[2], // id
                    parts[3], // title
                    parts[4], // description
                    parts[5], // assignee
                    parts[6]  // dueDate
                );
                addTask(columnName, newTask);
                break;
                
            case "MOVE_TASK":
                // Format: MOVE_TASK|taskId|fromColumn|toColumn
                moveTask(parts[1], parts[2], parts[3]);
                break;
                
            case "UPDATE_TASK":
                // Format: UPDATE_TASK|columnName|taskId|title|description|assignee|dueDate
                Task updatedTask = new Task(
                    parts[2], // id
                    parts[3], // title
                    parts[4], // description
                    parts[5], // assignee
                    parts[6]  // dueDate
                );
                updateTask(parts[1], updatedTask);
                break;
                
            case "DELETE_TASK":
                // Format: DELETE_TASK|columnName|taskId
                deleteTask(parts[1], parts[2]);
                break;
                
            case "CLEAR_COMPLETED":
                // Format: CLEAR_COMPLETED
                clearCompletedTasks();
                break;
                
            case "CHAT_MESSAGE":
                // Format: CHAT_MESSAGE|message
                broadcastMessage("CHAT|" + username + "|" + parts[1]);
                break;
        }
        }
        
        /**
         * Send a message to this client
         */
        public void sendMessage(String message) {
            out.println(message);
        }
    }
}
