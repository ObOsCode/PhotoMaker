package ru.roboticsnt.photoMaker.gui;

        import com.sun.deploy.util.ArrayUtil;
        import javafx.application.Platform;

        import javafx.event.ActionEvent;
        import javafx.fxml.FXML;
        import javafx.scene.control.*;
        import javafx.scene.image.ImageView;
        import javafx.scene.input.KeyEvent;
        import javafx.scene.layout.AnchorPane;
        import javafx.stage.DirectoryChooser;
        import javafx.stage.FileChooser;
        import org.opencv.core.*;
        import ru.roboticsnt.opencv.OpencvUtils;
        import ru.roboticsnt.opencv.gui.jfx.OpencvJFXProjectGUIControllerBase;
        import ru.roboticsnt.photoMaker.ArduinoController;
        import ru.roboticsnt.photoMaker.CameraController;
        import ru.roboticsnt.utils.FileSystemUtils;

        import java.io.File;
        import java.net.URL;
        import java.util.Optional;
        import java.util.ResourceBundle;
        import java.util.Timer;
        import java.util.TimerTask;


public class GUIController extends OpencvJFXProjectGUIControllerBase
{
    @FXML
    private TextField _cameraIpTf;
    @FXML
    private TextField _imagesPathTf;
    @FXML
    private TextField _imageWidthTf;
    @FXML
    private TextField _imageHeightTf;
    @FXML
    private Button _startCameraBut;
    @FXML
    private Button _makePhotoButton;
    @FXML
    private Label _filesCountTf;
    @FXML
    private ImageView _cameraImageView;
    @FXML
    private ImageView _screenshotImageView;
    @FXML
    private AnchorPane _cameraViewContainer;
    @FXML
    private ComboBox<String> _portsComBox;
    @FXML
    private Button _connectArduinoBut;
    @FXML
    private Label _arduinoStatusLabel;
    @FXML
    private Button _reduceZoomBut;
    @FXML
    private Button _addZoomBut;
    @FXML
    private Button _reduceFocusBut;
    @FXML
    private Button _addFocusBut;
    @FXML
    private Button _reduceDiaphragmBut;
    @FXML
    private Button _addDiaphragmBut;
    @FXML
    private Slider _sensitivitySlider;
    @FXML
    private Slider _brightnessSlider;
    @FXML
    private Slider _contrastSlider;
    @FXML
    private AnchorPane _vtzPanel;
    @FXML
    private RadioButton _videoModeRadioBut;
    @FXML
    private RadioButton _photoModeRadioBut;
    @FXML
    private Label _timerLabel;
    @FXML
    private Label _fileNameLabel;
    @FXML
    private Button _nextBut;
    @FXML
    private Button _prevBut;
    @FXML
    private TextField _opencvPathTf;


    private ArduinoController _arduinoController;
    private CameraController _cameraController;
    private Timer _recordTimer;

    private int _photoIndex;
    private int _videoIndex;


    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        super.initialize(location, resources);

        _makePhotoButton.setDisable(true);
        _imagesPathTf.setText(CameraController.DEFAULT_IMAGES_PATH);
        _cameraIpTf.setText(CameraController.DEFAULT_CAMERA_IP);
        _imageWidthTf.setText(Integer.toString(CameraController.DEFAULT_IMAGE_SIZE));
        _imageHeightTf.setText(Integer.toString(CameraController.DEFAULT_IMAGE_SIZE));
        _cameraImageView.fitWidthProperty().bind(_cameraViewContainer.widthProperty());
        _cameraImageView.fitHeightProperty().bind(_cameraViewContainer.heightProperty());

