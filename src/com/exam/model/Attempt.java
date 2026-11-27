package com.exam.model;


public class Attempt implements Comparable<Attempt> {
    private int id;
    private String studentName;
    private String studentRoll;
    private int examId;
    private String examTitle;
    private int score;
    private int total;
    private int timeTakenSec;
    private String submittedAt;

    public Attempt() {}

    public Attempt(String studentName, String studentRoll, int examId, int score, int total, int timeTakenSec) {
        this.studentName = studentName;
        this.studentRoll = studentRoll;
        this.examId = examId;
        this.score = score;
        this.total = total;
        this.timeTakenSec = timeTakenSec;
    }

    /**
     * Natural ordering: higher score first; ties broken by faster completion time.
     */
    @Override
    public int compareTo(Attempt other) {
        int scoreCmp = Integer.compare(other.score, this.score);
        if (scoreCmp != 0) {
            return scoreCmp;
        }
        return Integer.compare(this.timeTakenSec, other.timeTakenSec);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentRoll() { return studentRoll; }
    public void setStudentRoll(String studentRoll) { this.studentRoll = studentRoll; }

    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }

    public String getExamTitle() { return examTitle; }
    public void setExamTitle(String examTitle) { this.examTitle = examTitle; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getTimeTakenSec() { return timeTakenSec; }
    public void setTimeTakenSec(int timeTakenSec) { this.timeTakenSec = timeTakenSec; }

    public String getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }
}
