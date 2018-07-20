package ru.roboticsnt.photoMaker;


import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;
import ru.roboticsnt.ipCameras.PTZCameraController;
import ru.roboticsnt.opencv.Camera;
import ru.roboticsnt.opencv.OpencvUtils;
import ru.roboticsnt.photoMaker.gui.GUIController;
import ru.roboticsnt.utils.FileSystemUtils;


public class CameraController
{

    private Camera _camera;

    private boolean _isCameraStarted = false;
    private boolean _isMakePhoto = false;

    //Video
    private boolean _isVideoMode = false;
    private boolean _isRecord = false;
    private VideoWriter _videoWriter = null;

    private int _nextImageIndex = 0;
    private int _nextVideoIndex = 0;
    private String _folderPath;
    private String _fileName;
    private Size _frameSize;

    public static final String DEFAULT_IMAGES_PATH = "/home/user/Изображения/DataSets/temp";
    public static final String DEFAULT_CAMERA_IP = "192.168.3.106";
    public static final int DEFAULT_IMAGE_SIZE = 1024;

    private GUIController _guiController;

    public PTZCameraController ptz;


    public CameraController(GUIController guiController)
    {
        _guiController = guiController;
    }


    public void startCamera()
    {
        _isCameraStarted = !_isCameraStarted;

        if(_isCameraStarted)
        {
            _guiController.loadOpenCV();

            String cameraIP = _guiController.getCameraIP();

            try
            {
                int cameraIndex = Integer.parseInt(cameraIP);

                _camera = new Camera(cameraIndex);

            }catch (NumberFormatException exception)
            {
                _camera = Camera.create(cameraIP);
            }

            if(_camera.isOpened())
            {
                _guiController.setStartCameraViewSettings();
                ptz = new PTZCameraController(_camera, PTZCameraController.FALCON_EYE_CAMERA_PTZ_REQUEST_PATH);
            }else
            {
                System.out.println("Camera (" + cameraIP + ") connection failed. Check IP address");
                stopCamera();
                return;
            }

            new Thread(() ->
            {
                Mat frame;
                Mat displayFrame;
                Mat photo = null;

                while (_isCameraStarted)
                {
                    if (!_camera.isOpened())
                    {
                        System.out.println("Camera connection lost");
                        stopCamera();
                        return;
                    }

                    frame = _camera.getNextFrame();

                    if(!frame.empty())
                    {
                        displayFrame = frame.clone();

                        _guiController.showCamera(displayFrame);

                        //video mode
                        if(_isVideoMode)
                        {
                            if(_isRecord)
                            {
                                if(!_videoWriter.isOpened())
                                {
                                    _videoWriter.open(_folderPath + "/" +  _fileName, VideoWriter.fourcc('X', 'V', 'I', 'D'), 15, _frameSize, true);
                                }

                                if(_videoWriter != null && _videoWriter.isOpened())
                                {
                                    photo = OpencvUtils.cropImage(frame, _frameSize);
                                    _videoWriter.write(photo.clone());
                                    _guiController.showPhoto(photo);
                                }
                            }
                        //photo mode
                        }else
                        {
                            if(_isMakePhoto)
                            {
                                _fileName = getPhotoNameByIndex(_nextImageIndex);
                                _nextImageIndex++;

                                photo = OpencvUtils.cropImage(frame, _frameSize);
                                OpencvUtils.saveFrame(photo, _folderPath, _fileName);
                                _guiController.showFileName(_fileName);
                                _guiController.showPhoto(photo);
                                _guiController.showFilesCount(_nextImageIndex);

                                _isMakePhoto = false;
                            }
                        }

                        frame.release();
                        displayFrame.release();

                        if(photo != null)
                        {
                            photo.release();
                        }
                    }

                    try
                    {
                        Thread.sleep(33);
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }).start();
        }else
        {
//            _guiController.setStopCameraViewSettings();
//            _camera.release();
            stopCamera();
        }
    }


    private void stopCamera()
    {
        System.out.println("STOP CAMERA");
        _isCameraStarted = false;

        if(_camera != null)
        {
            _camera.release();
        }

        stopRecord();
        _guiController.setStopCameraViewSettings();
    }


    public void makePhoto()
    {
        if(_isCameraStarted)
        {
            int frameWidth = Integer.parseInt(_guiController.getFrameWidth());
            int frameHeight = Integer.parseInt(_guiController.getFrameHeight());
            _frameSize = new Size(frameWidth, frameHeight);
            _folderPath = _guiController.getFolderPath();

            if (_isVideoMode)
            {
                if(_isRecord)
                {
                    stopRecord();
                }else
                {
                    startRecord();
                }
            }else
            {
                _isMakePhoto = true;
            }
        }
    }


    public void setVideoMode(boolean value)
    {
        if(_isRecord)
        {
            stopRecord();
        }

        _isVideoMode = value;

        if(_isVideoMode)
        {
            _guiController.setMakePhotoButtonText("Start record");
        }else
        {
            _guiController.resetRecordTimer();
            _guiController.setMakePhotoButtonText("Make photo");
        }

        updateFilesCount();
    }


    public void updateFilesCount()
    {
        _folderPath = _guiController.getFolderPath();

        try
        {
            _nextImageIndex = FileSystemUtils.directoryFilesCount(_folderPath, FileSystemUtils.FILE_TYPE_JPEG);
            _nextVideoIndex = FileSystemUtils.directoryFilesCount(_folderPath, FileSystemUtils.FILE_TYPE_AVI);

        }catch (Error error)
        {
            System.out.println(error.getMessage());
        }


        int filesCount;

        if(_isVideoMode)
        {
            filesCount = _nextVideoIndex;

            if(filesCount > 0)
            {
                int videoIndex = _nextVideoIndex - 1;
                _guiController.showVideoFirstFrame(videoIndex);
            }
        }else
        {
            filesCount = _nextImageIndex;

            if(filesCount > 0)
            {
                int imgIndex = _nextImageIndex - 1;
                _guiController.showPhoto(imgIndex);
            }
        }
        _guiController.showFilesCount(filesCount);
    }


    public boolean isVideoMode()
    {
        return _isVideoMode;
    }


    public boolean isRecord()
    {
        return _isRecord;
    }


    public boolean isCameraStarted()
    {
        return _isCameraStarted;
    }


    public int getPhotosCount()
    {
        return _nextImageIndex;
    }


    public int getVideosCount()
    {
        return _nextVideoIndex;
    }


    public String getFolderPath()
    {
        return _folderPath;
    }


    public String getPhotoNameByIndex(int index)
    {
        return "img_" + index + ".jpeg";
    }


    public String getVideoNameByIndex(int index)
    {
        return "video_" + index + ".avi";
    }


    public void release()
    {
//        _isCameraStarted = false;
//        stopRecord();
        stopCamera();
    }


    private void startRecord()
    {
        _fileName = getVideoNameByIndex(_nextVideoIndex);
        _videoWriter = new VideoWriter();
        _isRecord = true;
        _guiController.showFileName(_fileName);
        _guiController.setMakePhotoButtonText("Stop record");
        _guiController.startRecordTimer();
    }


    private void stopRecord()
    {
        _isRecord = false;

        if(_videoWriter != null && _videoWriter.isOpened())
        {
            _videoWriter.release();
            _videoWriter = null;
            _nextVideoIndex++;
            _guiController.showFilesCount(_nextVideoIndex);
            _guiController.setMakePhotoButtonText("Start record");
            _guiController.stopRecordTimer();
        }
    }

}
