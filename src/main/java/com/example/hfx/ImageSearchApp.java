package com.example.hfx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ImageSearchApp extends Application {
    private List<File> getFilesRecursively(File folder, FilenameFilter filter) {
        List<File> files = new ArrayList<>();
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                files.addAll(getFilesRecursively(file, filter));
            } else if (filter.accept(folder, file.getName())) {
                files.add(file);
            }
        }
        return files;
    }
    private Rectangle2D cropRect;

    public static void main(String[] args) {
        launch(args);
    }

    Image image;
    String imagePath;
    String imageSize;
    String imageReSizeHeight;
    String imageReSizeWidth;
    String imagePathCrop;
    LocalDateTime selectedDate = LocalDateTime.now();
    List<File> listSimilarImageFiles = new ArrayList<>();
    List<File> files;
    VBox imageBox = new VBox(10);

    public void start(Stage primaryStage) {
        primaryStage.setTitle("Image Search App");

        GridPane gridPane = new GridPane();

        TextField fileTextField = new TextField();
        fileTextField.setPromptText("Select image...");
        fileTextField.setEditable(false);

        TextField folderTextField = new TextField();
        folderTextField.setPromptText("Select folder...");
        folderTextField.setEditable(false);

        VBox colorPickerBox = new VBox(10);
        colorPickerBox.setPadding(new Insets(5));
        Label colorLabel = new Label("Colors:");
        colorLabel.setWrapText(true);
        colorPickerBox.getChildren().add(colorLabel);

        List<ColorPicker> colorPickers = new ArrayList<>();
        addColorPicker(colorPickerBox, colorPickers);

        Button addColorButton = new Button("Add Color");
        addColorButton.setOnAction(e -> addColorPicker(colorPickerBox, colorPickers));

        Button searchButtonImage = new Button("Search");
        Button searchButtonColor=new Button("Color");
        Button resizeImage=new Button("Resize");
        searchButtonImage.setPrefWidth(200);
        searchButtonColor.setPrefWidth(200);
        resizeImage.setPrefWidth(200);

        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(300);
        imageView.setFitWidth(300);

        Button cropButton = new Button("Crop");
        cropButton.setPrefWidth(200);
        cropButton.setOnAction(e -> {
            // Create an instance of CropImage and pass the original image
            CropImage c = new CropImage(image);
            Stage cropStage = new Stage();

            try {
                // Start the CropImage stage
                c.start(cropStage);

                // Wait for the CropImage stage to be closed
                cropStage.setOnHidden(event -> {
                    // Get the cropped image from the CropImage instance
                    image = c.getCropImage().getImage();
                    // Convert the cropped Image to a WritableImage
                    WritableImage writableCroppedImage = new WritableImage((int) image.getWidth(), (int) image.getHeight());
                    PixelReader pixelReader = image.getPixelReader();
                    PixelWriter pixelWriter = writableCroppedImage.getPixelWriter();
                    for (int y = 0; y < image.getHeight(); y++) {
                        for (int x = 0; x < image.getWidth(); x++) {
                            Color color = pixelReader.getColor(x, y);
                            pixelWriter.setColor(x, y, color);
                        }
                    }

                    // Save the cropped image
                    c.saveCroppedImage(cropStage, writableCroppedImage);
                    // Set the cropped image as the new image for the ImageView
                    imageView.setImage(image);

                    imagePathCrop = c.getCroppedImagePath();
                });

                // Show the CropImage stage
                cropStage.show();

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        });
        Button sizeButton = new Button("Size");
        sizeButton.setPrefWidth(200);
        sizeButton.setOnAction(e -> {
            List<File> filesToRemove = new ArrayList<>();
            List<File> listSimilarImageFilesSize = new ArrayList<>(listSimilarImageFiles);

            if (listSimilarImageFilesSize != null && listSimilarImageFilesSize.size() > 0) {
                for (File file : listSimilarImageFilesSize) {
                    if (file.isFile()) {
                        long fileSizeBytes = file.length();

                        if (Long.parseLong(imageSize.replace(",", "")) < fileSizeBytes) {
                            filesToRemove.add(file);
                        }
                    }
                }

                listSimilarImageFilesSize.removeAll(filesToRemove);
                imageBox.getChildren().clear();
                for (File file : listSimilarImageFilesSize) {
                    System.out.println("Size Image ");
                    System.out.println(file.length());
                    Image image = new Image(file.toURI().toString());
                    Platform.runLater(() -> {
                        ImageView similarImageView = new ImageView(image);
                        similarImageView.setPreserveRatio(true);
                        similarImageView.setFitHeight(200);
                        similarImageView.setFitWidth(400);
                        imageBox.getChildren().add(similarImageView);
                    });
                }
            }
        });



        Button dateButton = new Button("Date");
        dateButton.setPrefWidth(200);
        dateButton.setOnAction(e -> {
            List<File> filesToRemove = new ArrayList<>();
            List<File> listSimilarImageFilesDate = new ArrayList<>(listSimilarImageFiles);

            if (listSimilarImageFilesDate != null && listSimilarImageFilesDate.size() > 0) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd  a");
                for (File file : listSimilarImageFilesDate) {
                    Path filePath = file.toPath();
                    BasicFileAttributes attr = null;
                    try {
                        attr = Files.readAttributes(filePath, BasicFileAttributes.class);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    FileTime creationTime = attr.creationTime();
                    LocalDateTime existingImageDateTime = LocalDateTime.ofInstant(creationTime.toInstant(), ZoneId.systemDefault());
                    String formattedDateTime = existingImageDateTime.format(formatter);
                    System.out.println(formattedDateTime);
                    if (selectedDate.toLocalDate().isBefore(existingImageDateTime.toLocalDate())) {
                        System.out.println("yyyyes");
                        filesToRemove.add(file);
                    }
                }
                listSimilarImageFilesDate.removeAll(filesToRemove);
                imageBox.getChildren().clear();

                for (File file : listSimilarImageFilesDate) {
                    Image image = new Image(file.toURI().toString());
                    Platform.runLater(() -> {
                        ImageView similarImageView = new ImageView(image);
                        similarImageView.setPreserveRatio(true);
                        similarImageView.setFitHeight(200);
                        similarImageView.setFitWidth(400);
                        imageBox.getChildren().add(similarImageView);
                    });
                }
            }
        });


        FileChooser fileChooser = new FileChooser();

        fileTextField.setOnMouseClicked(e -> {
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                String imagePath = file.toURI().toString();
                fileTextField.setText(imagePath);

                image = new Image(imagePath);
                imageView.setImage(image);
            }
        });

        DirectoryChooser directoryChooser = new DirectoryChooser();

        folderTextField.setOnMouseClicked(e -> {
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                String folderPath = selectedDirectory.getAbsolutePath();
                folderTextField.setText(folderPath);
            }
        });


        TextField enterReSizeHeight = new TextField();

        enterReSizeHeight.textProperty().addListener((observable, oldValue, newValue) -> {
            imageReSizeHeight = newValue;

        });
        TextField enterReSizeWidth = new TextField();

        enterReSizeWidth.textProperty().addListener((observable, oldValue, newValue) -> {
            imageReSizeWidth = newValue;

        });


        TextField enterSize = new TextField();

        enterSize.textProperty().addListener((observable, oldValue, newValue) -> {
            imageSize = newValue;

        });
        DatePicker enterDate = new DatePicker(LocalDate.now());
        enterDate.setPromptText("Select date ");
        enterDate.setOnAction(event -> {
            LocalDate selectedDate = enterDate.getValue();
            LocalTime selectedTime = LocalTime.of(6, 0); // Set the default time to 06:00 AM
            LocalDateTime selectedDateTime = LocalDateTime.of(selectedDate, selectedTime);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd  a");
            String formattedDateTime = selectedDateTime.format(formatter);

            System.out.println(formattedDateTime);
        });
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.addRow(0, new Label("Image Path:"), fileTextField);
        gridPane.addRow(1, new Label("Folder Path:"), folderTextField);
        gridPane.addRow(2, new Label("Filter by size :"), enterSize);
        gridPane.addRow(3, new Label("Filter by Date :"), enterDate);
        gridPane.addRow(6, colorPickerBox, addColorButton);
        gridPane.addRow(4, new Label("Resize image height :"), enterReSizeHeight);
        gridPane.addRow(5, new Label("Resize image width :"), enterReSizeWidth);
        gridPane.addRow(7, sizeButton, cropButton);
        gridPane.addRow(8, dateButton, resizeImage);
        gridPane.addRow(9, searchButtonColor, searchButtonImage);


        HBox hbox = new HBox(10);
        hbox.setPadding(new Insets(10));
        hbox.getChildren().addAll(gridPane);
        Separator separator = new Separator();
        separator.setOrientation(Orientation.VERTICAL);
        hbox.getChildren().add(separator);
        hbox.getChildren().addAll(imageView);

        ScrollPane scrollPane = new ScrollPane();

        scrollPane.setContent(imageBox);
        hbox.getChildren().addAll(scrollPane);

        Scene scene = new Scene(hbox, 800, 400);
        primaryStage.setScene(scene);
        primaryStage.show();

        resizeImage.setOnAction(event -> {
            imageBox.getChildren().clear();

            imageView.setFitWidth(Double.parseDouble(imageReSizeWidth));
            imageView.setFitHeight(Double.parseDouble(imageReSizeHeight));
            imageView.setPreserveRatio(true);


        });


        searchButtonColor.setOnAction(event -> {
            imageBox.getChildren().clear();

            List<Color> selectedColors = new ArrayList<>();
            for (ColorPicker colorPicker : colorPickers) {
                selectedColors.add(colorPicker.getValue());
            }

            File folder = new File(folderTextField.getText());
            files = getFilesRecursively(folder, (dir, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg"));

            searchImagesWithSelectedColors(selectedColors,files,imageBox);

        });

        searchButtonImage.setOnAction(event -> {
            imageBox.getChildren().clear();
            if (imagePathCrop != null)
                imagePath = imagePathCrop;
            else
                imagePath = fileTextField.getText();

            image = new Image(imagePath);
            Color[][] enteredImageColors = extractImageColors(image);
            int[][][] enteredImageHistogram = GetHistogram(enteredImageColors);

            // Iterate over all the images in the selected folder and its subfolders
            File folder = new File(folderTextField.getText());
            files = getFilesRecursively(folder, (dir, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg"));


            files.forEach(file -> {

                Image image = new Image(file.toURI().toString());
                Color[][] imageColors = extractImageColors(image);
                int[][][] imageHistogram = GetHistogram(imageColors);

                double similarity = compareHistograms(enteredImageHistogram, imageHistogram);

                if (similarity > 0.2) {  // Adjust this threshold as needed
                    System.out.println("similarity");
                    listSimilarImageFiles.add(file);
                    System.out.println(similarity);
                    Platform.runLater(() -> {
                        ImageView similarImageView = new ImageView(image);
                        similarImageView.setPreserveRatio(true);
                        similarImageView.setFitHeight(200);
                        similarImageView.setFitWidth(400);
                        imageBox.getChildren().add(similarImageView);
                    });
                }
            });
        });
    }


    public void searchImagesWithSelectedColors(List<Color> selectedColors, List<File> files, VBox imageBox) {
        for (File file : files) {
            if (file.getName().endsWith(".jpg") || file.getName().endsWith(".png") || file.getName().endsWith(".jpeg")) {
                try {
                    BufferedImage image = ImageIO.read(file);
                    Map<Color, Integer> colorCountMap = new HashMap<>();
                    for (Color color : selectedColors) {
                        colorCountMap.put(color, 0);
                    }

                    boolean anyColorPresent = false;
                    boolean allColorsPresent = true;

                    for (int y = 0; y < image.getHeight(); y++) {
                        for (int x = 0; x < image.getWidth(); x++) {
                            java.awt.Color pixelColor = new java.awt.Color(image.getRGB(x, y));
                            for (Color color : selectedColors) {
                                if (isCloseTo(pixelColor, color,80)) {
                                    colorCountMap.put(color, colorCountMap.get(color) + 1);
//                                    if (colorCountMap.get(color) > image.getWidth() * image.getHeight() / 8) {
//                                        anyColorPresent = true;
//                                    }
                                }
                            }
                        }
                    }

                    for (Color color : selectedColors) {
                        if (colorCountMap.get(color) <= image.getWidth() * image.getHeight() / 8) {
                            allColorsPresent = false;
                            break;
                        }
                    }

                    // if (anyColorPresent|| allColorsPresent) {
                    if (allColorsPresent) {
                        Platform.runLater(() -> {
                            ImageView similarImageView = new ImageView(file.getPath());
                            similarImageView.setPreserveRatio(true);
                            similarImageView.setFitHeight(200);
                            similarImageView.setFitWidth(400);
                            imageBox.getChildren().add(similarImageView);
                        });
                    }

                    System.out.println(file.getName() + ": " + colorCountMap);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        System.out.println("*************************************");
    }

    private boolean isCloseTo(java.awt.Color pixelColor, Color selectedColor, int tolerance) {
        int redDiff = Math.abs(pixelColor.getRed() - (int) (selectedColor.getRed() * 255));
        int greenDiff = Math.abs(pixelColor.getGreen() - (int) (selectedColor.getGreen() * 255));
        int blueDiff = Math.abs(pixelColor.getBlue() - (int) (selectedColor.getBlue() * 255));
        return redDiff < tolerance && greenDiff < tolerance && blueDiff < tolerance;
    }

    private void addColorPicker(VBox colorPickerBox, List<ColorPicker> colorPickers) {
        ColorPicker colorPicker = new ColorPicker();
        colorPickers.add(colorPicker);
        colorPickerBox.getChildren().add(colorPicker);
    }

    private Color[][] extractImageColors(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        Color[][] colors = new Color[width][height];

        PixelReader pixelReader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                colors[x][y] = pixelReader.getColor(x, y);
            }
        }

        return colors;
    }

    private int[][][] GetHistogram(Color[][] colors) {
        int numBins = 8;
        int[][][] histogram = new int[numBins][numBins][numBins];

        for (Color[] colorRow : colors) {
            for (Color color : colorRow) {
                int redBin = (int) (color.getRed() * (numBins - 1));
                int greenBin = (int) (color.getGreen() * (numBins - 1));
                int blueBin = (int) (color.getBlue() * (numBins - 1));
                histogram[redBin][greenBin][blueBin]++;
            }
        }

        return histogram;
    }

    private double compareHistograms(int[][][] hist1, int[][][] hist2) {
        int numBins = hist1.length;
        double[] hist1Vector = new double[numBins * numBins * numBins];
        double[] hist2Vector = new double[numBins * numBins * numBins];
        int index = 0;

        for (int i = 0; i < numBins; i++) {
            for (int j = 0; j < numBins; j++) {
                for (int k = 0; k < numBins; k++) {
                    hist1Vector[index] = hist1[i][j][k];
                    hist2Vector[index] = hist2[i][j][k];
                    index++;
                }
            }
        }

        return correlation(hist1Vector, hist2Vector);
    }

    private double correlation(double[] x, double[] y) {
        double xMean = mean(x);
        double yMean = mean(y);
        double sumXY = 0.0;
        double sumX2 = 0.0;
        double sumY2 = 0.0;

        for (int i = 0; i < x.length; i++) {
            sumXY += (x[i] - xMean) * (y[i] - yMean);
            sumX2 += Math.pow(x[i] - xMean, 2);
            sumY2 += Math.pow(y[i] - yMean, 2);
        }

        return sumXY / Math.sqrt(sumX2 * sumY2);
    }

    private double mean(double[] x) {
        double sum = 0.0;
        for (double xi : x) {
            sum += xi;
        }
        return sum / x.length;
    }

}
