package org.example.eventdrivenprogrammingproject1;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server extends Application {
    private static final int PORT = 291;
    private static final Map<String, List<Lecture>> schedule = Collections.synchronizedMap(new HashMap<>());
    private TextArea logArea;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        logArea = new TextArea();
        logArea.setEditable(false);

        BorderPane root = new BorderPane(logArea);
        Scene scene = new Scene(root, 600, 400);

        primaryStage.setTitle("Lecture Scheduler Server");
        primaryStage.setScene(scene);
        primaryStage.show();

        new Thread(this::startServer).start();
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log("Server started on port " + PORT + ". Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                log("Client connected from: " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket, schedule, this::log).start();
            }
        } catch (IOException e) {
            log("Error: " + e.getMessage());
        }
    }

    private void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }
}
