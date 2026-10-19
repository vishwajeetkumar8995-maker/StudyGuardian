package org.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SessionHistory {

    // ----  MySQL username ----
    private static final String DB_URL =
            "jdbc:mysql://127.0.0.1:3306/stydy_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Abhi@@9162";

    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS sessions ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "student_name VARCHAR(100) NOT NULL,"
                    + "subject VARCHAR(100),"
                    + "session_date DATETIME NOT NULL,"
                    + "planned_minutes INT,"
                    + "studied_seconds INT,"
                    + "breaks_taken INT,"
                    + "pauses_taken INT,"
                    + "paused_seconds INT,"
                    + "alerts INT,"
                    + "distracted_seconds INT,"
                    + "ended_by VARCHAR(30))";

    private static final String INSERT =
            "INSERT INTO sessions (student_name, subject, session_date, planned_minutes, studied_seconds,"
                    + " breaks_taken, pauses_taken, paused_seconds, alerts, distracted_seconds, ended_by)"
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?)";

    // true = MySQL me save hua, false = MySQL fail hua aur CSV me save hua
    public static boolean save(StudySession s, String endedBy) {
        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            try (Statement st = con.createStatement()) {
                st.execute(CREATE_TABLE);
            }

            try (PreparedStatement ps = con.prepareStatement(INSERT)) {
                ps.setString(1, s.getName());
                ps.setString(2, s.getSubject());
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(4, s.getTotalMinutes());
                ps.setLong(5, s.studiedMillis() / 1000);
                ps.setInt(6, s.getBreaksTaken());
                ps.setInt(7, s.getPausesTaken());
                ps.setLong(8, s.getPausedSeconds());
                ps.setInt(9, s.getAlertCount());
                ps.setLong(10, s.getDistractedSeconds());
                ps.setString(11, endedBy);
                ps.executeUpdate();
            }
            return true;

        } catch (SQLException e) {
            System.out.println("MySQL me save nahi hua: " + e.getMessage());
            saveToCsv(s, endedBy);
            return false;
        }
    }

    // Backup: MySQL na chale to data yahan save hoga
    private static void saveToCsv(StudySession s, String endedBy) {
        File file = new File("study_history_backup.csv");
        boolean newFile = !file.exists();

        try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
            if (newFile) {
                out.println("Date,Name,Subject,Planned(min),Studied(sec),Breaks,Pauses,Paused(sec),Alerts,Distracted(sec),Ended by");
            }
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
            out.println(time + "," + s.getName().replace(",", " ") + "," + s.getSubject().replace(",", " ") + ","
                    + s.getTotalMinutes() + "," + (s.studiedMillis() / 1000) + "," + s.getBreaksTaken() + ","
                    + s.getPausesTaken() + "," + s.getPausedSeconds() + "," + s.getAlertCount() + ","
                    + s.getDistractedSeconds() + "," + endedBy);
        } catch (IOException e) {
            System.out.println("CSV backup bhi save nahi hua: " + e.getMessage());
        }
    }
}