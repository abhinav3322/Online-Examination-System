package com.exam.web;

import com.exam.dao.ExamDAO;
import com.exam.dao.QuestionDAO;
import com.exam.exception.ExamExpiredException;
import com.exam.exception.ValidationException;
import com.exam.model.Attempt;
import com.exam.model.Exam;
import com.exam.model.Question;
import com.exam.service.CsvService;
import com.exam.service.GradingService;
import com.exam.thread.ExamTimerThread;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class ExamHttpServer {
    private final int port;
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final ExamDAO examDAO = new ExamDAO();
    private final GradingService gradingService = new GradingService();
    private final CsvService csvService = new CsvService();
    private final Map<String, ExamTimerThread> activeTimers = new ConcurrentHashMap<>();

    public ExamHttpServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleStatic);
        server.createContext("/api/questions", this::handleQuestions);
        server.createContext("/api/exams", this::handleExams);
        server.createContext("/api/attempt/start", this::handleStartAttempt);
        server.createContext("/api/attempt/submit", this::handleSubmitAttempt);
        server.createContext("/api/history", this::handleHistory);
        server.createContext("/api/toppers", this::handleToppers);
        server.createContext("/api/export", this::handleExportCsv);
        server.setExecutor(null);
        server.start();
        System.out.println("Online Examination System running at http://localhost:" + port);
    }

    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if ("/".equals(path)) {
            path = "/index.html";
        }
        Path file = Paths.get("frontend", path.substring(1)).normalize();
        if (!file.startsWith(Paths.get("frontend")) || !Files.exists(file) || Files.isDirectory(file)) {
            sendText(ex, 404, "text/plain", "Not Found");
            return;
        }
        String contentType = "text/plain";
        if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
        else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
        else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";

        byte[] bytes = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void handleQuestions(HttpExchange ex) throws IOException {
        try {
            if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
                List<Question> list = questionDAO.getAllQuestions();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(questionToJson(list.get(i), true));
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());
            } else if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                Map<String, String> body = parseFlatJson(readBody(ex));
                String subject = requireNonEmpty(body.get("subject"), "Subject is required");
                String prompt = requireNonEmpty(body.get("prompt"), "Question prompt is required");
                String a = requireNonEmpty(body.get("optA"), "Option A is required");
                String b = requireNonEmpty(body.get("optB"), "Option B is required");
                String c = requireNonEmpty(body.get("optC"), "Option C is required");
                String d = requireNonEmpty(body.get("optD"), "Option D is required");
                String correct = requireNonEmpty(body.get("correctOption"), "Correct option is required").toUpperCase();

                Question q = new Question(0, subject, prompt, new String[]{a, b, c, d}, correct);
                int id = questionDAO.addQuestion(q);
                sendJson(ex, 201, "{\"success\":true,\"id\":" + id + "}");
            } else {
                sendJson(ex, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
            }
        } catch (ValidationException ve) {
            sendJson(ex, 400, "{\"success\":false,\"message\":\"" + esc(ve.getMessage()) + "\"}");
        } catch (Exception e) {
            sendJson(ex, 500, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private void handleExams(HttpExchange ex) throws IOException {
        try {
            if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
                List<Exam> exams = examDAO.getAllExams();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < exams.size(); i++) {
                    Exam exam = exams.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{")
                      .append("\"id\":").append(exam.getId()).append(",")
                      .append("\"title\":\"").append(esc(exam.getTitle())).append("\",")
                      .append("\"durationMinutes\":").append(exam.getDurationMinutes()).append(",")
                      .append("\"questionCount\":").append(exam.getQuestions().size())
                      .append("}");
                }
                sb.append("]");
                sendJson(ex, 200, sb.toString());
            } else if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                String raw = readBody(ex);
                Map<String, String> body = parseFlatJson(raw);
                String title = requireNonEmpty(body.get("title"), "Exam title is required");
                int duration = parsePositiveInt(body.get("durationMinutes"), "Duration must be positive");
                List<Integer> qIds = parseIntList(body.get("questionIds"));
                if (qIds.isEmpty()) {
                    throw new ValidationException("Select at least one question");
                }
                int id = examDAO.createExam(title, duration, qIds);
                sendJson(ex, 201, "{\"success\":true,\"id\":" + id + "}");
            } else {
                sendJson(ex, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
            }
        } catch (ValidationException ve) {
            sendJson(ex, 400, "{\"success\":false,\"message\":\"" + esc(ve.getMessage()) + "\"}");
        } catch (Exception e) {
            sendJson(ex, 500, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private void handleStartAttempt(HttpExchange ex) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> body = parseFlatJson(readBody(ex));
            int examId = parsePositiveInt(body.get("examId"), "Valid examId required");
            Exam exam = examDAO.getExamById(examId);
            if (exam == null) {
                throw new ValidationException("Exam not found");
            }

            List<Question> questions = new ArrayList<>(exam.getQuestions());
            Collections.shuffle(questions); // Shuffle question order per student

            String sessionId = UUID.randomUUID().toString();
            ExamTimerThread timer = new ExamTimerThread(sessionId, exam.getDurationMinutes());
            activeTimers.put(sessionId, timer);
            Thread t = new Thread(timer, "ExamTimer-" + sessionId.substring(0, 6));
            t.start();

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"success\":true,")
              .append("\"sessionId\":\"").append(sessionId).append("\",")
              .append("\"examTitle\":\"").append(esc(exam.getTitle())).append("\",")
              .append("\"durationSeconds\":").append(exam.getDurationMinutes() * 60).append(",")
              .append("\"questions\":[");
            for (int i = 0; i < questions.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(questionToJson(questions.get(i), false)); // hide correctOption during exam
            }
            sb.append("]}");
            sendJson(ex, 200, sb.toString());
        } catch (ValidationException ve) {
            sendJson(ex, 400, "{\"success\":false,\"message\":\"" + esc(ve.getMessage()) + "\"}");
        } catch (Exception e) {
            sendJson(ex, 500, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private void handleSubmitAttempt(HttpExchange ex) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"success\":false,\"message\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> body = parseFlatJson(readBody(ex));
            String sessionId = requireNonEmpty(body.get("sessionId"), "Missing sessionId");
            String studentName = requireNonEmpty(body.get("studentName"), "Student name is required");
            String studentRoll = requireNonEmpty(body.get("studentRoll"), "Roll number is required");
            int examId = parsePositiveInt(body.get("examId"), "Valid examId required");
            boolean autoSubmit = "true".equalsIgnoreCase(body.get("autoSubmit"));

            ExamTimerThread timer = activeTimers.get(sessionId);
            if (timer == null) {
                throw new ValidationException("Invalid or expired exam session");
            }
            if (timer.isExpired() && !autoSubmit) {
                throw new ExamExpiredException("Exam time limit has expired");
            }

            int elapsed = timer.markSubmitted();
            if (elapsed < 0) {
                throw new ValidationException("This exam session was already submitted");
            }
            activeTimers.remove(sessionId);

            // Answers encoded as "qId:opt,qId:opt"
            Map<Integer, String> answers = parseAnswerPairs(body.get("answers"));
            GradingService.EvaluationResult res = gradingService.gradeAndSave(studentName, studentRoll, examId, answers, elapsed);

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"success\":true,")
              .append("\"score\":").append(res.attempt.getScore()).append(",")
              .append("\"total\":").append(res.attempt.getTotal()).append(",")
              .append("\"timeTakenSec\":").append(res.attempt.getTimeTakenSec()).append(",")
              .append("\"breakdown\":[");
            for (int i = 0; i < res.breakdown.size(); i++) {
                GradingService.QuestionResult qr = res.breakdown.get(i);
                if (i > 0) sb.append(",");
                sb.append("{")
                  .append("\"questionId\":").append(qr.questionId).append(",")
                  .append("\"prompt\":\"").append(esc(qr.prompt)).append("\",")
                  .append("\"selectedOption\":\"").append(esc(qr.selectedOption)).append("\",")
                  .append("\"correctOption\":\"").append(esc(qr.correctOption)).append("\",")
                  .append("\"correct\":").append(qr.correct)
                  .append("}");
            }
            sb.append("]}");
            sendJson(ex, 200, sb.toString());
        } catch (ExamExpiredException eee) {
            sendJson(ex, 409, "{\"success\":false,\"message\":\"" + esc(eee.getMessage()) + "\"}");
        } catch (ValidationException ve) {
            sendJson(ex, 400, "{\"success\":false,\"message\":\"" + esc(ve.getMessage()) + "\"}");
        } catch (Exception e) {
            sendJson(ex, 500, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private void handleHistory(HttpExchange ex) throws IOException {
        try {
            String query = ex.getRequestURI().getQuery();
            String roll = extractQueryParam(query, "roll");
            List<Attempt> list = gradingService.getHistory(roll);
            sendJson(ex, 200, attemptsToJson(list));
        } catch (Exception e) {
            sendJson(ex, 500, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private void handleToppers(HttpExchange ex) throws IOException {
        try {
            String query = ex.getRequestURI().getQuery();
            String examIdStr = extractQueryParam(query, "examId");
            int examId = (examIdStr != null && !examIdStr.isEmpty()) ? Integer.parseInt(examIdStr) : 0;
            List<Attempt> toppers = gradingService.getToppers(examId, 10);
            sendJson(ex, 200, attemptsToJson(toppers));
        } catch (Exception e) {
            sendJson(ex, 500, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    private void handleExportCsv(HttpExchange ex) throws IOException {
        try {
            List<Attempt> all = gradingService.getHistory(null);
            String outFile = "exam_results_export.csv";
            csvService.exportAttemptsToCsv(all, outFile);
            byte[] bytes = Files.readAllBytes(Paths.get(outFile));
            ex.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
            ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"exam_results.csv\"");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            sendJson(ex, 500, "{\"success\":false,\"message\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    // --- Helper methods (Strings, StringBuilder, Input Validation) ---

    private String questionToJson(Question q, boolean includeAnswer) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":").append(q.getId()).append(",")
          .append("\"subject\":\"").append(esc(q.getSubject())).append("\",")
          .append("\"prompt\":\"").append(esc(q.getPrompt())).append("\",")
          .append("\"options\":[")
          .append("\"").append(esc(q.getOptions()[0])).append("\",")
          .append("\"").append(esc(q.getOptions()[1])).append("\",")
          .append("\"").append(esc(q.getOptions()[2])).append("\",")
          .append("\"").append(esc(q.getOptions()[3])).append("\"]");
        if (includeAnswer) {
            sb.append(",\"correctOption\":\"").append(esc(q.getCorrectOption())).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String attemptsToJson(List<Attempt> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Attempt a = list.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"id\":").append(a.getId()).append(",")
              .append("\"studentName\":\"").append(esc(a.getStudentName())).append("\",")
              .append("\"studentRoll\":\"").append(esc(a.getStudentRoll())).append("\",")
              .append("\"examId\":").append(a.getExamId()).append(",")
              .append("\"examTitle\":\"").append(esc(a.getExamTitle())).append("\",")
              .append("\"score\":").append(a.getScore()).append(",")
              .append("\"total\":").append(a.getTotal()).append(",")
              .append("\"timeTakenSec\":").append(a.getTimeTakenSec()).append(",")
              .append("\"submittedAt\":\"").append(esc(a.getSubmittedAt())).append("\"")
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendJson(HttpExchange ex, int status, String json) throws IOException {
        sendText(ex, status, "application/json; charset=UTF-8", json);
    }

    private void sendText(HttpExchange ex, int status, String type, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String requireNonEmpty(String val, String msg) throws ValidationException {
        if (val == null || val.trim().isEmpty()) {
            throw new ValidationException(msg);
        }
        return val.trim();
    }

    private int parsePositiveInt(String val, String msg) throws ValidationException {
        try {
            int n = Integer.parseInt(val != null ? val.trim() : "");
            if (n <= 0) throw new ValidationException(msg);
            return n;
        } catch (NumberFormatException e) {
            throw new ValidationException(msg);
        }
    }

    private List<Integer> parseIntList(String csv) {
        List<Integer> ids = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty()) return ids;
        for (String part : csv.split(",")) {
            try {
                ids.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {}
        }
        return ids;
    }

    private Map<Integer, String> parseAnswerPairs(String raw) {
        Map<Integer, String> map = new HashMap<>();
        if (raw == null || raw.trim().isEmpty()) return map;
        for (String pair : raw.split(",")) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                try {
                    map.put(Integer.parseInt(kv[0].trim()), kv[1].trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }

    private String extractQueryParam(String query, String key) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=");
            if (kv.length == 2 && kv[0].equals(key)) {
                return kv[1];
            }
        }
        return null;
    }

  
    private Map<String, String> parseFlatJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        String trimmed = json.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch == '"' && (i == 0 || trimmed.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (ch == ',' && !inQuotes) {
                tokens.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (current.length() > 0) tokens.add(current.toString());

        for (String token : tokens) {
            int colon = token.indexOf(':');
            if (colon > 0) {
                String k = stripQuotes(token.substring(0, colon).trim());
                String v = stripQuotes(token.substring(colon + 1).trim());
                map.put(k, v);
            }
        }
        return map;
    }

    private String stripQuotes(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1).replace("\\\"", "\"");
        }
        return s;
    }
}
