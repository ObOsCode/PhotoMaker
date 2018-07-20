package ru.roboticsnt.photoMaker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.roboticsnt.photoMaker.gui.GUIController;

public class Launcher extends Application
{

    private GUIController _guiController;

    private ArduinoController _arduinoController;

    private CameraController _cameraController;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("gui/sample.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Photo maker");
        primaryStage.setScene(new Scene(root, 800, 630));
        primaryStage.show();

        _guiController = loader.getController();
        _cameraController = new CameraController(_guiController);
        _arduinoController = new ArduinoController(_guiController, _cameraController);
        _guiController.setArduinoController(_arduinoController);
        _guiController.setCameraController(_cameraController);
    }


    @Override
    public void stop() throws Exception
    {
        System.out.println("STOP APPLICATION");
        _cameraController.release();
        _arduinoController.closeConnection();
        _arduinoController.stopUpdatePortsTimer();
        super.stop();
    }


    public static void main(String[] args)
    {
        launch(args);
    }
}
