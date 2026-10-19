package org.example;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;

public class CameraCapture{
    public static void main(String[] args) throws Exception {

        // Camera 0 = default webcam
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();

        // Window jisme live feed dikhega
        CanvasFrame canvas = new CanvasFrame("Study Guardian - Camera Feed");
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

        System.out.println("Camera start ho gaya! Window band karne ke liye X dabao.");

        while (canvas.isVisible()) {
            org.bytedeco.javacv.Frame frame = grabber.grab();
            if (frame != null) {
                canvas.showImage(frame);
            }
        }

        grabber.stop();
        canvas.dispose();
    }
}