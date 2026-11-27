# Online Examination System (Core Java + MySQL + HTML/CSS/JS)

A simple 2nd-year Java college project (`25CSE0202`) built strictly with syllabus topics—no Spring Boot, Servlets, or external web frameworks.

## Project Structure

- `src/com/exam/Main.java` — Seeds 5 sample Java MCQs and launches the server on port `8080`.
- `src/com/exam/model/` — `Question`, `Exam`, `Attempt` (`Comparable<Attempt>`), and generic `ApiResponse<T>`.
- `src/com/exam/exception/` — Custom exceptions `ExamExpiredException` and `ValidationException`.
- `src/com/exam/dao/` — JDBC `DBConnection` (MySQL), `QuestionDAO`, `ExamDAO`, and `AttemptDAO` using `PreparedStatement`.
- `src/com/exam/thread/` — `ExamTimerThread` (`Runnable`, `Thread.sleep`, `synchronized`, `notifyAll`).
- `src/com/exam/service/` — `GradingService` (Streams & `Comparator`) and `CsvService` (`FileWriter`/`FileReader`).
- `src/com/exam/web/` — `ExamHttpServer` using built-in JDK `com.sun.net.httpserver.HttpServer`.
- `frontend/` — Vanilla `index.html`, `style.css`, and `app.js`.

## How to Compile & Run (MySQL)

1. In MySQL (Workbench or CLI), create the database (tables are created automatically on startup, or you can run `schema.sql`):
   ```sql
   CREATE DATABASE IF NOT EXISTS online_exam_db;
   ```
2. Open `src/com/exam/dao/DBConnection.java` and set `DB_USER` and `DB_PASSWORD` to match your local MySQL credentials.
3. Download **MySQL Connector/J** (e.g., `mysql-connector-j-8.4.0.jar`) and place it in a `lib/` folder inside the project root.
4. Compile all Java files from the project root:
   - **Windows (PowerShell / CMD):**
     ```bat
     mkdir out
     javac -d out -cp "lib/*" src/com/exam/model/*.java src/com/exam/exception/*.java src/com/exam/dao/*.java src/com/exam/thread/*.java src/com/exam/service/*.java src/com/exam/web/*.java src/com/exam/Main.java
     ```
   - **macOS / Linux:**
     ```bash
     mkdir -p out
     javac -d out -cp "lib/*" $(find src -name "*.java")
     ```
5. Run the application:
   - **Windows:** `java -cp "out;lib/*" com.exam.Main`
   - **macOS / Linux:** `java -cp "out:lib/*" com.exam.Main`
6. Open your browser to `http://localhost:8080`.
