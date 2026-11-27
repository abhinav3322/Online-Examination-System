package com.exam.dao;

import com.exam.model.Question;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class QuestionDAO {

    public int addQuestion(Question q) throws SQLException {
        String sql = "INSERT INTO questions (subject, prompt, opt_a, opt_b, opt_c, opt_d, correct_opt) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, q.getSubject());
            ps.setString(2, q.getPrompt());
            ps.setString(3, q.getOptions()[0]);
            ps.setString(4, q.getOptions()[1]);
            ps.setString(5, q.getOptions()[2]);
            ps.setString(6, q.getOptions()[3]);
            ps.setString(7, q.getCorrectOption());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public List<Question> getAllQuestions() throws SQLException {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT id, subject, prompt, opt_a, opt_b, opt_c, opt_d, correct_opt FROM questions ORDER BY id DESC";
        Connection conn = DBConnection.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public static Question mapRow(ResultSet rs) throws SQLException {
        String[] opts = new String[] {
            rs.getString("opt_a"),
            rs.getString("opt_b"),
            rs.getString("opt_c"),
            rs.getString("opt_d")
        };
        return new Question(
            rs.getInt("id"),
            rs.getString("subject"),
            rs.getString("prompt"),
            opts,
            rs.getString("correct_opt")
        );
    }
}
