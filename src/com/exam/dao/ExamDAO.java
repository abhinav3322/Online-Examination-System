package com.exam.dao;

import com.exam.model.Exam;
import com.exam.model.Question;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class ExamDAO {

    public int createExam(String title, int durationMinutes, List<Integer> questionIds) throws SQLException {
        // LinkedHashSet preserves selection order and removes duplicate question IDs
        Set<Integer> uniqueIds = new LinkedHashSet<>(questionIds);
        Connection conn = DBConnection.getConnection();
        int examId = -1;

        String insertExamSql = "INSERT INTO exams (title, duration_minutes) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertExamSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setInt(2, durationMinutes);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    examId = keys.getInt(1);
                }
            }
        }

        if (examId > 0 && !uniqueIds.isEmpty()) {
            String linkSql = "INSERT INTO exam_questions (exam_id, question_id) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(linkSql)) {
                for (Integer qId : uniqueIds) {
                    ps.setInt(1, examId);
                    ps.setInt(2, qId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
        return examId;
    }

    public List<Exam> getAllExams() throws SQLException {
        List<Exam> exams = new ArrayList<>();
        String sql = "SELECT id, title, duration_minutes FROM exams ORDER BY id DESC";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Exam e = new Exam(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getInt("duration_minutes")
                );
                e.setQuestions(getQuestionsForExam(e.getId()));
                exams.add(e);
            }
        }
        return exams;
    }

    public Exam getExamById(int examId) throws SQLException {
        String sql = "SELECT id, title, duration_minutes FROM exams WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Exam e = new Exam(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getInt("duration_minutes")
                    );
                    e.setQuestions(getQuestionsForExam(examId));
                    return e;
                }
            }
        }
        return null;
    }

    public List<Question> getQuestionsForExam(int examId) throws SQLException {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT q.id, q.subject, q.prompt, q.opt_a, q.opt_b, q.opt_c, q.opt_d, q.correct_opt " +
                     "FROM questions q INNER JOIN exam_questions eq ON q.id = eq.question_id " +
                     "WHERE eq.exam_id = ? ORDER BY q.id ASC";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(QuestionDAO.mapRow(rs));
                }
            }
        }
        return list;
    }
}
