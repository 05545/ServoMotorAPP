package com.example.servomotor;

/*
HECHO POR RODRIGO SOSA ROMERO
GITHUB: https://github.com/05545
 */

/*
Este archivo es el controlador de bluetooth, que se va a encargar del puerto y de la conexión
Es importante que antes de compilar este archivo o la apliación entera se haga la sincronización
con las dependencias.
 */

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothManager {
    private static final String TAG = "BluetoothManager";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String ESP32_NAME = "ServoController_ESP32";

    // Request codes para permisos
    public static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice esp32Device;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Thread readThread;
    private boolean isConnected = false;

    private BluetoothListener listener;
    private Handler mainHandler;

    public interface BluetoothListener {
        void onConnectionChanged(boolean connected);
        void onDataReceived(String data);
        void onError(String error);
        void onPermissionRequired(); // callback para solicitar permisos. Es de suma importancia el que se manejen correctamente los posibles errores en tiempo de ejecución.
    }

    public BluetoothManager(Context context, BluetoothListener listener) {
        this.context = context;
        this.listener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean isBluetoothAvailable() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        if (bluetoothAdapter == null) return false;

        // Verificar permisos antes de acceder
        if (!hasBluetoothPermissions()) {
            return false;
        }

        try {
            return bluetoothAdapter.isEnabled();
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException al verificar Bluetooth: " + e.getMessage());
            return false;
        }
    }

    // Verificar nuevamente si se cuentan con los permisos necesarios
    public boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                            == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN)
                            == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;
        }
    }

    // Solicitar permisos necesarios, en caso de no tenerlos
    public void requestBluetoothPermissions() {
        if (!(context instanceof Activity)) {
            notifyError("Se requiere Activity para solicitar permisos");
            return;
        }

        Activity activity = (Activity) context;
        String[] permissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }

        ActivityCompat.requestPermissions(activity, permissions, REQUEST_BLUETOOTH_PERMISSIONS);
    }

    public boolean findAndConnectToESP32() {
        // Verificar permisos primero, medida preventiva de un colapso de la apliacación
        if (!hasBluetoothPermissions()) {
            if (listener != null) {
                listener.onPermissionRequired();
            }
            return false;
        }

        if (!isBluetoothEnabled()) {
            notifyError("Bluetooth no está habilitado");
            return false;
        }

        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                if (ESP32_NAME.equals(deviceName)) {
                    esp32Device = device;
                    break;
                }
            }

            if (esp32Device == null) {
                notifyError("ESP32 no encontrado. Asegúrate de que esté emparejado.");
                return false;
            }

            // Conectar en un hilo separado, mantiene el control de sockets
            new Thread(this::connectToDevice).start();
            return true;

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage());
            notifyError("Permisos de Bluetooth insuficientes");
            if (listener != null) {
                listener.onPermissionRequired();
            }
            return false;
        }
    }

    private void connectToDevice() {
        if (!hasBluetoothPermissions()) {
            notifyError("Permisos de Bluetooth no otorgados");
            return;
        }

        try {
            bluetoothSocket = esp32Device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();

            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            isConnected = true;

            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onConnectionChanged(true);
                }
            });

            startReadThread();
            Log.d(TAG, "Conectado al ESP32");

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException al conectar: " + e.getMessage());
            notifyError("Permisos insuficientes para conectar");
            if (listener != null) {
                mainHandler.post(() -> listener.onPermissionRequired());
            }
        } catch (IOException e) {
            Log.e(TAG, "Error al conectar: " + e.getMessage());
            notifyError("Error al conectar: " + e.getMessage());
            disconnect();
        }
    }

    private void startReadThread() {
        readThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            while (isConnected) {
                try {
                    bytes = inputStream.read(buffer);
                    String receivedData = new String(buffer, 0, bytes).trim();

                    mainHandler.post(() -> {
                        if (listener != null) {
                            listener.onDataReceived(receivedData);
                        }
                    });

                } catch (IOException e) {
                    if (isConnected) {
                        Log.e(TAG, "Error al leer datos: " + e.getMessage());
                        notifyError("Conexión perdida");
                        disconnect();
                    }
                    break;
                }
            }
        });
        readThread.start();
    }

    public void sendAngle(int angle) {
        if (!isConnected || outputStream == null) {
            notifyError("No hay conexión Bluetooth");
            return;
        }

        new Thread(() -> {
            try {
                String command = "ANGLE:" + angle + "\n";
                outputStream.write(command.getBytes());
                outputStream.flush();
                Log.d(TAG, "Enviado: " + command.trim());
            } catch (IOException e) {
                Log.e(TAG, "Error al enviar datos: " + e.getMessage());
                notifyError("Error al enviar datos");
            }
        }).start();
    }

    public void disconnect() {
        isConnected = false;

        if (readThread != null) {
            readThread.interrupt();
        }

        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error al cerrar conexión: " + e.getMessage());
        }

        mainHandler.post(() -> {
            if (listener != null) {
                listener.onConnectionChanged(false);
            }
        });

        Log.d(TAG, "Desconectado del ESP32");
    }

    public boolean isConnected() {
        return isConnected;
    }

    private void notifyError(String error) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onError(error);
            }
        });
    }

    // Se maneja la solicitud de permisos
    public void onPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                Log.d(TAG, "Permisos de Bluetooth otorgados");
            } else {
                notifyError("Permisos de Bluetooth denegados. La aplicación no puede funcionar sin ellos.");
            }
        }
    }
}