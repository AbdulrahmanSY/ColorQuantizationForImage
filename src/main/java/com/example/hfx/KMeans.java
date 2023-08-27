package com.example.hfx;

import javafx.scene.image.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class KMeans {
    private int k ;
    private double[][] centroids;

    public  Image applyKMeans(Image originalImage, int value) {

        k=value;

        PixelReader pixelReader = originalImage.getPixelReader();
        int width = (int) originalImage.getWidth();
        int height = (int) originalImage.getHeight();

        // Get the pixels of the original image
        int[] pixels = new int[width * height];
        pixelReader.getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        // Convert the pixels to Lab color space
        double[][] pixelsLab = new double[width * height][3];
        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            double[] lab = ColorUtils.rgbToLab(r, g, b);
            pixelsLab[i][0] = lab[0];
            pixelsLab[i][1] = lab[1];
            pixelsLab[i][2] = lab[2];
        }
        // Apply k-means clustering to the pixels in Lab color space
        // the number of colors to quantize to

        double[][] centroids = fit(pixelsLab);

        int[] colors = new int[k];
        for (int i = 0; i < centroids.length; i++) {
            double[] lab = centroids[i];
            int[] rgb = ColorUtils.labToRgb(lab[0], lab[1], lab[2]);
            colors[i] = new java.awt.Color(rgb[0], rgb[1], rgb[2]).getRGB();
        }


        // Map each pixel to its nearest centroid color
        int[] quantizedPixels = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            double[] lab = pixelsLab[i];
            int cluster = predict(lab, centroids);
            quantizedPixels[i] = colors[cluster];
        }

        // Create the quantized image
        WritableImage quantizedImage = new WritableImage(width, height);
        PixelWriter pixelWriter = quantizedImage.getPixelWriter();
        pixelWriter.setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), quantizedPixels, 0, width);

        return quantizedImage;
    }


    public double[][] fit(double[][] data) {
        // Initialize the centroids randomly
        Random random = new Random();
        centroids = new double[k][data[0].length];
        for (int i = 0; i < k; i++) {
            int index = random.nextInt(data.length);
            centroids[i] = data[index];
        }

        // Iterate until convergence
        double prevCost = Double.POSITIVE_INFINITY;
        while (true) {
            // Assign each data point to its nearest centroid
            List<List<double[]>> clusters = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                clusters.add(new ArrayList<>());
            }
            for (double[] point : data) {
                double minDist = Double.POSITIVE_INFINITY;
                int minIndex = -1;
                for (int i = 0; i < k; i++) {
                    double dist = distance(point, centroids[i]);
                    if (dist < minDist) {
                        minDist = dist;
                        minIndex = i;
                    }
                }
                clusters.get(minIndex).add(point);
            }

            // Update the centroids
            double cost = 0.0;
            for (int i = 0; i < k; i++) {
                double[] newCentroid = new double[data[0].length];
                for (double[] point : clusters.get(i)) {
                    for (int j = 0; j < point.length; j++) {
                        newCentroid[j] += point[j];
                    }
                }
                if (!clusters.get(i).isEmpty()) {
                    for (int j = 0; j < newCentroid.length; j++) {
                        newCentroid[j] /= clusters.get(i).size();
                    }
                }
                centroids[i] = newCentroid;

                // Calculate the cost
                for (double[] point : clusters.get(i)) {
                    cost += distance(point, centroids[i]);
                }
            }

            // Check for convergence
            if (cost >= prevCost) {
                break;
            }
            prevCost = cost;
        }

        return centroids;
    }

    public int predict(double[] point, double[][] centroids) {
        double minDist = Double.POSITIVE_INFINITY;
        int minIndex = -1;
        for (int i = 0; i < k; i++) {
            double dist = distance(point, centroids[i]);
            if (dist < minDist) {
                minDist = dist;
                minIndex = i;
            }
        }
        return minIndex;
    }

    private double distance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }



}