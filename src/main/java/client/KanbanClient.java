package main.java.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.UUID;

/**
 * Client application for the Kanban Board
 * Provides a GUI for interacting with the Kanban board server
 */
public class KanbanClient extends JFrame {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    
    // GUI Components
    private JPanel mainPanel;
    private JPanel boardPanel;
    private JPanel chatPanel;
    private JTextArea chatArea;
    private JTextField chatField;
    private Map<String, JPanel> columnPanels;
    private Map<String, Map<String, TaskCard>> taskCards;
    
    // Data
    private List<ColumnData> columns;
    
    public KanbanClient() {
        // Set up the frame
        super("Kanban Board");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        // Initialize data structures
        columnPanels = new HashMap<>();
        taskCards = new HashMap<>();
        columns = new ArrayList<>();
        
        // Set up the main panel with a split layout
        mainPanel = new JPanel(new BorderLayout());
        
        // Create the board panel (left side)
        boardPanel = new JPanel(new BorderLayout());
        boardPanel.setBorder(BorderFactory.createTitledBorder("Kanban Board"));
        
        // Create the chat panel (right side)
        chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        
        JPanel chatInputPanel = new JPanel(new BorderLayout());
        chatField = new JTextField();
        JButton sendButton = new JButton("Send");
        
        chatInputPanel.add(chatField, BorderLayout.CENTER);
        chatInputPanel.add(sendButton, BorderLayout.EAST);
        
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);
        
        // Add action listener for sending chat messages
        ActionListener sendChatAction = e -> {
            String message = chatField.getText().trim();
            if (!message.isEmpty()) {
                sendMessage("CHAT_MESSAGE|" + message);
                chatField.setText("");
            }
        };
        
        chatField.addActionListener(sendChatAction);
        sendButton.addActionListener(sendChatAction);
        
