package main.java.server;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a task in the Kanban board
 */
public class Task implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String title;
    private String description;
    private String assignee;
    private String dueDate;
    
    public Task(String title, String description, String assignee, String dueDate) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.assignee = assignee;
        this.dueDate = dueDate;
    }
    
    public Task(String id, String title, String description, String assignee, String dueDate) {
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
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getAssignee() {
        return assignee;
    }
    
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
    
    public String getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", assignee='" + assignee + '\'' +
                ", dueDate='" + dueDate + '\'' +
                '}';
    }
}
