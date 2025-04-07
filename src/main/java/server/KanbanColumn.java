package main.java.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a column in the Kanban board (e.g., "To Do", "In Progress", "Done")
 */
public class KanbanColumn implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private List<Task> tasks;
    
    public KanbanColumn(String name) {
        this.name = name;
        this.tasks = new ArrayList<>();
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<Task> getTasks() {
        return tasks;
    }
    
    public void addTask(Task task) {
        tasks.add(task);
    }
    
    public void removeTask(Task task) {
        tasks.remove(task);
    }
    
    @Override
    public String toString() {
        return "KanbanColumn{" +
                "name='" + name + '\'' +
                ", tasks=" + tasks +
                '}';
    }
}
