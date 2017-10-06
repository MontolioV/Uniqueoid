package com.unduplicator.gui;

import com.unduplicator.ResourcesProvider;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * <p>Created by MontolioV on 29.09.17.
 */
public class AboutChunk extends AbstractGUIChunk {
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private ChunkManager chunkManager;

    private ArrayList<Thread> threads = new ArrayList<>();
    private Thread interactionThread = null;
    private boolean running = true;

    private ImageView rainbowImageView = new ImageView();
    private Text fpsRepresentation = new Text();
    private Hyperlink gitHubLink = new Hyperlink();
    private Hyperlink mailToLink = new Hyperlink();
    private Label nameYearLabel = new Label();

    private final int WIDTH = 500;
    private final int HEIGHT = 300;
    private final int MIDDLE = WIDTH / 2;
    private final int RANDOM_LOWER_BOUND = -WIDTH;
    private final int RANDOM_UPPER_BOUND = WIDTH;
    private double rPeak = 0;
    private double gPeak = MIDDLE;
    private double bPeak = WIDTH;
    private double darkness = 1;
    private int compensatorStep = 500;
    private int msSleepPerStep = 1000 / compensatorStep;
    private double amplitude = WIDTH / 3;
    private Function<Double, Double> compensatorFunction = makeDefaultCompensatorFunction();
    private int maxFPS = 120;

    public AboutChunk(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
        setSelfNode(makeNode());
        updateLocaleContent();
        showItself();
    }

    /**
     * Updates gui local dependent elements to current local settings.
     */
    @Override
    public void updateLocaleContent() {
        gitHubLink.setText(resProvider.getStrFromMessagesBundle("gitHubLink"));
        mailToLink.setText(resProvider.getStrFromMessagesBundle("emailMe"));
        nameYearLabel.setText(resProvider.getStrFromMessagesBundle("nameYear"));
    }

