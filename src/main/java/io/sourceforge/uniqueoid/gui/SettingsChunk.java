package io.sourceforge.uniqueoid.gui;

import io.sourceforge.uniqueoid.GlobalFiles;
import io.sourceforge.uniqueoid.ResourcesProvider;
import io.sourceforge.uniqueoid.logic.FindTaskSettings;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * <p>Created by MontolioV on 31.10.17.
 */
public class SettingsChunk extends AbstractGUIChunk{
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private ChunkManager chunkManager;

    private File saveFile = GlobalFiles.getInstance().getTasksSettingsFile();
    private FindTaskSettings findTaskSettings;

    private Supplier<Long> minFileSizeSupp;
    private Supplier<Long> maxFileSizeSupp;
    private Supplier<Long> bigFileSizeSupp;
    private Supplier<Integer> maxHashBufferSizeSupp;

    private CheckBox isDisposableChBox = new CheckBox();
    private CheckBox isParallelChBox = new CheckBox();
    private Spinner<Integer> parallelismSpinner;

    private Button saveButton = new Button();
    private Button cancelButton = new Button();
    private Button defaultButton = new Button();

    private Label algorithmLabel = new Label();
    private Label fileSizeRestrictionsLabel = new Label();
    private Label minSizeLabel = new Label();
    private Label maxSizeLabel = new Label();
    private Label bigFileSizeLabel = new Label();
    private Label maxBufferSizeLabel = new Label();

    private ObservableList<BytePower> bytePowers = FXCollections.observableArrayList(BytePower.values());

    public SettingsChunk(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
        makeChunk();
    }

    private void makeChunk() {
        load();
        setSelfNode(makeSelfNode());
        updateLocaleContent();
    }

