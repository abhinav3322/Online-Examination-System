package com.exam.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public class DBConnection {
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/online_exam_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "#"; // Set your MySQL password here

    private static Connection connection;

    private DBConnection() {}

    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            initSchema(connection);
        }
        return connection;
    }

    private static void initSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS questions (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "subject VARCHAR(100) NOT NULL," +
                "prompt TEXT NOT NULL," +
                "opt_a VARCHAR(255) NOT NULL," +
                "opt_b VARCHAR(255) NOT NULL," +
                "opt_c VARCHAR(255) NOT NULL," +
                "opt_d VARCHAR(255) NOT NULL," +
                "correct_opt CHAR(1) NOT NULL)"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS exams (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "title VARCHAR(200) NOT NULL," +
                "duration_minutes INT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS exam_questions (" +
                "exam_id INT NOT NULL," +
                "question_id INT NOT NULL," +
                "PRIMARY KEY (exam_id, question_id)," +
                "FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE," +
                "FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE)"
            );
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS attempts (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "student_name VARCHAR(150) NOT NULL," +
                "student_roll VARCHAR(50) NOT NULL," +
                "exam_id INT NOT NULL," +
                "score INT NOT NULL," +
                "total INT NOT NULL," +
                "time_taken_sec INT NOT NULL," +
                "submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE)"
            );
        }
    }
}
