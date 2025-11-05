package lk.ijse.weatherbroadcastsystem.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.*;
import java.net.Socket;

public class WeatherFormController {

    @FXML private TextField txtCity;
    @FXML private TextArea txtAreaChat;
    @FXML private Label lblStatus;
    @FXML private Button btnConnect;
    @FXML private Button btnDisconnect;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Thread readThread;

    private final String HOST = "localhost";
    private final int PORT = 4000;

    @FXML
    public void initialize() {
        txtAreaChat.setText("");
    }

    @FXML
    private void btnConnectOnAction() {
        try {
            socket = new Socket(HOST, PORT);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            lblStatus.setText("Status: Connected to " + HOST + ":" + PORT);
            btnConnect.setDisable(true);
            btnDisconnect.setDisable(false);

            // optionally send subscription (city) to server - not implemented server-side now
            String city = txtCity.getText().trim();
            if (!city.isEmpty()) {
                dos.writeUTF("SUBSCRIBE:" + city);
                dos.flush();
            }

            startReaderThread();

        } catch (IOException e) {
            showError("Connect failed: " + e.getMessage());
        }
    }

    private void startReaderThread() {
        readThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    String update = dis.readUTF(); // blocks
                    Platform.runLater(() -> txtAreaChat.appendText(update + "\n"));
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Status: Disconnected");
                    btnConnect.setDisable(false);
                    btnDisconnect.setDisable(true);
                    txtAreaChat.appendText("[Connection closed]\n");
                });
            }
        });
        readThread.setDaemon(true);
        readThread.start();
    }

    @FXML
    private void btnDisconnectOnAction() {
        try {
            if (readThread != null) readThread.interrupt();
            if (dos != null) dos.writeUTF("UNSUBSCRIBE"); // optional
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        } finally {
            lblStatus.setText("Status: Disconnected");
            btnConnect.setDisable(false);
            btnDisconnect.setDisable(true);
        }
    }

    @FXML
    private void btnClearOnAction() {
        txtAreaChat.clear();
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            a.showAndWait();
        });
    }
}