        _sensitivitySlider.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            int newSensitivity = newValue.intValue();
            _cameraController.ptz.setSpeed(newSensitivity);
            _sensitivitySlider.setValue(newSensitivity);
        });

        _brightnessSlider.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            int newBrightness = newValue.intValue();
            _cameraController.ptz.setBrightness(newBrightness);
        });

        _contrastSlider.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            int newContrast = newValue.intValue();
            _cameraController.ptz.setContrast(newContrast);
        });

        ToggleGroup switchModeGroup = new ToggleGroup();

        _videoModeRadioBut.setToggleGroup(switchModeGroup);
        _photoModeRadioBut.setToggleGroup(switchModeGroup);

        switchModeGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) ->
        {
            if(newValue == _videoModeRadioBut)
            {
                _cameraController.setVideoMode(true);
            }else if(newValue == _photoModeRadioBut)
            {
                _cameraController.setVideoMode(false);
            }
        });

        _opencvPathTf.setText(getDefaultOpenCVPath());

        showArduinoStatus(false, "Arduino not connected");
    }


    public void loadOpenCV()
    {
        String path = _opencvPathTf.getText();
        super.loadOpenCV(path);
    }


    public void setArduinoController(ArduinoController controller)
    {
        _arduinoController = controller;
    }


    public void setCameraController(CameraController controller)
    {
        _cameraController = controller;
        _cameraController.updateFilesCount();
    }


    public String getCameraIP()
    {
        return _cameraIpTf.getText();
    }


    public String getFolderPath()
    {
        return _imagesPathTf.getText();
    }


    public String getFrameWidth()
    {
        return _imageWidthTf.getText();
    }


    public String getFrameHeight()
    {
        return _imageHeightTf.getText();
    }


    public void setPorts(String[] ports)
    {
//        System.out.println("Ports " + ArrayUtil.arrayToString(ports));

        String selectedPort = _portsComBox.getValue();

        Platform.runLater(() -> _portsComBox.getItems().clear());

        if(ports.length <= 0)
        {
            _connectArduinoBut.setDisable(true);
            _portsComBox.setDisable(true);
            return;
        }

        _portsComBox.setDisable(false);
        _connectArduinoBut.setDisable(false);

        Platform.runLater(() ->
        {
            for (String port : ports)
            {
                _portsComBox.getItems().add(port);

                if(port.equals(selectedPort))
                {
                    _portsComBox.setValue(port);
                }
            }

            if(_portsComBox.getValue() == null)
            {
                _portsComBox.setValue(ports[0]);
            }
        });
    }


    public void showArduinoStatus(boolean isConnect, String consoleMessage)
    {
        System.out.println("ARDUINO STATUS: " + consoleMessage);

        Platform.runLater(() ->
        {
            if(isConnect)
            {
                _connectArduinoBut.setText("Cancel");
                _portsComBox.setDisable(true);
            }else
            {
                _connectArduinoBut.setText("Connect");
                _portsComBox.setDisable(false);
            }

            _arduinoStatusLabel.setText(consoleMessage);
        });
    }


    public void showFileName(String name)
    {
        Platform.runLater(() -> _fileNameLabel.setText(name));

    }


    public void showFilesCount(int value)
    {
        if(value <= 0)
        {
            showFileName("No files");
        }else
        {
            if(_cameraController.isVideoMode())
            {
                _videoIndex = value - 1;
                String fileName = _cameraController.getVideoNameByIndex(_videoIndex);
                showFileName(fileName);
            }else
            {
                _photoIndex = value - 1;
                String fileName = _cameraController.getPhotoNameByIndex(_photoIndex);
                showFileName(fileName);
            }
        }

        if(value > 1)
        {
            _prevBut.setDisable(false);
        }else
        {
            _prevBut.setDisable(true);
        }

        _nextBut.setDisable(true);

        Platform.runLater(() -> _filesCountTf.setText(Integer.toString(value)));
    }


    public void showCamera(Mat frame)
    {
        int width = Integer.parseInt(_imageWidthTf.getText());
        int height = Integer.parseInt(_imageHeightTf.getText());

        OpencvUtils.drawRectangleOnCenter(frame, width, height);

        showFrame(frame, _cameraImageView);
    }


    public void showPhoto(Mat frame)
    {
        showFrame(frame, _screenshotImageView);
    }


    public void showPhoto(String path)
    {
        showImage(path, _screenshotImageView);
    }


    public void showPhoto(int index)
    {
        _photoIndex = index;
        String fileName = _cameraController.getPhotoNameByIndex(_photoIndex);
        String filePath = _imagesPathTf.getText() + "/" + fileName;
        showPhoto(filePath);
    }


    public void showVideoFirstFrame(int index)
    {
        String videoPath = _imagesPathTf.getText() + "/" + _cameraController.getVideoNameByIndex(index);
        Mat videoFirstFrame = OpencvUtils.getVideoFirstFrame(videoPath);
        showPhoto(videoFirstFrame);
    }


    public void setStartCameraViewSettings()
    {
        _makePhotoButton.setDisable(false);
        _cameraImageView.setVisible(true);
        _startCameraBut.setText("Stop");
        _vtzPanel.setDisable(false);
    }


    public void setStopCameraViewSettings()
    {
        _cameraImageView.setVisible(false);
        _makePhotoButton.setDisable(true);
        _startCameraBut.setText("Start");
        _vtzPanel.setDisable(true);
    }


    public void setMakePhotoButtonText(String text)
    {
        Platform.runLater(() -> _makePhotoButton.setText(text));
    }


    public void startRecordTimer()
    {
        _timerLabel.setDisable(false);
        _recordTimer = new Timer();
        _recordTimer.scheduleAtFixedRate(new RecordTimerTask(), 0, 1000);
    }


    public void stopRecordTimer()
    {
        _recordTimer.cancel();
        _recordTimer = null;
    }


    public void resetRecordTimer()
    {
        _timerLabel.setDisable(true);
        _timerLabel.setText(RecordTimerTask.DEFAULT_TIMER_TEXT);
    }


    //                              GUI EVENTS


    @FXML
    private void onKeyPress(KeyEvent event)
    {
//        System.out.println("onKeyPress");
    }


    @FXML
    private void onKeyReleased(KeyEvent event)
    {
        if(!_cameraController.isCameraStarted())
        {
            return;
        }

        switch (event.getCode())
        {
            case ENTER:
                System.out.println("Enter key press");
                break;

            case LEFT:
                System.out.println("Left key press");
                break;

            default:
                System.out.println("Other key press");

        }
    }


    @FXML
    private void onKeyTyped(KeyEvent event)
    {
//        System.out.println("onKeyTyped");
    }


    @FXML
    private void onPrevButtonPress(ActionEvent event)
    {
        int index;
        String fileName;

        if(_cameraController.isVideoMode())
        {
            if(_cameraController.isRecord())
            {
                return;
            }
            _videoIndex -= 1;
            fileName = _cameraController.getVideoNameByIndex(_videoIndex);
            showVideoFirstFrame(_videoIndex);
            index = _videoIndex;
        }else
        {
            _photoIndex -= 1;
            fileName = _cameraController.getPhotoNameByIndex(_photoIndex);
            showPhoto(_photoIndex);
            index = _photoIndex;
        }

        showFileName(fileName);

        if(index - 1 < 0)
        {
            _prevBut.setDisable(true);
        }
        _nextBut.setDisable(false);

    }


    @FXML
    private void onNextButtonPress(ActionEvent event)
    {
        int index;
        String fileName;
        int filesCount;

        if(_cameraController.isVideoMode())
        {
            if(_cameraController.isRecord())
            {
                return;
            }
            _videoIndex += 1;
            fileName = _cameraController.getVideoNameByIndex(_videoIndex);
            filesCount = _cameraController.getVideosCount();
            showVideoFirstFrame(_videoIndex);
            index = _videoIndex;
        }else
        {
            _photoIndex += 1;
            fileName = _cameraController.getPhotoNameByIndex(_photoIndex);
            filesCount = _cameraController.getPhotosCount();
            showPhoto(_photoIndex);
            index = _photoIndex;
        }

        showFileName(fileName);

        if(index + 1 >= filesCount)
        {
            _nextBut.setDisable(true);
        }
        _prevBut.setDisable(false);
    }


    @FXML
    private void onStartCameraButPress(ActionEvent actionEvent)
    {
//        super.loadOpenCV();
        _cameraController.startCamera();
    }


    @FXML
    private void onMakePhotoButPress(ActionEvent actionEvent)
    {
        if(_cameraController.isVideoMode())
        {
            _nextBut.setDisable(true);
            _prevBut.setDisable(true);
        }
        _cameraController.makePhoto();
    }


    @FXML
    private void onBrowseImagesFolderButPress(ActionEvent actionEvent)
    {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File initDirectory = new File(CameraController.DEFAULT_IMAGES_PATH);
        directoryChooser.setInitialDirectory(initDirectory);
        File selectedDirectory = directoryChooser.showDialog(_imagesPathTf.getScene().getWindow());

        if(selectedDirectory != null)
        {
            String path = selectedDirectory.getAbsolutePath();
            _imagesPathTf.setText(path);

            _cameraController.updateFilesCount();
        }
    }


    @FXML
    private void onBrowseOpenCVFolderButPress(ActionEvent event)
    {
        FileChooser fileChooser = new FileChooser();
        File initDirectory = new File(super.getDefaultOpenCVPath()).getParentFile();
        fileChooser.setInitialDirectory(initDirectory);
        File selectedFile = fileChooser.showOpenDialog(_opencvPathTf.getScene().getWindow());

        if(selectedFile != null)
        {
            _opencvPathTf.setText(selectedFile.getAbsolutePath());
        }
    }


    @FXML
    private void onClearFolderButPress(ActionEvent actionEvent)
    {
        String path = _imagesPathTf.getText();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeight(500);
        alert.setTitle("Attention!!!");
        alert.setHeaderText("Confirm delete");
        alert.setContentText("Would you like delete all files from " + path + "?");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.get() == ButtonType.OK)
        {
            FileSystemUtils.clearDirectory(path, false);

            _cameraController.updateFilesCount();
        }
    }


    @FXML
    private void onOpenImagesFolderButPress(ActionEvent event)
    {
        FileSystemUtils.openFolder(_cameraController.getFolderPath());
    }


    @FXML
    private void onArduinoConnectButPress(ActionEvent actionEvent)
    {
        if(_arduinoController.isConnected())
        {
            _arduinoController.closeConnection();
        }else
        {
            _arduinoController.connect(_portsComBox.getValue());
        }
    }

    ///////////////////////////////////
    ////////////    PTZ     ///////////
    ///////////////////////////////////

    @FXML
    private void onReduceZoomButPress(ActionEvent event)
    {
        _cameraController.ptz.reduceZoom();
    }


    @FXML
    private void onAddZoomButPress(ActionEvent event)
    {
        _cameraController.ptz.addZoom();
    }


    @FXML
    private void onReduceFocusButPress(ActionEvent event)
    {
        _cameraController.ptz.reduceFocus();
    }


    @FXML
    private void onAddFocusButPress(ActionEvent event)
    {
        _cameraController.ptz.addFocus();
    }


    @FXML
    private void onReduceDiaphragmButPress(ActionEvent event)
    {
        _cameraController.ptz.reduceDiaphragm();
    }


    @FXML
    private void onAddDiaphragmButPress(ActionEvent event)
    {
        _cameraController.ptz.addDiaphragm();
    }


//                              Classes

    private class RecordTimerTask extends TimerTask
    {
        public static final String DEFAULT_TIMER_TEXT = "00:00:00";
        private int _time = 0;

        @Override
        public void run()
        {
            Platform.runLater(() ->
            {
                int hours = _time / 3600;
                int minutes = (_time - hours * 3600) / 60;
                int seconds = _time - hours * 3600 - minutes * 60;

                _timerLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                _time++;
            });

        }
    }

}

