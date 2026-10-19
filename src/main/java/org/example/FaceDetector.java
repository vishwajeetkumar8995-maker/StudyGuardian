package org.example;

import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class FaceDetector {

    private static volatile boolean endRequested = false;
    private static volatile boolean pauseToggleRequested = false;

    private static int askNumber(String message, int min) {
        while (true) {
            String text = JOptionPane.showInputDialog(null, message, "Study Guardian", JOptionPane.QUESTION_MESSAGE);
            if (text == null) System.exit(0);
            try {
                int value = Integer.parseInt(text.trim());
                if (value >= min) return value;
            } catch (NumberFormatException ignored) {}
            JOptionPane.showMessageDialog(null, "Sahi number daalo (kam se kam " + min + ")");
        }
    }

    private static String askText(String message, String defaultValue) {
        String text = JOptionPane.showInputDialog(null, message, "Study Guardian", JOptionPane.QUESTION_MESSAGE);
        if (text == null) System.exit(0);
        text = text.trim();
        return text.isEmpty() ? defaultValue : text;
    }

    public static void main(String[] args) throws Exception {

        // ---------- 1. User se details lo ----------
        String name = askText("Aapka naam?", "Student");
        String subject = askText("Kya padh rahe ho? (subject/topic)", "General");
        int totalMin = askNumber("Kitne minute padhna hai? (total time, breaks ke saath)", 1);
        int breakEvery = askNumber("Har kitne minute padhai ke baad break chahiye? (0 = koi break nahi)", 0);
        int breakLen = 0;
        if (breakEvery > 0) {
            breakLen = askNumber("Break kitne minute ka hoga?", 1);
        }

        // ---------- 2. Detectors aur camera ----------
        CascadeClassifier faceDetector = new CascadeClassifier("src/main/resources/haarcascade_frontalface_default.xml");
        CascadeClassifier eyeDetector = new CascadeClassifier("src/main/resources/haarcascade_eye.xml");

        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        int frameHeight = grabber.getImageHeight();

        Mat gray = new Mat();
        RectVector faces = new RectVector();
        RectVector eyes = new RectVector();

        DistractionTracker tracker = new DistractionTracker(5);
        StudySession session = new StudySession(name, subject, totalMin, breakEvery, breakLen);

        // ---------- 3. Status window ----------
        JLabel nameLabel = new JLabel("Student: " + name + "  |  " + subject);
        JLabel timeLabel = new JLabel("Time left: " + StudySession.format(session.remainingMillis()));
        JLabel statusLabel = new JLabel("Status: Focused");
        JLabel countLabel = new JLabel("Alerts: 0   Breaks: 0   Pauses: 0");
        JButton pauseButton = new JButton("Pause");
        JButton endButton = new JButton("End session");

        Font font = new Font("Arial", Font.PLAIN, 16);
        for (JLabel label : new JLabel[]{nameLabel, timeLabel, statusLabel, countLabel}) {
            label.setFont(font);
        }

        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.add(pauseButton);
        buttons.add(endButton);

        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        panel.setPreferredSize(new Dimension(380, 240));
        panel.add(nameLabel);
        panel.add(timeLabel);
        panel.add(statusLabel);
        panel.add(countLabel);
        panel.add(buttons);

        JFrame window = new JFrame("Study Guardian");
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                endRequested = true;
            }
        });
        endButton.addActionListener(e -> endRequested = true);
        pauseButton.addActionListener(e -> pauseToggleRequested = true);
        window.add(panel);
        window.pack();
        window.setLocation(30, 30);
        window.setVisible(true);

        // ---------- 4. Main loop ----------
        boolean wasOnBreak = false;
        boolean wasAlerting = false;
        long lastFrameTime = System.currentTimeMillis();
        long lastLabelUpdate = 0;

        while (!endRequested && !session.isFinished()) {
            Frame frame = grabber.grab();
            if (frame == null) continue;

            long now = System.currentTimeMillis();
            long frameGap = now - lastFrameTime;
            lastFrameTime = now;

            // Pause / Resume button dabaya gaya?
            if (pauseToggleRequested) {
                pauseToggleRequested = false;
                if (session.isPaused()) {
                    session.resume();
                    tracker.reset();
                } else {
                    session.pause();
                }
                String buttonText = session.isPaused() ? "Resume" : "Pause";
                SwingUtilities.invokeLater(() -> pauseButton.setText(buttonText));
                lastLabelUpdate = 0; // label turant update ho
            }

            boolean paused = session.isPaused();
            boolean onBreak = session.isOnBreak();
            boolean alerting = false;

            if (paused) {
                tracker.reset();
                Thread.sleep(100);
            } else if (onBreak) {
                if (!wasOnBreak) {
                    session.breakStarted();
                    tracker.reset();
                    AlertSystem.notifyBeep();
                }
            } else {
                if (wasOnBreak) {
                    AlertSystem.notifyBeep();
                }

                Mat mat = converter.convert(frame);
                cvtColor(mat, gray, COLOR_BGR2GRAY);
                faceDetector.detectMultiScale(gray, faces);

                boolean isBadFrame;
                if (faces.size() == 0) {
                    isBadFrame = true;
                } else {
                    Rect face = faces.get(0);
                    int faceCenterY = face.y() + face.height() / 2;
                    boolean headDown = (double) faceCenterY / frameHeight > 0.65;

                    Mat faceROI = gray.apply(face);
                    eyeDetector.detectMultiScale(faceROI, eyes);
                    boolean eyesClosed = eyes.size() == 0;

                    isBadFrame = headDown || eyesClosed;
                }

                alerting = tracker.update(isBadFrame);
                if (alerting) {
                    session.addDistracted(frameGap);
                    if (!wasAlerting) session.addAlert();
                }
            }

            if (paused) {
                wasAlerting = false;
            } else {
                wasOnBreak = onBreak;
                wasAlerting = alerting;
            }
            AlertSystem.setDistracted(alerting);

            // Window ke labels har 1 second me update
            if (now - lastLabelUpdate >= 1000) {
                lastLabelUpdate = now;
                String timeText = "Time left: " + StudySession.format(session.remainingMillis());
                String statusText;
                if (paused) statusText = "Status: PAUSED - timer ruka hua hai";
                else if (onBreak) statusText = "Status: BREAK - aaram karo";
                else if (alerting) statusText = "Status: DISTRACTED";
                else statusText = "Status: Focused";
                String countText = "Alerts: " + session.getAlertCount()
                        + "   Breaks: " + session.getBreaksTaken()
                        + "   Pauses: " + session.getPausesTaken();
                SwingUtilities.invokeLater(() -> {
                    timeLabel.setText(timeText);
                    statusLabel.setText(statusText);
                    countLabel.setText(countText);
                });
            }
        }

        // ---------- 5. Session khatam: cleanup + history ----------
        AlertSystem.setDistracted(false);
        grabber.stop();

        String endedBy = endRequested ? "Stopped by user" : "Time complete";
        boolean savedInMySql = SessionHistory.save(session, endedBy);
        window.dispose();

        String saveInfo = savedInMySql
                ? "History MySQL me save ho gayi."
                : "MySQL connect nahi hua, isliye history study_history_backup.csv me save hui.";

        JOptionPane.showMessageDialog(null,
                "Session khatam!\n\n"
                        + "Naam: " + name + "\n"
                        + "Subject: " + subject + "\n"
                        + "Padhai ka time: " + StudySession.format(session.studiedMillis()) + " (mm:ss)\n"
                        + "Breaks: " + session.getBreaksTaken() + "\n"
                        + "Pauses: " + session.getPausesTaken() + " (" + session.getPausedSeconds() + " sec)\n"
                        + "Alerts: " + session.getAlertCount() + "\n"
                        + "Distracted time: " + session.getDistractedSeconds() + " sec\n\n"
                        + saveInfo,
                "Study Guardian", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }
}