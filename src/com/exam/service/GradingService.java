package com.exam.service;

import com.exam.dao.AttemptDAO;
import com.exam.dao.ExamDAO;
import com.exam.model.Attempt;
import com.exam.model.Exam;
import com.exam.model.Question;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class GradingService {
    private final ExamDAO examDAO = new ExamDAO();
    private final AttemptDAO attemptDAO = new AttemptDAO();

    public static class QuestionResult {
        public final int questionId;
        public final String prompt;
        public final String selectedOption;
        public final String correctOption;
        public final boolean correct;

        public QuestionResult(int questionId, String prompt, String selectedOption, String correctOption, boolean correct) {
            this.questionId = questionId;
            this.prompt = prompt;
            this.selectedOption = selectedOption;
            this.correctOption = correctOption;
            this.correct = correct;
        }
    }

    public static class EvaluationResult {
        public final Attempt attempt;
        public final List<QuestionResult> breakdown;

        public EvaluationResult(Attempt attempt, List<QuestionResult> breakdown) {
            this.attempt = attempt;
            this.breakdown = breakdown;
        }
    }

    public EvaluationResult gradeAndSave(String studentName, String studentRoll, int examId,
                                         Map<Integer, String> answers, int timeTakenSec) throws SQLException {
        Exam exam = examDAO.getExamById(examId);
        if (exam == null) {
            throw new IllegalArgumentException("Exam not found: " + examId);
        }

        List<Question> questions = exam.getQuestions();
        int score = 0;
        List<QuestionResult> breakdown = new ArrayList<>();

        for (Question q : questions) {
            String selected = answers.getOrDefault(q.getId(), "").trim().toUpperCase();
            boolean isCorrect = q.getCorrectOption().equals(selected);
            if (isCorrect) {
                score++;
            }
            breakdown.add(new QuestionResult(q.getId(), q.getPrompt(), selected, q.getCorrectOption(), isCorrect));
        }

        Attempt attempt = new Attempt(studentName, studentRoll, examId, score, questions.size(), Math.max(1, timeTakenSec));
        attempt.setExamTitle(exam.getTitle());
        int id = attemptDAO.saveAttempt(attempt);
        attempt.setId(id);

        return new EvaluationResult(attempt, breakdown);
    }

  
    public List<Attempt> getHistory(String rollFilter) throws SQLException {
        List<Attempt> all = attemptDAO.getAllAttempts();
        if (rollFilter == null || rollFilter.trim().isEmpty()) {
            return all;
        }
        String needle = rollFilter.trim().toLowerCase();
        return all.stream()
                .filter(a -> a.getStudentRoll() != null && a.getStudentRoll().toLowerCase().contains(needle))
                .collect(Collectors.toList());
    }

    public List<Attempt> getToppers(int examId, int limit) throws SQLException {
        return attemptDAO.getAllAttempts().stream()
                .filter(a -> examId <= 0 || a.getExamId() == examId)
                .sorted(Comparator.comparingInt(Attempt::getScore).reversed()
                        .thenComparingInt(Attempt::getTimeTakenSec))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
