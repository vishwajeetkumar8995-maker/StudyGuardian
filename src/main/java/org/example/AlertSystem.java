package org.example;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.Color;
import java.awt.Font;
import java.io.File;

public class AlertSystem {

    // Apna song yahan rakhna hai (WAV format)
    private static final String MUSIC_FILE = "src/main/resources/music.wav";

    private static volatile boolean distracted = false;
    private static Clip musicClip = null;
    private static JDialog popup = null;

    // FaceDetector har frame par ye call karta hai
    public static synchronized void setDistracted(boolean isDistracted) {
        if (isDistracted && !distracted) {
            distracted = true;
            startMusic();
            showPopup();
        } else if (!isDistracted && distracted) {
            distracted = false;
            stopMusic();
            hidePopup();
        }
    }

    // Break shuru aur khatam hone par chhota beep
    public static void notifyBeep() {
        new Thread(AlertSystem::playBeep).start();
    }

    // ---------- Popup ----------

    private static void showPopup() {
        SwingUtilities.invokeLater(() -> {
            if (popup != null) return;

            popup = new JDialog();
            popup.setTitle("STUDY GUARDIAN ALERT");
            popup.setModal(false);          // camera loop ruke nahi
            popup.setAlwaysOnTop(true);     // dusri windows ke upar dikhe
            popup.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            JLabel label = new JLabel("PADHAI KARO!!! PADHAI KARO!!!", SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 28));
            label.setForeground(Color.RED);
            label.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

            popup.getContentPane().add(label);
            popup.pack();
            popup.setLocationRelativeTo(null);
            popup.setVisible(true);
        });
    }

    private static void hidePopup() {
        SwingUtilities.invokeLater(() -> {
            if (popup != null) {
                popup.dispose();
                popup = null;
            }
        });
    }

    // ---------- Music ----------

    private static void startMusic() {
        File file = new File(MUSIC_FILE);
        if (file.exists()) {
            try {
                AudioInputStream stream = AudioSystem.getAudioInputStream(file);
                musicClip = AudioSystem.getClip();
                musicClip.open(stream);
                musicClip.loop(Clip.LOOP_CONTINUOUSLY); // jab tak distracted hai, baar-baar bajega
                return;
            } catch (Exception e) {
                System.out.println("Music file nahi chali, built-in melody bajegi: " + e.getMessage());
            }
        }
        startMelodyFallback();
    }

    private static void stopMusic() {
        if (musicClip != null) {
            musicClip.stop();
            musicClip.close();
            musicClip = null;
        }
        // Fallback melody ka thread khud ruk jayega (distracted false hote hi)
    }

    // Song file na mile to ye melody bajti hai
    private static void startMelodyFallback() {
        Thread t = new Thread(() -> {
            double[] notes = {523.25, 587.33, 659.25, 698.46, 783.99, 659.25, 783.99, 523.25};
            try {
                float sampleRate = 44100;
                AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format);
                line.start();

                while (distracted) {
                    for (double frequency : notes) {
                        if (!distracted) break;
                        playNote(line, frequency, 350, sampleRate);
                    }
                }
                line.flush();
                line.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static void playNote(SourceDataLine line, double frequency, int durationMs, float sampleRate) {
        int numSamples = (int) (sampleRate * durationMs / 1000);
        byte[] buffer = new byte[numSamples * 2];
        int fade = (int) (sampleRate * 0.02); // 20 ms fade, taaki click ki awaaz na aaye

        for (int i = 0; i < numSamples; i++) {
            double angle = 2.0 * Math.PI * i * frequency / sampleRate;
            double envelope = Math.min(1.0, Math.min(i, numSamples - i) / (double) fade);
            short sample = (short) (Math.sin(angle) * Short.MAX_VALUE * 0.6 * envelope);
            buffer[2 * i] = (byte) (sample & 0xFF);
            buffer[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        line.write(buffer, 0, buffer.length);
    }

    // ---------- Chhota beep (break ke liye) ----------

    private static void playBeep() {
        try {
            float sampleRate = 44100;
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();
            playNote(line, 1000, 300, sampleRate);
            line.drain();
            line.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}