package net.penguincoders.doit.models;

public class ToDoModel {
    private int id, status;
    private String task, extra;

    public ToDoModel() {
    }

    public ToDoModel(int status, String task, String extra) {
        this.status = status;
        this.task = task;
        this.extra = extra;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
