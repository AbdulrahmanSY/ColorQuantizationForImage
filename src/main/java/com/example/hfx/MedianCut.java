package com.example.hfx;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

public class MedianCut {
    public static Image applyMedianCut(Image img, int colorCount) {
        BufferedImage image = convertToBufferedImage(img);
        List<Color> colors = extractColors(image);
        List<ColorCube> colorCubes = splitCubes(colors, colorCount);
        BufferedImage reducedImage = reduceColors(image, colorCubes);

        return SwingFXUtils.toFXImage(reducedImage, null);
    }

    private static List<Color> extractColors(BufferedImage image) {
        Map<Color, Integer> colorMap = new HashMap<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
                colorMap.put(color, colorMap.getOrDefault(color, 0) + 1);
            }
        }
        List<Color> colors = new ArrayList<>(colorMap.keySet());
        colors.forEach(color -> color.count = colorMap.get(color));
        return colors;
    }
    private static List<ColorCube> splitCubes(List<Color> colors, int colorCount) {
        List<ColorCube> colorCubes = new ArrayList<>();
        ColorCube initialCube = new ColorCube(colors);
        colorCubes.add(initialCube);

        while (colorCubes.size() < colorCount) {
            ColorCube cubeToSplit = Collections.max(colorCubes, Comparator.comparing(ColorCube::getLongestSide));
            colorCubes.remove(cubeToSplit);
            colorCubes.addAll(cubeToSplit.split());
        }

        return colorCubes;
    }
    private static BufferedImage reduceColors(BufferedImage image, List<ColorCube> colorCubes) {
        BufferedImage reducedImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
                ColorCube closestCube = colorCubes.stream().min(Comparator.comparing(cube -> cube.distanceTo(color))).orElse(null);
                Color reducedColor = closestCube.getAverageColor();
                reducedImage.setRGB(x, y, reducedColor.getRGB());
            }
        }
        return reducedImage;
    }

    public static BufferedImage convertToBufferedImage(javafx.scene.image.Image image) {
        BufferedImage bufferedImage = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(SwingFXUtils.fromFXImage(image, null), 0, 0, null);
        graphics.dispose();
        return bufferedImage;
    }
}

class Color extends java.awt.Color {
    int count;

    Color(int r, int g, int b) {
        super(r, g, b);
        count = 0;
    }
}

class ColorCube {
    List<Color> colors;
    int redMin, redMax, greenMin, greenMax, blueMin, blueMax;

    ColorCube(List<Color> colors) {
        this.colors = colors;
        updateBounds();
    }

    void updateBounds() {
        redMin = colors.stream().mapToInt(Color::getRed).min().orElse(0);
        redMax = colors.stream().mapToInt(Color::getRed).max().orElse(0);
        greenMin = colors.stream().mapToInt(Color::getGreen).min().orElse(0);
        greenMax = colors.stream().mapToInt(Color::getGreen).max().orElse(0);
        blueMin = colors.stream().mapToInt(Color::getBlue).min().orElse(0);
        blueMax = colors.stream().mapToInt(Color::getBlue).max().orElse(0);
    }

    int getLongestSide() {
        return Math.max(redMax - redMin, Math.max(greenMax - greenMin, blueMax - blueMin));
    }

    List<ColorCube> split() {
        int longestSide = getLongestSide();
        colors.sort(Comparator.comparingInt(color -> {
            if (longestSide == redMax - redMin) {
                return color.getRed();
            } else if (longestSide == greenMax - greenMin) {
                return color.getGreen();
            } else {
                return color.getBlue();
            }
        }));

        int medianIndex = colors.size() / 2;
        List<Color> firstHalf = colors.subList(0, medianIndex);
        List<Color> secondHalf = colors.subList(medianIndex, colors.size());

        return Arrays.asList(new ColorCube(firstHalf), new ColorCube(secondHalf));
    }

    Color getAverageColor() {
        int redSum = colors.stream().mapToInt(Color::getRed).sum();
        int greenSum = colors.stream().mapToInt(Color::getGreen).sum();
        int blueSum = colors.stream().mapToInt(Color::getBlue).sum();
        int totalCount = colors.stream().mapToInt(color -> color.count).sum();

        return new Color(redSum / totalCount, greenSum / totalCount, blueSum / totalCount);
    }

    double distanceTo(Color color) {
        int redMean = (color.getRed() + redMin + redMax) / 3;
        int greenMean = (color.getGreen() + greenMin + greenMax) / 3;
        int blueMean = (color.getBlue() + blueMin + blueMax) / 3;

        return Math.sqrt(Math.pow(color.getRed() - redMean, 2) + Math.pow(color.getGreen() - greenMean, 2) + Math.pow(color.getBlue() - blueMean, 2));
    }
}
