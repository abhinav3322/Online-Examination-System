package com.exam;

import com.exam.dao.ExamDAO;
import com.exam.dao.QuestionDAO;
import com.exam.model.Question;
import com.exam.web.ExamHttpServer;

import java.util.ArrayList;
import java.util.List;


public class Main {
    public static void main(String[] args) {
        try {
            seedSampleDataIfEmpty();
            ExamHttpServer server = new ExamHttpServer(8080);
            server.start();
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedSampleDataIfEmpty() throws Exception {
        QuestionDAO qDao = new QuestionDAO();
        if (!qDao.getAllQuestions().isEmpty()) {
            return;
        }
        List<Integer> ids = new ArrayList<>();
        ids.add(qDao.addQuestion(new Question(0, "Core Java", "Which collection maintains insertion order and disallows duplicates?",
                new String[]{"ArrayList", "HashSet", "LinkedHashSet", "PriorityQueue"}, "C")));
        ids.add(qDao.addQuestion(new Question(0, "JDBC", "Which interface prevents SQL injection via precompiled parameter binding?",
                new String[]{"Statement", "PreparedStatement", "ResultSet", "DriverManager"}, "B")));
        ids.add(qDao.addQuestion(new Question(0, "Multithreading", "Which state does a thread enter when Thread.sleep(1000) is called?",
                new String[]{"NEW", "BLOCKED", "TIMED_WAITING", "TERMINATED"}, "C")));
        ids.add(qDao.addQuestion(new Question(0, "Streams", "Which Stream operation is used to sort elements in a pipeline?",
                new String[]{"filter()", "map()", "sorted()", "distinct()"}, "C")));
        ids.add(qDao.addQuestion(new Question(0, "OOP", "Which keyword refers to the current class instance?",
                new String[]{"super", "this", "static", "final"}, "B")));

        ExamDAO eDao = new ExamDAO();
        eDao.createExam("2nd Year Java Mid-Term Quiz", 5, ids);
        System.out.println("Seeded 5 sample Java MCQs and 1 default exam.");
    }
}
