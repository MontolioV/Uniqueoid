package com.unduplicator.gui;

import com.unduplicator.ResourcesProvider;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * <p>Created by MontolioV on 30.08.17.
 */
public class GUI extends Application{
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private BorderPane appPane = new BorderPane();
    private Stage primaryStage;
    private ChunkManager chunkManager;

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * The main entry point for all JavaFX applications.
     * The start method is called after the init method has returned,
     * and after the system is ready for the application to begin running.
     * <p>
     * <p>
     * NOTE: This method is called on the JavaFX Application Thread.
     * </p>
     *
     * @param primaryStage the primary stage for this application, onto which
     *                     the application scene can be set. The primary stage will be embedded in
     *                     the browser if the application was launched as an applet.
     *                     Applications may create other stages, if needed, but they will not be
     *                     primary stages and will not be embedded in the browser.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        chunkManager = new ChunkManager(this);
        Scene mainScene = new Scene(appPane, 700, 600);
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    protected void setCenterNode(Node node) {
        appPane.setCenter(node);
    }

    protected void setTopNode(Node node) {
        appPane.setTop(node);
    }

    protected void showException(Exception ex) {
        ex.printStackTrace();

        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));

        Platform.runLater(() -> {
            Alert exAlert = new Alert(Alert.AlertType.ERROR);
            exAlert.setHeaderText(resProvider.getStrFromGUIBundle("exceptionAlertHeader") + "\n" + ex.toString());
            exAlert.getDialogPane().setExpandableContent(new TextArea(stringWriter.toString()));
            resizeAlertManually(exAlert);

            exAlert.showAndWait();
        });
    }

    protected void resizeAlertManually(Alert alert) {
        //Got a bug with stage resizing, must resize manually
        alert.getDialogPane().expandedProperty().addListener(observable -> {
            Platform.runLater(() -> {
                alert.getDialogPane().requestLayout();
                Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                stage.sizeToScene();
            });
        });
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    protected boolean isNodeShownInCenter(Node node) {
        return appPane.getCenter().equals(node);
    }
}
