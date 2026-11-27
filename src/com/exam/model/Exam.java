package com.exam.model;

import java.util.ArrayList;
import java.util.List;


public class Exam {
    private int id;
    private String title;
    private int durationMinutes;
    private List<Question> questions;

    public Exam() {
        this.questions = new ArrayList<>();
    }

    public Exam(int id, String title, int durationMinutes) {
        this.id = id;
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.questions = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }
}
