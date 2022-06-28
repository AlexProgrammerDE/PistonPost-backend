package net.pistonmaster.pistonpost.gif;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.awt.image.BufferedImage;

/**
 * Taken from <a href="https://stackoverflow.com/a/17269591/">StackOverFlow</a>
 */
@Data
@AllArgsConstructor
public class ImageFrame {
    private BufferedImage image;
    private int delay;
    private String disposal;
    private int width;
    private int height;
}
