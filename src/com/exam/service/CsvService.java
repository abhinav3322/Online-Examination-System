package com.exam.service;

import com.exam.model.Attempt;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CsvService {

    public void exportAttemptsToCsv(List<Attempt> attempts, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("ID,Student Name,Roll No,Exam,Score,Total,Time Taken (sec),Submitted At\n");
            for (Attempt a : attempts) {
                writer.write(String.format("%d,%s,%s,%s,%d,%d,%d,%s\n",
                        a.getId(),
                        escapeCsv(a.getStudentName()),
                        escapeCsv(a.getStudentRoll()),
                        escapeCsv(a.getExamTitle()),
                        a.getScore(),
                        a.getTotal(),
                        a.getTimeTakenSec(),
                        escapeCsv(a.getSubmittedAt())));
            }
        }
    }

    public List<String[]> readCsvRows(String filePath) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    rows.add(line.split(","));
                }
            }
        }
        return rows;
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}