        // Create a split pane to divide the board and chat
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardPanel, chatPanel);
        splitPane.setResizeWeight(0.7); // Board gets 70% of the space
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        // Add a toolbar at the top
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        
        JButton addTaskButton = new JButton("Add Task");
        addTaskButton.addActionListener(e -> showAddTaskDialog());
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> sendMessage("REFRESH"));
        
        JButton clearCompletedButton = new JButton("Clear Completed Tasks");
        clearCompletedButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to clear all completed tasks from the 'Done' and 'Completed' columns?",
                "Clear Completed Tasks",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            
            if (result == JOptionPane.YES_OPTION) {
                sendMessage("CLEAR_COMPLETED");
            }
        });
        
        toolbar.add(addTaskButton);
        toolbar.add(refreshButton);
        toolbar.add(clearCompletedButton);
        
        mainPanel.add(toolbar, BorderLayout.NORTH);
        
        // Add the main panel to the frame
        add(mainPanel);
        
        // Set up window closing event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
        
        // Show the login dialog and connect to the server
        showLoginDialog();
    }
    
    /**
     * Show a dialog to get the username and connect to the server
     */
    private void showLoginDialog() {
        JTextField usernameField = new JTextField(15);
        
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Enter your username:"));
        panel.add(usernameField);
        
        int result = JOptionPane.showConfirmDialog(
            this, panel, "Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            username = usernameField.getText().trim();
            
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                showLoginDialog();
                return;
            }
            
            // Connect to the server
            if (connectToServer()) {
                setVisible(true);
            } else {
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }
    
    /**
     * Connect to the Kanban board server
     */
    private boolean connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Send the username to the server
            sendMessage("LOGIN|" + username);
            
            // Start a thread to listen for server messages
            new Thread(this::listenForServerMessages).start();
            
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                this,
                "Could not connect to the server: " + e.getMessage(),
                "Connection Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }
    
    /**
     * Disconnect from the server
     */
    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Send a message to the server
     */
    private void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
    
    /**
     * Listen for messages from the server
     */
    private void listenForServerMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                final String finalMessage = message;
                SwingUtilities.invokeLater(() -> processServerMessage(finalMessage));
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Connection to server lost: " + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE
                );
                disconnect();
            }
        }
    }
    
    /**
     * Process a message from the server
     */
    private void processServerMessage(String message) {
        System.out.println("Received message from server: " + message);
        String[] parts = message.split("\\|");
        String command = parts[0];
        
        switch (command) {
            case "BOARD_UPDATE":
                String boardData = "";
                if (parts.length > 1) {
                    boardData = parts[1];
                    // If there are more parts, they are part of the board data (split by |)
                    for (int i = 2; i < parts.length; i++) {
                        boardData += "|" + parts[i];
                    }
                }
                updateBoard(boardData);
                break;
                
            case "USER_JOINED":
                chatArea.append(parts[1] + " has joined the board.\n");
                break;
                
            case "USER_LEFT":
                chatArea.append(parts[1] + " has left the board.\n");
                break;
                
            case "CHAT":
                chatArea.append(parts[1] + ": " + parts[2] + "\n");
                break;
        }
    }
    
    /**
     * Update the board with the data received from the server
     */
    private void updateBoard(String boardData) {
        // Clear existing data
        columns.clear();
        columnPanels.clear();
        taskCards.clear();
        
        System.out.println("Received board data: " + boardData);
        
        // Parse the board data
        if (!boardData.isEmpty()) {
            String[] columnStrings = boardData.split("\\|");
            System.out.println("Number of columns in data: " + columnStrings.length);
            
            for (String columnString : columnStrings) {
                String[] columnParts = columnString.split(":");
                String columnName = columnParts[0];
                System.out.println("Parsing column: " + columnName);
                
                ColumnData column = new ColumnData(columnName);
                columns.add(column);
                
                // Parse tasks if there are any
                if (columnParts.length > 1 && !columnParts[1].isEmpty()) {
                    String[] taskStrings = columnParts[1].split(";");
                    
                    for (String taskString : taskStrings) {
                        String[] taskParts = taskString.split(",");
                        
                        if (taskParts.length >= 5) {
                            TaskData task = new TaskData(
                                taskParts[0], // id
                                taskParts[1], // title
                                taskParts[2], // description
                                taskParts[3], // assignee
                                taskParts[4]  // dueDate
                            );
                            column.addTask(task);
                        }
                    }
                }
            }
        }
        
        System.out.println("Parsed columns:");
        for (ColumnData column : columns) {
            System.out.println("- " + column.getName() + " (tasks: " + column.getTasks().size() + ")");
        }
        
        // Rebuild the board UI
        rebuildBoardUI();
    }
    
    /**
     * Rebuild the board UI based on the current data
     */
    private void rebuildBoardUI() {
        // Remove all components from the board panel
        boardPanel.removeAll();
        
        // Create a main panel with vertical layout to hold active and completed sections
        JPanel mainBoardPanel = new JPanel(new BorderLayout(0, 10));
        mainBoardPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Create a panel to hold the active columns
        List<ColumnData> activeColumns = new ArrayList<>();
        ColumnData completedColumn = null;
        
        // Separate active columns from completed columns
        List<ColumnData> completedColumns = new ArrayList<>();
        for (ColumnData column : columns) {
            if (column.getName().equals("Done") || column.getName().equals("Completed")) {
                completedColumns.add(column);
            } else {
                activeColumns.add(column);
            }
        }
        
        // Create active columns panel
        JPanel activeColumnsPanel = new JPanel(new GridLayout(1, activeColumns.size(), 10, 0));
        activeColumnsPanel.setBorder(BorderFactory.createTitledBorder("Active Tasks"));
        
        // Create each active column
        for (ColumnData column : activeColumns) {
            JPanel columnPanel = createColumnPanel(column);
            columnPanels.put(column.getName(), columnPanel);
            activeColumnsPanel.add(columnPanel);
            
            // Create a map for this column's task cards
            taskCards.put(column.getName(), new HashMap<>());
            
            // Add tasks to the column
            for (TaskData task : column.getTasks()) {
                TaskCard taskCard = new TaskCard(task, column.getName());
                columnPanel.add(taskCard);
                taskCards.get(column.getName()).put(task.getId(), taskCard);
            }
        }
        
        // Add the active columns panel to the main board panel
        mainBoardPanel.add(activeColumnsPanel, BorderLayout.CENTER);
        
        // Create completed tasks panel if there are completed columns
        if (!completedColumns.isEmpty()) {
            JPanel completedTasksPanel = new JPanel(new BorderLayout());
            completedTasksPanel.setBorder(BorderFactory.createTitledBorder("Completed Tasks"));
            
            // Create a panel to hold all completed columns side by side
            JPanel completedColumnsPanel = new JPanel(new GridLayout(1, completedColumns.size(), 10, 0));
            
            // Create each completed column
            for (ColumnData column : completedColumns) {
                JPanel columnPanel = createColumnPanel(column);
                columnPanels.put(column.getName(), columnPanel);
                completedColumnsPanel.add(columnPanel);
                
                // Create a map for this column's task cards
                taskCards.put(column.getName(), new HashMap<>());
                
                // Add tasks to the column
                for (TaskData task : column.getTasks()) {
                    TaskCard taskCard = new TaskCard(task, column.getName());
                    columnPanel.add(taskCard);
                    taskCards.get(column.getName()).put(task.getId(), taskCard);
                }
            }
            
            // Add the completed columns panel to a scroll pane
            JScrollPane completedScrollPane = new JScrollPane(completedColumnsPanel);
            completedScrollPane.setPreferredSize(new Dimension(0, 150));
            completedTasksPanel.add(completedScrollPane, BorderLayout.CENTER);
            
            // Add the completed tasks panel to the main board panel
            mainBoardPanel.add(completedTasksPanel, BorderLayout.SOUTH);
        }
        
        // Add the main board panel to a scroll pane
        JScrollPane scrollPane = new JScrollPane(mainBoardPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        boardPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Refresh the UI
        boardPanel.revalidate();
        boardPanel.repaint();
    }
    
    /**
     * Create a panel for a column
     */
    private JPanel createColumnPanel(ColumnData column) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(column.getName()));
        panel.setBackground(Color.LIGHT_GRAY);
        
        // Make the panel a drop target for tasks
        panel.setTransferHandler(new TaskTransferHandler(column.getName()));
        
        return panel;
    }
    
    /**
     * Show a dialog to add a new task
     */
    private void showAddTaskDialog() {
        // Create a more compact and modern dialog
        JDialog dialog = new JDialog(this, "New Task", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
        
        // Create a form panel with a more compact layout
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Column selection (more compact)
        JPanel columnPanel = new JPanel(new BorderLayout());
        columnPanel.add(new JLabel("Column:"), BorderLayout.WEST);
        JComboBox<String> columnComboBox = new JComboBox<>();
        
        // Make sure all columns are added to the dropdown
        System.out.println("Available columns for new task:");
        for (ColumnData column : columns) {
            System.out.println("- " + column.getName());
            columnComboBox.addItem(column.getName());
        }
        
        columnPanel.add(columnComboBox, BorderLayout.CENTER);
        
        // Title field
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(new JLabel("Title:"), BorderLayout.WEST);
        JTextField titleField = new JTextField(15);
        titlePanel.add(titleField, BorderLayout.CENTER);
        
        // Description field (smaller)
        JPanel descPanel = new JPanel(new BorderLayout());
        descPanel.add(new JLabel("Description:"), BorderLayout.NORTH);
        JTextArea descriptionArea = new JTextArea(3, 15);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        descPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Assignee field
        JPanel assigneePanel = new JPanel(new BorderLayout());
        assigneePanel.add(new JLabel("Assignee:"), BorderLayout.WEST);
        JTextField assigneeField = new JTextField(15);
        assigneePanel.add(assigneeField, BorderLayout.CENTER);
        
        // Due date field
        JPanel datePanel = new JPanel(new BorderLayout());
        datePanel.add(new JLabel("Due Date:"), BorderLayout.WEST);
        JTextField dueDateField = new JTextField(15);
        dueDateField.setToolTipText("YYYY-MM-DD");
        datePanel.add(dueDateField, BorderLayout.CENTER);
        
        // Add components to the form with spacing
        formPanel.add(columnPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(titlePanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(descPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(assigneePanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(datePanel);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton okButton = new JButton("Add Task");
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        // Result variable needs to be effectively final for the lambda
        final boolean[] taskAdded = {false};
        
        okButton.addActionListener(e -> {
            String columnName = (String) columnComboBox.getSelectedItem();
            String title = titleField.getText().trim();
            String description = descriptionArea.getText().trim();
            String assignee = assigneeField.getText().trim();
            String dueDate = dueDateField.getText().trim();
            
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Title cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Generate a unique ID for the task
            String taskId = UUID.randomUUID().toString();
            
            // Send the add task message to the server
            sendMessage("ADD_TASK|" + columnName + "|" + taskId + "|" + title + "|" + description + "|" + assignee + "|" + dueDate);
            
            taskAdded[0] = true;
            dialog.dispose();
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Set default button and show dialog
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.setVisible(true);
    }
    
    /**
     * Show a dialog to edit an existing task
     */
    private void showEditTaskDialog(TaskData task, String columnName) {
        // Create a more compact and modern dialog
        JDialog dialog = new JDialog(this, "Edit Task", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 400);
        dialog.setLocationRelativeTo(this);
        
        // Create a form panel with a more compact layout
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Column selection dropdown
        JPanel columnPanel = new JPanel(new BorderLayout());
        columnPanel.add(new JLabel("Column:"), BorderLayout.WEST);
        JComboBox<String> columnComboBox = new JComboBox<>();
        
        // Add all columns to the dropdown
        System.out.println("Available columns for edit task:");
        for (ColumnData column : columns) {
            System.out.println("- " + column.getName());
            columnComboBox.addItem(column.getName());
        }
        
        // Set the current column as selected
        columnComboBox.setSelectedItem(columnName);
        columnPanel.add(columnComboBox, BorderLayout.CENTER);
        
        // Title field
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(new JLabel("Title:"), BorderLayout.WEST);
        JTextField titleField = new JTextField(task.getTitle(), 15);
        titlePanel.add(titleField, BorderLayout.CENTER);
        
        // Description field (smaller)
        JPanel descPanel = new JPanel(new BorderLayout());
        descPanel.add(new JLabel("Description:"), BorderLayout.NORTH);
        JTextArea descriptionArea = new JTextArea(task.getDescription(), 3, 15);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        descPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Assignee field
        JPanel assigneePanel = new JPanel(new BorderLayout());
        assigneePanel.add(new JLabel("Assignee:"), BorderLayout.WEST);
        JTextField assigneeField = new JTextField(task.getAssignee(), 15);
        assigneePanel.add(assigneeField, BorderLayout.CENTER);
        
        // Due date field
        JPanel datePanel = new JPanel(new BorderLayout());
        datePanel.add(new JLabel("Due Date:"), BorderLayout.WEST);
        JTextField dueDateField = new JTextField(task.getDueDate(), 15);
        dueDateField.setToolTipText("YYYY-MM-DD");
        datePanel.add(dueDateField, BorderLayout.CENTER);
        
        // Add components to the form with spacing
        formPanel.add(columnPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(titlePanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(descPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(assigneePanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(datePanel);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        JButton saveButton = new JButton("Save Changes");
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        // Result variable needs to be effectively final for the lambda
        final boolean[] taskUpdated = {false};
        
        saveButton.addActionListener(e -> {
            String newColumnName = (String) columnComboBox.getSelectedItem();
            String title = titleField.getText().trim();
            String description = descriptionArea.getText().trim();
            String assignee = assigneeField.getText().trim();
            String dueDate = dueDateField.getText().trim();
            
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Title cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // If column has changed, move the task
            if (!newColumnName.equals(columnName)) {
                sendMessage("MOVE_TASK|" + task.getId() + "|" + columnName + "|" + newColumnName);
            }
            
            // Send the update task message to the server
            sendMessage("UPDATE_TASK|" + newColumnName + "|" + task.getId() + "|" + title + "|" + description + "|" + assignee + "|" + dueDate);
            
            taskUpdated[0] = true;
            dialog.dispose();
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Set default button and show dialog
        dialog.getRootPane().setDefaultButton(saveButton);
        dialog.setVisible(true);
    }
    
    /**
     * Show a confirmation dialog to delete a task
     */
    private void showDeleteTaskDialog(TaskData task, String columnName) {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete the task \"" + task.getTitle() + "\"?",
            "Delete Task",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            // Send the delete task message to the server
            sendMessage("DELETE_TASK|" + columnName + "|" + task.getId());
        }
    }
    
    /**
     * Task card component for displaying a task
     */
    private class TaskCard extends JPanel {
        private TaskData task;
        private String columnName;
        
        public TaskCard(TaskData task, String columnName) {
            this.task = task;
            this.columnName = columnName;
            
            // Set fixed size for all task cards
            setPreferredSize(new Dimension(200, 100));
            setMinimumSize(new Dimension(200, 100));
            setMaximumSize(new Dimension(200, 100));
            
            setLayout(new BorderLayout(5, 5));
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createLineBorder(Color.BLACK)
            ));
            setBackground(Color.WHITE);
            setCursor(new Cursor(Cursor.HAND_CURSOR)); // Change cursor to indicate clickable
            
            // Title at the top
            JLabel titleLabel = new JLabel(task.getTitle());
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
            add(titleLabel, BorderLayout.NORTH);
            
            // Assignee at the bottom
            JLabel assigneeLabel = new JLabel("Assignee: " + task.getAssignee());
            add(assigneeLabel, BorderLayout.SOUTH);
            
            // Add context menu
            JPopupMenu contextMenu = new JPopupMenu();
            
            JMenuItem viewItem = new JMenuItem("View Details");
            viewItem.addActionListener(e -> showTaskDetailsDialog(task));
            
            JMenuItem editItem = new JMenuItem("Edit");
            editItem.addActionListener(e -> showEditTaskDialog(task, columnName));
            
            JMenuItem deleteItem = new JMenuItem("Delete");
            deleteItem.addActionListener(e -> showDeleteTaskDialog(task, columnName));
            
            contextMenu.add(viewItem);
            contextMenu.add(editItem);
            contextMenu.add(deleteItem);
            
            // Make the task card draggable
            setTransferHandler(new TaskTransferHandler(task.getId()));
            
            // Combined mouse listener for all interactions
            addMouseListener(new MouseAdapter() {
                private boolean isDragging = false;
                private Point dragStart = null;
                
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showContextMenu(e);
                        return;
                    }
                    
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        // Store the starting point for potential drag
                        dragStart = e.getPoint();
                    }
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showContextMenu(e);
                        return;
                    }
                    
                    // If we didn't drag much, treat it as a click
                    if (e.getButton() == MouseEvent.BUTTON1 && !isDragging) {
                        // Left click shows task details
                        showTaskDetailsDialog(task);
                    }
                    
                    // Reset drag state
                    isDragging = false;
                    dragStart = null;
                }
                
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragStart != null) {
                        // If we've moved more than a few pixels, consider it a drag
                        int dx = Math.abs(e.getX() - dragStart.x);
                        int dy = Math.abs(e.getY() - dragStart.y);
                        
                        if (dx > 5 || dy > 5) {
                            isDragging = true;
                            JComponent comp = (JComponent) e.getSource();
                            TransferHandler handler = comp.getTransferHandler();
                            handler.exportAsDrag(comp, e, TransferHandler.MOVE);
                        }
                    }
                }
                
                private void showContextMenu(MouseEvent e) {
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            });
            
            // Also need to add mouse motion listener for drag detection
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    // This is needed to properly detect drags
                    // The actual logic is in the mouseListener above
                }
            });
        }
    }
    
    /**
     * Show a dialog with the full details of a task
     */
    private void showTaskDetailsDialog(TaskData task) {
        JDialog dialog = new JDialog(this, "Task Details", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
        
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Title
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Title:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        JLabel titleValue = new JLabel(task.getTitle());
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(titleValue, BorderLayout.CENTER);
        
        // Description
        JPanel descPanel = new JPanel(new BorderLayout());
        JLabel descLabel = new JLabel("Description:");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.BOLD));
        JTextArea descValue = new JTextArea(task.getDescription());
        descValue.setEditable(false);
        descValue.setLineWrap(true);
        descValue.setWrapStyleWord(true);
        descValue.setBackground(UIManager.getColor("Panel.background"));
        JScrollPane scrollPane = new JScrollPane(descValue);
        scrollPane.setPreferredSize(new Dimension(350, 150));
        descPanel.add(descLabel, BorderLayout.NORTH);
        descPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Assignee
        JPanel assigneePanel = new JPanel(new BorderLayout());
        JLabel assigneeLabel = new JLabel("Assignee:");
        assigneeLabel.setFont(assigneeLabel.getFont().deriveFont(Font.BOLD));
        JLabel assigneeValue = new JLabel(task.getAssignee());
        assigneePanel.add(assigneeLabel, BorderLayout.NORTH);
        assigneePanel.add(assigneeValue, BorderLayout.CENTER);
        
        // Due Date
        JPanel dueDatePanel = new JPanel(new BorderLayout());
        JLabel dueDateLabel = new JLabel("Due Date:");
        dueDateLabel.setFont(dueDateLabel.getFont().deriveFont(Font.BOLD));
        JLabel dueDateValue = new JLabel(task.getDueDate());
        dueDatePanel.add(dueDateLabel, BorderLayout.NORTH);
        dueDatePanel.add(dueDateValue, BorderLayout.CENTER);
        
        // Add components to panel with spacing
        detailsPanel.add(titlePanel);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        detailsPanel.add(descPanel);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        detailsPanel.add(assigneePanel);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        detailsPanel.add(dueDatePanel);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton editButton = new JButton("Edit");
        JButton closeButton = new JButton("Close");
        
        editButton.addActionListener(e -> {
            dialog.dispose();
            showEditTaskDialog(task, getColumnNameForTask(task.getId()));
        });
        
        closeButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(editButton);
        buttonPanel.add(closeButton);
        
        dialog.add(detailsPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    /**
     * Find the column name for a task by its ID
     */
    private String getColumnNameForTask(String taskId) {
        for (Map.Entry<String, Map<String, TaskCard>> entry : taskCards.entrySet()) {
            if (entry.getValue().containsKey(taskId)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Transfer handler for drag and drop of tasks
     */
    private class TaskTransferHandler extends TransferHandler {
        private final String id;
        
        public TaskTransferHandler(String id) {
            this.id = id;
        }
        
        @Override
        protected Transferable createTransferable(JComponent c) {
            return new StringSelection(id);
        }
        
        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }
        
        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }
        
        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            
            try {
                String taskId = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                
                // Find the source column
                String sourceColumn = null;
                for (Map.Entry<String, Map<String, TaskCard>> entry : taskCards.entrySet()) {
                    if (entry.getValue().containsKey(taskId)) {
                        sourceColumn = entry.getKey();
                        break;
                    }
                }
                
                // If this is a column ID, it's a drop target
                if (columns.stream().anyMatch(col -> col.getName().equals(id))) {
                    // Move the task to this column
                    if (sourceColumn != null && !sourceColumn.equals(id)) {
                        sendMessage("MOVE_TASK|" + taskId + "|" + sourceColumn + "|" + id);
                        return true;
                    }
                }
                
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
    
    /**
     * Data class for a Kanban column
     */
    private static class ColumnData {
        private String name;
        private List<TaskData> tasks;
        
        public ColumnData(String name) {
            this.name = name;
            this.tasks = new ArrayList<>();
        }
        
        public String getName() {
            return name;
        }
        
        public List<TaskData> getTasks() {
            return tasks;
        }
        
        public void addTask(TaskData task) {
            tasks.add(task);
        }
    }
    
    /**
     * Data class for a task
     */
    private static class TaskData {
        private String id;
        private String title;
        private String description;
        private String assignee;
        private String dueDate;
        
        public TaskData(String id, String title, String description, String assignee, String dueDate) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.assignee = assignee;
            this.dueDate = dueDate;
        }
        
        public String getId() {
            return id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getAssignee() {
            return assignee;
        }
        
        public String getDueDate() {
            return dueDate;
        }
    }
    
    /**
     * Main method to start the application
     */
    public static void main(String[] args) {
        // Set the look and feel to the system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create and show the application
        SwingUtilities.invokeLater(KanbanClient::new);
    }
}
