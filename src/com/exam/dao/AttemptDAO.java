package com.exam.dao;

import com.exam.model.Attempt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class AttemptDAO {

    public int saveAttempt(Attempt a) throws SQLException {
        String sql = "INSERT INTO attempts (student_name, student_roll, exam_id, score, total, time_taken_sec) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getStudentName());
            ps.setString(2, a.getStudentRoll());
            ps.setInt(3, a.getExamId());
            ps.setInt(4, a.getScore());
            ps.setInt(5, a.getTotal());
            ps.setInt(6, a.getTimeTakenSec());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public List<Attempt> getAllAttempts() throws SQLException {
        List<Attempt> list = new ArrayList<>();
        String sql = "SELECT a.id, a.student_name, a.student_roll, a.exam_id, e.title AS exam_title, " +
                     "a.score, a.total, a.time_taken_sec, a.submitted_at " +
                     "FROM attempts a INNER JOIN exams e ON a.exam_id = e.id " +
                     "ORDER BY a.id DESC";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Attempt a = new Attempt();
                a.setId(rs.getInt("id"));
                a.setStudentName(rs.getString("student_name"));
                a.setStudentRoll(rs.getString("student_roll"));
                a.setExamId(rs.getInt("exam_id"));
                a.setExamTitle(rs.getString("exam_title"));
                a.setScore(rs.getInt("score"));
                a.setTotal(rs.getInt("total"));
                a.setTimeTakenSec(rs.getInt("time_taken_sec"));
                a.setSubmittedAt(rs.getString("submitted_at"));
                list.add(a);
            }
        }
        return list;
    }
}
