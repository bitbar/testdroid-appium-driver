package com.testdroid.appium;

import javax.swing.*;
import java.awt.*;

/**
 * @author Jarno Tuovinen <jarno.tuovinen@bitbar.com>
 */
// @TODO make screenshot displaying to fit to screen, reuse the frame
class ImagePanel extends JPanel {
    private Image img;

    public ImagePanel(Image image) {
        this.img = image;
        Dimension size = new Dimension(image.getWidth(null), image.getHeight(null));
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setSize(size);
        setLayout(null);
    }

    public void paintComponent(Graphics g) {
        g.drawImage(img, 0, 0, null);
    }
}
