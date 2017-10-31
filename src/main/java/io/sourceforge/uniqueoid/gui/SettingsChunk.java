package io.sourceforge.uniqueoid.gui;

import io.sourceforge.uniqueoid.GlobalFiles;
import io.sourceforge.uniqueoid.ResourcesProvider;
import io.sourceforge.uniqueoid.logic.FindTaskSettings;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.*;
import java.util.function.Function;

/**
 * <p>Created by MontolioV on 31.10.17.
 */
public class SettingsChunk extends AbstractGUIChunk{
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private ChunkManager chunkManager;

    private File saveFile = GlobalFiles.getInstance().getTasksSettingsFile();
    private FindTaskSettings findTaskSettings;

    public SettingsChunk(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
        load();
        setSelfNode();
    }

    /**
     * Updates gui local dependent elements to current local settings.
     */
    @Override
    public void updateLocaleContent() {

    }

    private void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveFile))){
            oos.writeObject(findTaskSettings);
        } catch (IOException e) {
            chunkManager.showException(e);
        }
    }
    private void load() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveFile))) {
            findTaskSettings = (FindTaskSettings) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            makeDefault();
        }
    }
    private void makeDefault() {
        findTaskSettings = new FindTaskSettings();
    }

    protected FindTaskSettings getFindTaskSettings() {
        return findTaskSettings;
    }

    private Node makeSelfNode() {
        ListView<String> listView = new ListView<>();
        listView.
    }

    private Node makeFileDelimiterNode() {
        BytePower minClosest = findClosestGrage(findTaskSettings.getMinFileSize());
        BytePower maxClosest = findClosestGrage(findTaskSettings.getMaxFileSize());
        double minInitial = (findTaskSettings.getMinFileSize() / (minClosest.getModifier() * 100d)) / 100;
        double maxInitial = (findTaskSettings.getMaxFileSize() / (maxClosest.getModifier() * 100d)) / 100;

        ObservableList<BytePower> bytePowers = FXCollections.observableArrayList(BytePower.values());
        ComboBox<BytePower> minMeasureCB = new ComboBox<>(bytePowers);
        ComboBox<BytePower> maxMeasureCB = new ComboBox<>(bytePowers);
        TextField minSizeTF = new TextField();
        TextField maxSizeTF = new TextField();

        Function<TextField, ChangeListener<BytePower>> comboBoxListenerProvider = textField -> (observable, oldValue, newValue) -> {
            double bytes = Double.parseDouble(textField.getText()) * oldValue.getModifier();
            textField.setText(String.format("%.2f", bytes / newValue.getModifier()));
        };
        Function<ComboBox<BytePower>, ChangeListener<String>> textFieldListenerProvider = comboBox -> (observable, oldValue, newValue) -> {
            TextField tf = (TextField) observable;
            try {
                double d = Double.parseDouble(newValue);
                if (d > (Long.MAX_VALUE / comboBox.valueProperty().get().getModifier())) {
                    d = Long.MAX_VALUE / comboBox.valueProperty().get().getModifier();
                } else if (d < 0) {
                    d = 0;
                }
                tf.setText(String.format("%.2f", d));
            } catch (NumberFormatException e) {
                tf.setText(oldValue);
            }
        };

        minSizeTF.textProperty().addListener(textFieldListenerProvider.apply(minMeasureCB));
        maxSizeTF.textProperty().addListener(textFieldListenerProvider.apply(maxMeasureCB));

        minMeasureCB.getSelectionModel().select(minClosest);
        maxMeasureCB.getSelectionModel().select(maxClosest);
        minMeasureCB.valueProperty().addListener(comboBoxListenerProvider.apply(minSizeTF));
        maxMeasureCB.valueProperty().addListener(comboBoxListenerProvider.apply(maxSizeTF));


        VBox result = new VBox();
        return result;
    }



    private BytePower findClosestGrage(long value) {
        BytePower[] bytePowers = BytePower.values();
        for (int i = bytePowers.length - 1; i > 0; i--) {
            if ((value / bytePowers[i].getModifier()) > 0) {
                return bytePowers[i];
            }
        }
        return bytePowers[0];
    }
}
