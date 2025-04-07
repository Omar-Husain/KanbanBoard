package main.java;

import javax.swing.*;
import main.java.client.KanbanClient;
import main.java.server.KanbanServer;

/**
 * Main entry point for the Kanban Board application.
 * Allows the user to start either the server or the client.
 */
public class Main {
    public static void main(String[] args) {
        // Set the look and feel to the system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Show a dialog to choose between server and client
        String[] options = {"Start Server", "Start Client", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
            null,
            "Welcome to Kanban Board Application\nPlease select an option:",
            "Kanban Board",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        switch (choice) {
            case 0: // Start Server
                startServer();
                break;
                
            case 1: // Start Client
                startClient();
                break;
                
            default: // Cancel or close
                System.exit(0);
                break;
        }
    }
    
    /**
     * Start the Kanban Board server
     */
    private static void startServer() {
        // Create a new thread to run the server
        new Thread(() -> {
            try {
                KanbanServer.main(new String[0]);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                    null,
                    "Error starting server: " + e.getMessage(),
                    "Server Error",
                    JOptionPane.ERROR_MESSAGE
                );
                e.printStackTrace();
            }
        }).start();
        
        // Ask if the user also wants to start a client
        int startClient = JOptionPane.showConfirmDialog(
            null,
            "Server started. Would you also like to start a client?",
            "Start Client",
            JOptionPane.YES_NO_OPTION
        );
        
        if (startClient == JOptionPane.YES_OPTION) {
            startClient();
        }
    }
    
    /**
     * Start the Kanban Board client
     */
    private static void startClient() {
        SwingUtilities.invokeLater(KanbanClient::new);
    }
}