    public void shutDownAnimation() {
        running = false;
        for (Thread thread : threads) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void showItself() {
        Scene aboutScene = new Scene((Parent) getAsNode(), WIDTH, HEIGHT);
        aboutScene.setOnMouseClicked(event -> attractLights(event.getSceneX(), event.getSceneY()));

        Stage aboutStage = new Stage();
        aboutStage.setScene(aboutScene);
        aboutStage.setResizable(false);
        aboutStage.sizeToScene();
        aboutStage.setOnCloseRequest(event -> chunkManager.terminateAboutChunk());
        aboutStage.setAlwaysOnTop(true);

        aboutStage.show();
    }
    private Node makeNode() {
        makeThreadHandler();

        Effect blur = new GaussianBlur(WIDTH / 100);
        rainbowImageView.setEffect(blur);
        Node background = new Label("", rainbowImageView);
        Node overlay = makeOverlayNode();

        return new StackPane(background, overlay);
    }
    private Node makeOverlayNode() {
        BorderPane result = new BorderPane();

        Text title = new Text("Unduplicator");
        title.setStyle("-fx-opacity: 0.7; -fx-font: 50px Monospace; -fx-fill: WHITE");

        gitHubLink.setBorder(Border.EMPTY);
        gitHubLink.setPadding(new Insets(0));
        gitHubLink.setOnAction(event -> {
            try {
                URI gitHubURI = new URL("https://github.com/MontolioV/Unduplicator").toURI();
                linkFromDesktop(gitHubURI);
            } catch (URISyntaxException | MalformedURLException e) {
                e.printStackTrace();
            }
        });

        mailToLink.setBorder(Border.EMPTY);
        mailToLink.setPadding(new Insets(0));
        mailToLink.setOnAction(event -> {
            try {
                linkFromDesktop(new URI("mailto:mail@gmail.com"));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        });

        HBox hBox = new HBox(30, gitHubLink, mailToLink);
        VBox vBox = new VBox(10, hBox, nameYearLabel);
        hBox.setAlignment(Pos.CENTER);
        vBox.setAlignment(Pos.CENTER);

        result.setCenter(title);
        result.setBottom(vBox);

        result.setPadding(new Insets(20));
        return result;
    }
    private void linkFromDesktop(URI uri) {
        Thread thread = new Thread(() -> {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null) {
                try {
                    desktop.browse(uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void makeThreadHandler() {
        Thread thread = new Thread(() -> {
            makeImgGeneratorThread();

            changeImageOverTime();
        });

        threads.add(thread);
        thread.setDaemon(true);
        thread.start();
    }
    private void makeImgGeneratorThread() {
        Thread imgGeneratorThread = new Thread(() -> {
            final int HALF_WIDTH = WIDTH / 2;
            int r, g, b;
            int maxDelay = 1000 / maxFPS;
            long msPerFrame = 1;

            while (running) {
                long startTime = System.currentTimeMillis();
                WritableImage rainbowImage = new WritableImage(WIDTH, HEIGHT);
                PixelWriter pixelWriter = rainbowImage.getPixelWriter();

                for (int y = 0; y < HEIGHT; y++) {
                    double fY = y * darkness;
                    for (int x = 0; x < WIDTH; x++) {
                        double fX = x;

                        Function<Double, Double> scales = peak -> {
                            double scalesX = 1 - (Math.abs(fX - peak) / (HALF_WIDTH));
                            double scalesY = 1 - (fY / HEIGHT);
                            if (scalesX < 0) scalesX = 0;
                            if (scalesY < 0) scalesY = 0;
                            return scalesX * scalesY;
                        };

                        r = (int) (255 * scales.apply(rPeak));
                        g = (int) (255 * scales.apply(gPeak));
                        b = (int) (255 * scales.apply(bPeak));

                        int pixel = (255 << 24) | (r << 16) | (g << 8) | b;
                        pixelWriter.setArgb(x, y, pixel);
                    }
                }

                rainbowImageView.setImage(rainbowImage);
                fpsRepresentation.setText(String.valueOf(1000 / Math.max(maxDelay, msPerFrame)));

                msPerFrame = System.currentTimeMillis() - startTime;
                long delay = maxDelay - msPerFrame;
                if (delay > 0) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        threads.add(imgGeneratorThread);
        imgGeneratorThread.setDaemon(true);
        imgGeneratorThread.start();
    }

    private void changeImageOverTime() {
        double rCompensator = 0;
        double gCompensator = 0;
        double bCompensator = 0;

        while (running) {
            try {
                Thread.sleep(msSleepPerStep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            rCompensator = compensatorFunction.apply(rPeak);
            gCompensator = compensatorFunction.apply(gPeak);
            bCompensator = compensatorFunction.apply(bPeak);

            rPeak += randomRange() + rCompensator;
            gPeak += randomRange() + gCompensator;
            bPeak += randomRange() + bCompensator;
        }
    }
    private double randomRange() {
        return (RANDOM_LOWER_BOUND + (Math.random() * (RANDOM_UPPER_BOUND - RANDOM_LOWER_BOUND))) / (WIDTH * 4);
    }
    private Function<Double,Double> makeDefaultCompensatorFunction () {
        BiFunction<Double, Double, Double> relativeFactorFunction = (mainPeak, otherPeak) -> {
            double difference = Math.max(mainPeak, otherPeak) - Math.min(mainPeak, otherPeak);
            double modifiedDifference = (difference - amplitude) / 2;
            return mainPeak > otherPeak ? -modifiedDifference : modifiedDifference;
        };
        Function<Double, Double> boundFactorFunction = mainPeak -> (MIDDLE - mainPeak) / 10;

        Function<Double,Double> defaultCompensatorFunction = peak -> {
            double closestPeak = 0;
            if (peak == rPeak) {
                closestPeak = Math.abs(rPeak - gPeak) < Math.abs(rPeak - bPeak) ? gPeak : bPeak;
            } else if (peak == gPeak) {
                closestPeak = Math.abs(gPeak - rPeak) < Math.abs(gPeak - bPeak) ? rPeak : bPeak;
            } else if (peak == bPeak) {
                closestPeak = Math.abs(bPeak - rPeak) < Math.abs(bPeak - gPeak) ? rPeak : gPeak;
            }
            double compensation = relativeFactorFunction.apply(peak, closestPeak) + boundFactorFunction.apply(peak);
            return compensation / compensatorStep;
        };

        return defaultCompensatorFunction;
    }
    private void attractLights(double x, double y) {
        if (interactionThread != null) {
            interactionThread.interrupt();
            try {
                interactionThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            threads.remove(interactionThread);
        }

        interactionThread = new Thread(() -> {
            Function<Double, Double> oldFunction = compensatorFunction;
            compensatorFunction = peak -> (x - peak) / compensatorStep;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            compensatorFunction = oldFunction;
        });

        interactionThread.setDaemon(true);
        threads.add(interactionThread);
        interactionThread.start();
    }
}
