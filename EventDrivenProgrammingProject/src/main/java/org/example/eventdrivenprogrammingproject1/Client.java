package org.example.eventdrivenprogrammingproject1;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;

public class Client extends Application {
    private TextArea responseArea;
    private ComboBox<String> actionBox;
    private TextField moduleField;
    private ComboBox<String> dateBox, startBox, endBox, roomBox;
    private Button sendButton, stopButton, viewTimetableButton;
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
        scene = new Scene(root, 350, 450);
        primaryStage.setScene(scene);
        primaryStage.show();
        connectToServer();
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
        roomBox.getItems().addAll("Room A", "Room B", "Room C", "Room D","Room E","Room F");

        sendButton = new Button("Send Request");
        stopButton = new Button("STOP");
        viewTimetableButton = new Button("View Timetable");

        responseArea = new TextArea();
        responseArea.setEditable(false);
        responseArea.setPromptText("Server response will appear here...");

        darkModeToggle = new ToggleButton("Dark Mode");
        darkModeToggle.setOnAction(e -> toggleDarkMode());

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getChildren().addAll(actionBox, moduleField, dateBox, startBox, endBox, roomBox,
                sendButton, stopButton, viewTimetableButton, responseArea, darkModeToggle);

        sendButton.setOnAction(e -> sendRequest());
        stopButton.setOnAction(e -> stopConnection());
        viewTimetableButton.setOnAction(e -> requestTimetable());

        return layout;
    }

    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        applyTheme();
        darkModeToggle.setText(isDarkMode ? "Light Mode" : "Dark Mode");
    }

    private void applyTheme() {
        String darkStyle = "-fx-base: #2d2d2d; " +
                "-fx-background: #3c3f41; " +
                "-fx-control-inner-background: #3c3f41; " +
                "-fx-text-fill: #e0e0e0;";

        if (isDarkMode) {
            // Apply dark theme to main components
            scene.getRoot().setStyle(darkStyle);
            responseArea.setStyle(darkStyle + "-fx-background-color: #3c3f41;");
            moduleField.setStyle(darkStyle);
            actionBox.setStyle(darkStyle);
            dateBox.setStyle(darkStyle);
            startBox.setStyle(darkStyle);
            endBox.setStyle(darkStyle);
            roomBox.setStyle(darkStyle);
            sendButton.setStyle(darkStyle);
            stopButton.setStyle(darkStyle);
            viewTimetableButton.setStyle(darkStyle);
        } else {
            // Reset to default theme
            scene.getRoot().setStyle("");
            responseArea.setStyle("");
            moduleField.setStyle("");
            actionBox.setStyle("");
            dateBox.setStyle("");
            startBox.setStyle("");
            endBox.setStyle("");
            roomBox.setStyle("");
            sendButton.setStyle("");
            stopButton.setStyle("");
            viewTimetableButton.setStyle("");
        }
    }

    private void requestTimetable() {
        if (socket == null || socket.isClosed()) {
            responseArea.appendText("Not connected to server!\n");
            return;
        }

        out.println("Display Schedule");
        Stage timetableStage = new Stage();
        timetableStage.setTitle("Lecture Timetable");

        TextArea timetableArea = new TextArea();
        timetableArea.setEditable(false);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getChildren().add(timetableArea);

        // Apply dark mode to timetable window if enabled
        if (isDarkMode) {
            String darkStyle = "-fx-base: #2d2d2d; " +
                    "-fx-background: #3c3f41; " +
                    "-fx-control-inner-background: #3c3f41; " +
                    "-fx-text-fill: #e0e0e0;";
            layout.setStyle(darkStyle);
            timetableArea.setStyle(darkStyle + "-fx-background-color: #3c3f41;");
        }

        timetableStage.setScene(new Scene(layout, 400, 300));
        timetableStage.show();

        try {
            String line;
            StringBuilder schedule = new StringBuilder();
            while ((line = in.readLine()) != null) {
                if ("END".equals(line)) break;
                schedule.append(line).append("\n");
            }
            timetableArea.setText(schedule.toString().trim());
        } catch (IOException e) {
            timetableArea.setText("Error retrieving timetable from server.");
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 291);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            responseArea.appendText("Connected to Server\n");
        } catch (IOException e) {
            responseArea.appendText("Failed to connect to server\n");
        }
    }

    private void sendRequest() {
        if (socket == null || socket.isClosed()) {
            responseArea.appendText("Not connected to server!\n");
            return;
        }

        String action = actionBox.getValue();
        String module = moduleField.getText();
        String date = dateBox.getValue();
        String timeStart = startBox.getValue();
        String timeEnd = endBox.getValue();
        String room = roomBox.getValue();

        if (action == null || (action.equals("Add Lecture") || action.equals("Remove Lecture")) &&
                (module.isEmpty() || date == null || timeStart == null || timeEnd == null || room == null)) {
            responseArea.appendText("Please fill in all required fields!\n");
            return;
        }

        String request = action + "; " + module + "; " + date + "; " + timeStart + "; " + timeEnd + "; " + room;
        out.println(request);

        try {
            String response = in.readLine();
            responseArea.appendText("Server: " + response + "\n");
        } catch (IOException e) {
            responseArea.appendText("Error receiving response from server\n");
        }
    }

    private void stopConnection() {
        if (socket != null && !socket.isClosed()) {
            out.println("STOP");
            responseArea.appendText("Sent STOP to server\n");
            try {
                socket.close();
            } catch (IOException e) {
                responseArea.appendText("Error closing connection\n");
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}