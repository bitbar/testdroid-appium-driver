package com.testdroid.appium;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * @author Jarno Tuovinen <jarno.tuovinen@bitbar.com>
 */
class ScreenshotDisplay extends JFrame {
    public void show(File screenshotFile) {
        ImagePanel panel;
        Image image;
        try {
            image = ImageIO.read(screenshotFile);
            panel = new ImagePanel(image);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return;
        }
        add(panel);
        setVisible(true);
        setSize(image.getWidth(null), image.getHeight(null));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
}
