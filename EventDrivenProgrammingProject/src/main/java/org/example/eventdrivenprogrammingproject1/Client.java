package org.example.eventdrivenprogrammingproject1;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.util.*;

public class Client extends Application {
    private TextArea responseArea;
    private ComboBox<String> actionBox;
    private TextField moduleField;
    private ComboBox<String> dateBox, startBox, endBox, roomBox;
    private Button sendButton, stopButton, viewTimetableButton, earlyLecturesButton;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ToggleButton darkModeToggle;
    private boolean isDarkMode = false;
    private Scene scene;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Lecture Scheduler Client");
        VBox root = createLayout();
        scene = new Scene(root, 350, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 291);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            responseArea.appendText("Connected to server\n");
        } catch (IOException e) {
            responseArea.appendText("Failed to connect to server: " + e.getMessage() + "\n");
        }
    }

    private VBox createLayout() {
        actionBox = new ComboBox<>();
        actionBox.getItems().addAll("Add Lecture", "Remove Lecture");
        actionBox.setPromptText("Select Action");

        moduleField = new TextField();
        moduleField.setPromptText("Module Name");

        dateBox = new ComboBox<>();
        dateBox.setPromptText("Day");
        dateBox.getItems().addAll("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");

        startBox = new ComboBox<>();
        startBox.setPromptText("Start Time");
        startBox.getItems().addAll("09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00");

        endBox = new ComboBox<>();
        endBox.setPromptText("End Time");
        endBox.getItems().addAll("10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00");

        roomBox = new ComboBox<>();
        roomBox.setPromptText("Room");
        roomBox.getItems().addAll("Room A", "Room B", "Room C", "Room D", "Room E", "Room F");

        sendButton = new Button("Send Request");
        stopButton = new Button("STOP");
        viewTimetableButton = new Button("View Timetable");
        earlyLecturesButton = new Button("Request Early Lectures");

        responseArea = new TextArea();
        responseArea.setEditable(false);
        responseArea.setPromptText("Server response will appear here...");

        darkModeToggle = new ToggleButton("Dark Mode");
        darkModeToggle.setOnAction(e -> toggleDarkMode());

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getChildren().addAll(
                actionBox, moduleField, dateBox, startBox, endBox, roomBox,
                sendButton, stopButton, viewTimetableButton, earlyLecturesButton,
                responseArea, darkModeToggle
        );

        sendButton.setOnAction(e -> handleRequest());
        stopButton.setOnAction(e -> stopConnection());
        viewTimetableButton.setOnAction(e -> displaySchedule());
        earlyLecturesButton.setOnAction(e -> requestEarlyLectures());

        return layout;
    }

    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        applyTheme();
        darkModeToggle.setText(isDarkMode ? "Light Mode" : "Dark Mode");
    }

    private void applyTheme() {
        String darkStyle = "-fx-base: #2d2d2d; -fx-background: #3c3f41; " +
                "-fx-control-inner-background: #3c3f41; -fx-text-fill: #e0e0e0;";

        if (isDarkMode) {
            scene.getRoot().setStyle(darkStyle);
            responseArea.setStyle(darkStyle + "-fx-background-color: #3c3f41;");
            // Apply to all other controls...
        } else {
            scene.getRoot().setStyle("");
            responseArea.setStyle("");
            // Reset all other controls...
        }
    }

    private void handleRequest() {
        if (socket == null || socket.isClosed()) {
            responseArea.appendText("Not connected to server!\n");
            return;
        }

        String action = actionBox.getValue();
        String module = moduleField.getText();
        String date = dateBox.getValue();
        String startTime = startBox.getValue();
        String endTime = endBox.getValue();
        String room = roomBox.getValue();

        if (action == null || module.isEmpty() || date == null ||
                startTime == null || endTime == null || room == null) {
            responseArea.appendText("Please fill in all required fields!\n");
            return;
        }

        String request = String.join("; ", action, module, date, startTime, endTime, room);
        out.println(request);

        try {
            String response = in.readLine();
            responseArea.appendText("Server: " + response + "\n");
        } catch (IOException e) {
            responseArea.appendText("Error receiving response from server\n");
        }
    }

    private void requestEarlyLectures() {
        if (socket == null || socket.isClosed()) {
            responseArea.appendText("Not connected to server!\n");
            return;
        }

        out.println("Early Lectures");
        try {
            String response = in.readLine();
            responseArea.appendText(response + "\n");
        } catch (IOException e) {
            responseArea.appendText("Error receiving response\n");
        }
    }

    private void displaySchedule() {
        if (socket == null || socket.isClosed()) {
            responseArea.appendText("Not connected to server!\n");
            return;
        }

        Stage timetableStage = new Stage();
        timetableStage.setTitle("Lecture Timetable");
        TextArea timetableArea = new TextArea();
        timetableArea.setEditable(false);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getChildren().add(timetableArea);

        if (isDarkMode) {
            String darkStyle = "-fx-base: #2d2d2d; -fx-background: #3c3f41; " +
                    "-fx-control-inner-background: #3c3f41; -fx-text-fill: #e0e0e0;";
            layout.setStyle(darkStyle);
            timetableArea.setStyle(darkStyle + "-fx-background-color: #3c3f41;");
        }

        timetableStage.setScene(new Scene(layout, 400, 300));
        timetableStage.show();

        out.println("Display Schedule");
        try {
            StringBuilder schedule = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null && !line.equals("END")) {
                schedule.append(line).append("\n");
            }
            timetableArea.setText(schedule.toString().trim());
        } catch (IOException e) {
            timetableArea.setText("Error retrieving timetable from server");
        }
    }

    private void stopConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                out.println("STOP");
                socket.close();
                responseArea.appendText("Disconnected from server\n");
            }
        } catch (IOException e) {
            responseArea.appendText("Error disconnecting: " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}