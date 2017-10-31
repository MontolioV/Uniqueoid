package io.sourceforge.uniqueoid.gui;

import io.sourceforge.uniqueoid.GlobalFiles;
import io.sourceforge.uniqueoid.ResourcesProvider;
import io.sourceforge.uniqueoid.logic.FindTaskSettings;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.function.Function;

/**
 * <p>Created by MontolioV on 31.10.17.
 */
public class SettingsChunk extends AbstractGUIChunk{
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private ChunkManager chunkManager;

    private File saveFile = GlobalFiles.getInstance().getTasksSettingsFile();
    private FindTaskSettings findTaskSettings;

    private ObservableList<BytePower> bytePowers = FXCollections.observableArrayList(BytePower.values());

    private Label fileSizeRestrictionsLabel = new Label();
    private Label minSizeLabel = new Label();
    private Label maxSizeLabel = new Label();

    public SettingsChunk(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
        load();
        setSelfNode(makeSelfNode());
    }

    /**
     * Updates gui local dependent elements to current local settings.
     */
    @Override
    public void updateLocaleContent() {
        bytePowers.forEach(BytePower::updateLocaleContent);
        fileSizeRestrictionsLabel.setText(resProvider.getStrFromGUIBundle("fileSizeRestrictions"));
        minSizeLabel.setText(resProvider.getStrFromGUIBundle("minSizeLabel"));
        maxSizeLabel.setText(resProvider.getStrFromGUIBundle("maxSizeLabel"));
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
        return makeFileDelimiterNode();
    }

    private Node makeFileDelimiterNode() {
        NumberFormat numberFormat = new DecimalFormat();

        BytePower minClosest = findClosestGrade(findTaskSettings.getMinFileSize());
        BytePower maxClosest = findClosestGrade(findTaskSettings.getMaxFileSize());

        ComboBox<BytePower> minMeasureCB = new ComboBox<>(bytePowers);
        ComboBox<BytePower> maxMeasureCB = new ComboBox<>(bytePowers);
        TextField minSizeTF = new TextField();
        TextField maxSizeTF = new TextField();

        Function<TextField, ChangeListener<BytePower>> comboBoxListenerProvider = textField -> (observable, oldValue, newValue) -> {
            try {
                double tfValue = numberFormat.parse(textField.getText()).doubleValue();
                textField.setText(String.format("%,.2f", tfValue * ((double) oldValue.getModifier() / newValue.getModifier())));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        };
        Function<ComboBox<BytePower>, ChangeListener<String>> textFieldListenerProvider = comboBox -> (observable, oldValue, newValue) -> {
            StringProperty stringProperty = (StringProperty) observable;

            try {
                double d = numberFormat.parse(newValue).doubleValue();
                if (d > (Long.MAX_VALUE / comboBox.valueProperty().get().getModifier())) {
                    d = Long.MAX_VALUE / comboBox.valueProperty().get().getModifier();
                } else if (d < 0) {
                    d = 0;
                }
                String formattedValue = String.format("%,.2f", d);
                if (d == (long) d) {
                    formattedValue = String.format("%,d", (long) d);
                }
                if (!newValue.equals(formattedValue)) {
                    stringProperty.setValue(formattedValue);
                }
            } catch (ParseException e) {
                e.printStackTrace();
                stringProperty.setValue(oldValue);
            }
        };

        minSizeTF.textProperty().addListener(textFieldListenerProvider.apply(minMeasureCB));
        maxSizeTF.textProperty().addListener(textFieldListenerProvider.apply(maxMeasureCB));

        minMeasureCB.getSelectionModel().select(minClosest);
        maxMeasureCB.getSelectionModel().select(maxClosest);
        minMeasureCB.valueProperty().addListener(comboBoxListenerProvider.apply(minSizeTF));
        maxMeasureCB.valueProperty().addListener(comboBoxListenerProvider.apply(maxSizeTF));

        minSizeTF.setText(String.valueOf(findTaskSettings.getMinFileSize()));
        maxSizeTF.setText(String.valueOf(findTaskSettings.getMaxFileSize()));

        VBox labelsVBox = new VBox(5, minSizeLabel, maxSizeLabel);
        VBox tfVBox = new VBox(5, minSizeTF, maxSizeTF);
        VBox cbVBox = new VBox(5, minMeasureCB, maxMeasureCB);
        HBox hBox = new HBox(10, labelsVBox, tfVBox, cbVBox);
        VBox result = new VBox(10, fileSizeRestrictionsLabel, hBox);
        return result;
    }

    private BytePower findClosestGrade(long value) {
        BytePower[] bytePowers = BytePower.values();
        for (int i = bytePowers.length - 1; i > 0; i--) {
            if ((value / bytePowers[i].getModifier()) > 0) {
                return bytePowers[i];
            }
        }
        return bytePowers[0];
    }
}
