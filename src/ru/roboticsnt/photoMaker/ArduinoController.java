package ru.roboticsnt.photoMaker;

import ru.roboticsnt.commandProtocol.ProtocolCommand;
import ru.roboticsnt.commandProtocol.commands.ArduinoCommand;
import ru.roboticsnt.commandProtocol.connections.ArduinoProtocolConnection;
import ru.roboticsnt.commandProtocol.connections.ProtocolConnectionListener;
import ru.roboticsnt.photoMaker.gui.GUIController;
import ru.roboticsnt.serial.SerialPortManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class ArduinoController
{

    private GUIController _guiController;
    private CameraController _cameraController;

    private final int _MAKE_PHOTO_PIN  = 12;
    private final int _ADD_ZOOM_PIN  = 11;
    private final int _REDUCE_ZOOM_PIN  = 10;
    private final int _ADD_FOCUS_PIN  = 9;
    private final int _REDUCE_FOCUS_PIN  = 8;
    private final int _ADD_DIAPHRAGM_PIN  = 7;
    private final int _REDUCE_DIAPHRAGM_PIN  = 6;

    private final int _UPDATE_PORTS_PERIOD = 1500;

    private Timer _timer;

    private ArduinoProtocolConnection _connection;

    private final int _MIN_INTERVAL_BETWEEN_SIGNALS = 220;//milliseconds

    private Map<Integer, Long> pinUpdatesList = new HashMap();


    public ArduinoController(GUIController controller, CameraController cameraController)
    {
        _guiController = controller;
        _cameraController = cameraController;

        startUpdatePortsTimer();
    }


    private void startUpdatePortsTimer()
    {
        stopUpdatePortsTimer();

        _timer = new Timer();

        TimerTask task = new TimerTask()
        {
            @Override
            public void run()
            {
                _guiController.setPorts(SerialPortManager.getPortNames());
            }
        };

        _timer.schedule(task, 0, _UPDATE_PORTS_PERIOD);
    }


    public void stopUpdatePortsTimer()
    {
        if(_timer != null)
        {
            _timer.cancel();
            _timer = null;
        }
    }


    public void closeConnection()
    {
        if(_connection != null)
        {
            _connection.close();
            _connection = null;
        }
    }


    public void connect(String port)
    {
        if(_connection != null)
        {
            closeConnection();
        }

        _guiController.showArduinoStatus(false, "Try connect to " + port + "...");

        _connection = new ArduinoProtocolConnection(port);

        _connection.addEventListener(new ProtocolConnectionListener()
        {
            @Override
            public void onConnect()
            {
                super.onConnect();

                stopUpdatePortsTimer();

                _guiController.showArduinoStatus(true, "Arduino connected");

                addPinListener(_MAKE_PHOTO_PIN);
                addPinListener(_ADD_ZOOM_PIN);
                addPinListener(_REDUCE_ZOOM_PIN);
                addPinListener(_ADD_FOCUS_PIN);
                addPinListener(_REDUCE_FOCUS_PIN);
                addPinListener(_ADD_DIAPHRAGM_PIN);
                addPinListener(_REDUCE_DIAPHRAGM_PIN);
            }

            @Override
            public void onCommand(ProtocolCommand command)
            {
                if(command.getType() == ArduinoCommand.TYPE_ARDUINO_DIGITAL_PIN_VALUE)
                {
                    int pin = command.getData()[0];
                    int value = command.getData()[1];

                    Long currentTime = System.currentTimeMillis();

                    if(pinUpdatesList.containsKey(pin))
                    {
                        Long lastPinChangeTime = pinUpdatesList.get(pin);

                        if((currentTime - lastPinChangeTime) < _MIN_INTERVAL_BETWEEN_SIGNALS)
                        {
                            return;
                        }
                    }

                    pinUpdatesList.put(pin, currentTime);

                    if(value == 0)
                    {
                        switch (pin)
                        {
                            case _MAKE_PHOTO_PIN:
                                _cameraController.makePhoto();
                                break;

                            case _ADD_ZOOM_PIN:
                                if(_cameraController.isCameraStarted())
                                {
                                        _cameraController.ptz.addZoom();
                                }
                                break;

                            case _REDUCE_ZOOM_PIN:
                                if(_cameraController.isCameraStarted())
                                {
                                    _cameraController.ptz.reduceZoom();
                                }
                                break;

                            case _ADD_FOCUS_PIN:
                                if(_cameraController.isCameraStarted())
                                {
                                    _cameraController.ptz.addFocus();
                                }
                                break;

                            case _REDUCE_FOCUS_PIN:
                                if(_cameraController.isCameraStarted())
                                {
                                    _cameraController.ptz.reduceFocus();
                                }
                                break;

                            case _ADD_DIAPHRAGM_PIN:
                                if(_cameraController.isCameraStarted())
                                {
                                    _cameraController.ptz.addDiaphragm();
                                }
                                break;

                            case _REDUCE_DIAPHRAGM_PIN:
                                if(_cameraController.isCameraStarted())
                                {
                                    _cameraController.ptz.reduceDiaphragm();
                                }
                                break;

                            default:
                                System.out.println("Pin № " + pin + " not listen.");
                                break;
                        }
                    }
                }
            }

            @Override
            public void onError(Exception exception)
            {
                super.onError(exception);
                _guiController.showArduinoStatus(false, "Arduino connection error");

                startUpdatePortsTimer();
            }

            @Override
            public void onDisconnect()
            {
                super.onDisconnect();
                _guiController.showArduinoStatus(false,"Arduino connection cancel");

                startUpdatePortsTimer();
            }

        });

        _connection.connect();
    }


    public boolean isConnected()
    {
        return (_connection != null) && _connection.isConnected();
    }


    private void addPinListener(int pin)
    {
        _connection.setPinMode(pin, ArduinoProtocolConnection.PIN_MODE_INPUT_PULLUP);
        _connection.addDigitalPinListener((byte) pin);
    }

}