    /**
     * Updates gui local dependent elements to current local settings.
     */
    @Override
    public void updateLocaleContent() {
        isDisposableChBox.setText(resProvider.getStrFromGUIBundle("isDisposableChBox"));
        isParallelChBox.setText(resProvider.getStrFromGUIBundle("parallelismChBox"));
        bytePowers.forEach(BytePower::updateLocaleContent);
        fileSizeRestrictionsLabel.setText(resProvider.getStrFromGUIBundle("fileSizeRestrictions"));
        minSizeLabel.setText(resProvider.getStrFromGUIBundle("minSizeLabel"));
        maxSizeLabel.setText(resProvider.getStrFromGUIBundle("maxSizeLabel"));
        bigFileSizeLabel.setText(resProvider.getStrFromGUIBundle("bigFileSizeLabel"));
        maxBufferSizeLabel.setText(resProvider.getStrFromGUIBundle("maxBufferSizeLabel"));
        saveButton.setText(resProvider.getStrFromGUIBundle("saveButton"));
        cancelButton.setText(resProvider.getStrFromGUIBundle("cancelButton"));
        defaultButton.setText(resProvider.getStrFromGUIBundle("defaultButton"));
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
            chunkManager.showException(e);
            makeDefault();
        }
    }
    private void makeDefault() {
        findTaskSettings = new FindTaskSettings();
        save();
    }

    protected FindTaskSettings getFindTaskSettings() {
        return findTaskSettings;
    }

    private Node makeSelfNode() {
        Node bottomNode = makeBottomNode();
        AnchorPane anchorPane = new AnchorPane(bottomNode);
        AnchorPane.setBottomAnchor(bottomNode, 0D);
        AnchorPane.setLeftAnchor(bottomNode, 0D);
        AnchorPane.setRightAnchor(bottomNode, 0D);
        VBox.setVgrow(anchorPane, Priority.ALWAYS);

        VBox vBox = new VBox(5,
                makeAlgorithmAndParallelismSettingsNode(),
                makeBigFileAndBufferSettingsNode(),
                makeFileDelimiterSettingsNode(),
                anchorPane);
        vBox.setPadding(new Insets(0, 20, 0, 0));

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(vBox);
        borderPane.setRight(new Separator(Orientation.VERTICAL));
        borderPane.setPadding(new Insets(20, 0, 20, 20));
        borderPane.setMinWidth(0D);
        return borderPane;
    }

    private Node makeAlgorithmAndParallelismSettingsNode() {
        isDisposableChBox.setSelected(findTaskSettings.isDisposable());
        isDisposableChBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        HBox.setHgrow(isDisposableChBox, Priority.ALWAYS);

        isParallelChBox.setSelected(findTaskSettings.isParallel());
        isParallelChBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        HBox.setHgrow(isParallelChBox, Priority.ALWAYS);

        parallelismSpinner = new Spinner<>(1, 1024, findTaskSettings.getParallelism());
        parallelismSpinner.getEditor().setPrefColumnCount(5);

        HBox parallelHBox = new HBox(5, isParallelChBox, parallelismSpinner);
        VBox result = new VBox(5,
                isDisposableChBox,
                parallelHBox);
        return result;
    }
    private Node makeBigFileAndBufferSettingsNode() {
        NumberFormat numberFormat = new DecimalFormat();

        ComboBox<BytePower> bigFileSizeCB = new ComboBox<>(bytePowers);
        ComboBox<BytePower> maxBufferSizeCB = new ComboBox<>(bytePowers);
        TextField bigFileTF = new TextField();
        TextField maxBufferTF = new TextField();

        Node bigFileSizeNode = makeByteInputElementsNode(bigFileTF, bigFileSizeCB, bigFileSizeLabel);
        Node maxBufferSizeNode = makeByteInputElementsNode(maxBufferTF, maxBufferSizeCB, maxBufferSizeLabel);

        bigFileSizeSupp = () -> {
            try {
                return (long) (numberFormat.parse(bigFileTF.getText()).doubleValue() * bigFileSizeCB.getSelectionModel().getSelectedItem().getModifier());
            } catch (ParseException e) {
                String message = resProvider.getStrFromExceptionBundle("illegalBigFileSize");
                chunkManager.showException(new IllegalArgumentException(message, e));
                return 10 * BytePower.MI_BYTES.getModifier();
            }
        };
        maxHashBufferSizeSupp = () -> {
            try {
                return (int) (numberFormat.parse(maxBufferTF.getText()).doubleValue() * maxBufferSizeCB.getSelectionModel().getSelectedItem().getModifier());
            } catch (ParseException e) {
                String message = resProvider.getStrFromExceptionBundle("illegalHashBufferSize");
                chunkManager.showException(new IllegalArgumentException(message, e));
                return (int) (10 * BytePower.MI_BYTES.getModifier());
            }
        };

        bindByteInputElements_0ToLongMax(bigFileTF, bigFileSizeCB);
        bindByteInputElements_0ToIntMax(maxBufferTF, maxBufferSizeCB);

        bigFileTF.setText(String.valueOf(findTaskSettings.getBigFileSize()));
        maxBufferTF.setText(String.valueOf(findTaskSettings.getMaxBufferSize()));

        VBox result = new VBox(5, bigFileSizeNode, maxBufferSizeNode);
        return result;
    }
    private Node makeFileDelimiterSettingsNode() {
        NumberFormat numberFormat = new DecimalFormat();

        ComboBox<BytePower> minMeasureCB = new ComboBox<>(bytePowers);
        ComboBox<BytePower> maxMeasureCB = new ComboBox<>(bytePowers);
        TextField minSizeTF = new TextField();
        TextField maxSizeTF = new TextField();

        Node minNode = makeByteInputElementsNode(minSizeTF, minMeasureCB, minSizeLabel);
        Node maxNode = makeByteInputElementsNode(maxSizeTF, maxMeasureCB, maxSizeLabel);

        minFileSizeSupp = () -> {
            try {
                return (long) (numberFormat.parse(minSizeTF.getText()).doubleValue() * minMeasureCB.getSelectionModel().getSelectedItem().getModifier());
            } catch (ParseException e) {
                String message = resProvider.getStrFromExceptionBundle("illegalMinFileSize");
                chunkManager.showException(new IllegalArgumentException(message, e));
                return 0L;
            }
        };
        maxFileSizeSupp = () -> {
            try {
                return (long) (numberFormat.parse(maxSizeTF.getText()).doubleValue() * maxMeasureCB.getSelectionModel().getSelectedItem().getModifier());
            } catch (ParseException e) {
                String message = resProvider.getStrFromExceptionBundle("illegalMaxFileSize");
                chunkManager.showException(new IllegalArgumentException(message, e));
                return Long.MAX_VALUE;
            }
        };

        bindByteInputElements_0ToLongMax(minSizeTF, minMeasureCB);
        bindByteInputElements_0ToLongMax(maxSizeTF, maxMeasureCB);

        minSizeTF.setText(String.valueOf(findTaskSettings.getMinFileSize()));
        maxSizeTF.setText(String.valueOf(findTaskSettings.getMaxFileSize()));

        VBox minMaxVBox = new VBox(5, minNode, maxNode);
        return minMaxVBox;
    }
    private Node makeBottomNode() {
        saveButton.setOnAction(event -> {
            findTaskSettings = new FindTaskSettings(
                    isDisposableChBox.isSelected(),
                    isParallelChBox.isSelected(),
                    parallelismSpinner.getValue(),
                    bigFileSizeSupp.get(),
                    maxHashBufferSizeSupp.get(),
                    maxFileSizeSupp.get(),
                    minFileSizeSupp.get());
            save();
            changeChunckAndRefresh();
        });
        cancelButton.setOnAction(event -> changeChunckAndRefresh());
        defaultButton.setOnAction(event -> {
            findTaskSettings = new FindTaskSettings();
            save();
            makeChunk();
            chunkManager.showSettingsNode();
        });

        saveButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cancelButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        defaultButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        VBox vBox = new VBox(10,
                saveButton,
                cancelButton,
                defaultButton);

        return vBox;
    }

    private Node makeByteInputElementsNode(TextField textField, ComboBox<BytePower> comboBox, Label label) {
        comboBox.getSelectionModel().select(BytePower.BYTES);
        comboBox.setMaxHeight(Double.MAX_VALUE);
        textField.setPrefWidth(100);
        HBox.setHgrow(textField, Priority.ALWAYS);
        HBox hBox = new HBox(0, textField, comboBox);
        VBox vBox = new VBox(3, label, hBox);
        return vBox;
    }
    private void bindByteInputElements(TextField textFieldToBind, ComboBox<BytePower> comboBoxToBind, ToDoubleFunction<Double> limitFunc) {
        NumberFormat numberFormat = new DecimalFormat();
        Function<TextField, ChangeListener<BytePower>> comboBoxListenerProvider = textField -> (observable, oldValue, newValue) -> {
            try {
                double tfValue = numberFormat.parse(textField.getText()).doubleValue();
                textField.setText(String.format("%,f", tfValue * ((double) oldValue.getModifier() / newValue.getModifier())));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        };
        Function<ComboBox<BytePower>, ChangeListener<String>> textFieldListenerProvider = comboBox -> (observable, oldValue, newValue) -> {
            StringProperty stringProperty = (StringProperty) observable;

            try {
                double d = limitFunc.applyAsDouble(numberFormat.parse(newValue).doubleValue());
                String formattedValue = String.format("%,f", d);
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

        textFieldToBind.textProperty().addListener(textFieldListenerProvider.apply(comboBoxToBind));
        comboBoxToBind.valueProperty().addListener(comboBoxListenerProvider.apply(textFieldToBind));
    }
    private void bindByteInputElements_0ToLongMax(TextField textFieldToBind, ComboBox<BytePower> comboBoxToBind) {
        ToDoubleFunction<Double> limitFunc = d -> {
            double maxValue = ((double) Long.MAX_VALUE) / comboBoxToBind.valueProperty().get().getModifier();
            if (d > maxValue) {
                return maxValue;
            } else if (d < 0) {
                return 0;
            } else {
                return d;
            }
        };
        bindByteInputElements(textFieldToBind, comboBoxToBind, limitFunc);
    }
    private void bindByteInputElements_0ToIntMax(TextField textFieldToBind, ComboBox<BytePower> comboBoxToBind) {
        ToDoubleFunction<Double> limitFunc = d -> {
            double maxValue = ((double) Integer.MAX_VALUE) / comboBoxToBind.valueProperty().get().getModifier();
            if (d > maxValue) {
                return maxValue;
            } else if (d < 0) {
                return 0;
            } else {
                return d;
            }
        };
        bindByteInputElements(textFieldToBind, comboBoxToBind, limitFunc);
    }

    private void changeChunckAndRefresh() {
        chunkManager.hideSettingsNode();
        Platform.runLater(this::makeChunk);
    }
}
