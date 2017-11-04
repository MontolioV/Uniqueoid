package io.sourceforge.uniqueoid.gui;

import io.sourceforge.uniqueoid.GlobalFiles;
import io.sourceforge.uniqueoid.ResourcesProvider;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.*;

/**
 * <p>Created by MontolioV on 30.08.17.
 */
public class GUI extends Application{
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private BorderPane appPane = new BorderPane();
    private Stage primaryStage;
    private ChunkManager chunkManager;

    private Scene mainScene;
    private double width = 700;
    private double height = 600;
    private boolean isFullScreen = false;

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * The application initialization method. This method is called immediately
     * after the Application class is loaded and constructed. An application may
     * override this method to perform initialization prior to the actual starting
     * of the application.
     * <p>
     * <p>
     * The implementation of this method provided by the Application class does nothing.
     * </p>
     * <p>
     * <p>
     * NOTE: This method is not called on the JavaFX Application Thread. An
     * application must not construct a Scene or a Stage in this
     * method.
     * An application may construct other JavaFX objects in this method.
     * </p>
     */
    @Override
    public void init() throws Exception {
        super.init();

        double tmpWidth = 0;
        double tmpHeight = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(GlobalFiles.getInstance().getSettingsFile()))) {
            tmpHeight = Double.parseDouble(br.readLine().split("=")[1]);
            tmpWidth = Double.parseDouble(br.readLine().split("=")[1]);
            isFullScreen = Boolean.parseBoolean(br.readLine().split("=")[1]);
            GlobalFiles.getInstance().setLastVisitedDir(br.readLine().split("=")[1]);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        if (tmpHeight > 100) height = tmpHeight;
        if (tmpWidth > 100) width = tmpWidth;
    }

    /**
     * This method is called when the application should stop, and provides a
     * convenient place to prepare for application exit and destroy resources.
     * <p>
     * <p>
     * The implementation of this method provided by the Application class does nothing.
     * </p>
     * <p>
     * <p>
     * NOTE: This method is called on the JavaFX Application Thread.
     * </p>
     */
    @Override
    public void stop() throws Exception {
        super.stop();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(GlobalFiles.getInstance().getSettingsFile()))) {
            bw.write("height=" + String.valueOf(mainScene.getHeight()));
            bw.newLine();
            bw.write("width=" + String.valueOf(mainScene.getWidth()));
            bw.newLine();
            bw.write("fullscreen=" + String.valueOf(getPrimaryStage().isFullScreen()));
            bw.newLine();
            bw.write("lastVisitedDir=" + GlobalFiles.getInstance().getLastVisitedDir().toString());
            bw.newLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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
        mainScene = new Scene(appPane, width, height);
        primaryStage.setScene(mainScene);
        primaryStage.setTitle("Uniqueoid");
        primaryStage.setFullScreen(isFullScreen);
        primaryStage.show();
    }

    protected void setCenterNode(Node node) {
        appPane.setCenter(node);
    }
    protected void setTopNode(Node node) {
        appPane.setTop(node);
    }
    protected void setBottomNode(Node node) {
        appPane.setBottom(node);
    }
    protected void setLeftNode(Node node) {
        appPane.setLeft(node);
    }
    protected void setRightNode(Node node) {
        appPane.setRight(node);
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

    protected Stage getPrimaryStage() {
        return primaryStage;
    }

    protected boolean isNodeShownInCenter(Node node) {
        return appPane.getCenter().equals(node);
    }

    protected void showInNewStage(Parent parent) {
        Scene scene = new Scene(parent);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();
    }
}
