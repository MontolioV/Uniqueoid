package com.unduplicator.gui;

import com.unduplicator.ResourcesProvider;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * <p>Created by MontolioV on 29.09.17.
 */
public class AboutChunk extends AbstractGUIChunk {
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private ChunkManager chunkManager;

    private ArrayList<Thread> threads = new ArrayList<>();
    private boolean running = true;

    private ConcurrentLinkedQueue<Image> imgQueue = new ConcurrentLinkedQueue<>();
    private ImageView rainbowImageView = new ImageView();

    private final int WIDTH = 700;
    private final int HEIGHT = 500;
    private final int UPPER_BOUND = WIDTH / 100;
    private final int LOWER_BOUND = WIDTH / -100;
    private final int BOTTOM = 0 + UPPER_BOUND;
    private final int MIDDLE = WIDTH / 2;
    private final int TOP = WIDTH - UPPER_BOUND;
    private double rPeak = UPPER_BOUND;
    private double gPeak = MIDDLE;
    private double bPeak = WIDTH - UPPER_BOUND;
    private double darkness = 1;
    private double compensatorDivider = 10000;
    private double amplitude = UPPER_BOUND - LOWER_BOUND / 4;
    private Function<Double, Double> compensatorFunction = makeDefaultCompensatorFunction();

    public AboutChunk(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
        setSelfNode(makeNode());
        showItself();
    }

    /**
     * Updates gui local dependent elements to current local settings.
     */
    @Override
    public void updateLocaleContent() {

    }

    public void attractLights(int x, int y) {

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
        Stage aboutStage = new Stage();
        aboutStage.setScene(aboutScene);
        aboutStage.setResizable(false);
        aboutStage.sizeToScene();
        aboutStage.setOnCloseRequest(event -> chunkManager.terminateAboutChunk());

        aboutStage.show();
    }

    private Node makeNode() {
        makeThreadHandler();
        return new Label("", rainbowImageView);
    }

    private void makeThreadHandler() {
        Thread thread = new Thread(() -> {
            try (PipedInputStream pis = new PipedInputStream();
                 BufferedInputStream bis = new BufferedInputStream(pis);
                 PipedOutputStream pos = new PipedOutputStream(pis);
                 BufferedOutputStream bos = new BufferedOutputStream(pos))
            {
                makeImgGeneratorThread(bos);
                makeImgCollectorThread(bis);
                makeImgShowThread();

                changeImageOverTime();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        threads.add(thread);
        thread.setDaemon(true);
        thread.start();
    }

    private void makeImgGeneratorThread(OutputStream outputStream) {
        Thread imgGeneratorThread = new Thread(() -> {
            while (running) {
                int r, g, b;
                BufferedImage rainbowBuffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

                for (int x = 0; x < WIDTH; x++) {
                    for (int y = 0; y < HEIGHT; y++) {
                        double fX = x;
                        double fY = y * darkness;

                        Function<Double, Double> scales = peak -> {
                            double scalesX = 1 - (Math.abs(fX - peak) / (WIDTH / 2));
                            double scalesY = 1 - (fY / HEIGHT);
                            if (scalesX < 0) scalesX = 0;
                            if (scalesY < 0) scalesY = 0;
                            return scalesX * scalesY;
                        };

                        r = (int) (255 * scales.apply(rPeak));
                        g = (int) (255 * scales.apply(gPeak));
                        b = (int) (255 * scales.apply(bPeak));

                        int pixel = (255 << 24) | (r << 16) | (g << 8) | b;
                        rainbowBuffer.setRGB(x, y, pixel);
                    }
                }

                try {
                    ImageIO.write(rainbowBuffer, "jpg", outputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        threads.add(imgGeneratorThread);
        imgGeneratorThread.setDaemon(true);
        imgGeneratorThread.start();
    }

    private void makeImgCollectorThread(InputStream inputStream) {
        Thread imgCollectorThread = new Thread(() -> {
            while (running) {
                if (imgQueue.size() > 100) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                imgQueue.add(new Image(inputStream));
            }
        });

        threads.add(imgCollectorThread);
        imgCollectorThread.setDaemon(true);
        imgCollectorThread.start();
    }

    private void makeImgShowThread() {
        Thread imgShowThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!imgQueue.isEmpty()) {
                    rainbowImageView.setImage(imgQueue.poll());
                }
            }
        });

        threads.add(imgShowThread);
        imgShowThread.setDaemon(true);
        imgShowThread.start();
    }

    private void changeImageOverTime() {
        double rCompensator = 0;
        double gCompensator = 0;
        double bCompensator = 0;

        while (running) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            rCompensator += compensatorFunction.apply(rPeak);
            gCompensator += compensatorFunction.apply(gPeak);
            bCompensator += compensatorFunction.apply(bPeak);

            rPeak += randomRange() + rCompensator;
            gPeak += randomRange() + gCompensator;
            bPeak += randomRange() + bCompensator;
        }
    }

    private double randomRange() {
        return (LOWER_BOUND + (Math.random() * (UPPER_BOUND - LOWER_BOUND))) / 8;
    }

    private Function<Double,Double> makeDefaultCompensatorFunction () {
        BiFunction<Double, Double, Double> relativeFactorFunction = (mainPeak, otherPeak) -> {
            double difference = Math.max(mainPeak, otherPeak) - Math.min(mainPeak, otherPeak);
            double modifiedDifference = amplitude - difference;
            return mainPeak > otherPeak ? modifiedDifference : -modifiedDifference;
        };
        Function<Double, Double> boundFactorFunction = mainPeak -> (MIDDLE - mainPeak) / 10;

        Function<Double,Double> defaultCompensatorFunction = peak -> {
            double closestPeak = 0;
            if (peak == rPeak) {
                closestPeak = Math.min(Math.abs(rPeak - gPeak), Math.abs(rPeak - bPeak));
            } else if (peak == gPeak) {
                closestPeak = Math.min(Math.abs(gPeak - rPeak), Math.abs(gPeak - bPeak));
            } else if (peak == bPeak) {
                closestPeak = Math.min(Math.abs(bPeak - rPeak), Math.abs(bPeak - gPeak));
            }
            double compensation = relativeFactorFunction.apply(peak, closestPeak) + boundFactorFunction.apply(peak);
            return compensation / compensatorDivider;
        };

        return defaultCompensatorFunction;
    }

}
