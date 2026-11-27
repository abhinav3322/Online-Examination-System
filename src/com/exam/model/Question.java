package com.exam.model;


public class Question {
    private int id;
    private String subject;
    private String prompt;
    private String[] options; // [optA, optB, optC, optD]
    private String correctOption; // "A", "B", "C", or "D"

    public Question() {
        this.options = new String[4];
    }

    public Question(int id, String subject, String prompt, String[] options, String correctOption) {
        this.id = id;
        this.subject = subject;
        this.prompt = prompt;
        this.options = options;
        this.correctOption = correctOption != null ? correctOption.trim().toUpperCase() : "A";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String[] getOptions() { return options; }
    public void setOptions(String[] options) { this.options = options; }

    public String getCorrectOption() { return correctOption; }
    public void setCorrectOption(String correctOption) {
        this.correctOption = correctOption != null ? correctOption.trim().toUpperCase() : "A";
    }
}
