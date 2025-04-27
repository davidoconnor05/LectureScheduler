package org.example.eventdrivenprogrammingproject1;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.io.*;
import java.net.*;
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
            log("Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                log("New client connected: " + clientSocket.getInetAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            log("Server error: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String request;
            while ((request = in.readLine()) != null) {
                log("Received: " + request);
                String response = processRequest(request);
                out.println(response);
                if (request.equals("Display Schedule")) {
                    sendSchedule(out);
                }
                log("Sent: " + response);
            }
        } catch (IOException e) {
            log("Client disconnected: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log("Error closing socket: " + e.getMessage());
            }
        }
    }

    private String processRequest(String request) {
        String[] parts = request.split("; ");
        if (parts.length == 0) return "Error: Empty request";

        switch (parts[0]) {
            case "Add Lecture":
                return parts.length == 6 ? addLecture(parts) : "Error: Invalid format";
            case "Remove Lecture":
                return parts.length == 6 ? removeLecture(parts) : "Error: Invalid format";
            case "Early Lectures":
                return getEarlyLectures();
            case "Display Schedule":
                return "";
            case "STOP":
                return "Goodbye";
            default:
                return "Error: Unknown command";
        }
    }

    private String addLecture(String[] parts) {
        Lecture newLecture = new Lecture(parts[1], parts[2], parts[3], parts[4], parts[5]);
        synchronized (schedule) {
            List<Lecture> lectures = schedule.computeIfAbsent(parts[1], k -> new ArrayList<>());
            if (isClash(newLecture)) {
                return "Error: Scheduling conflict";
            }
            lectures.add(newLecture);
            return "Lecture added successfully";
        }
    }

    private String removeLecture(String[] parts) {
        Lecture toRemove = new Lecture(parts[1], parts[2], parts[3], parts[4], parts[5]);
        synchronized (schedule) {
            List<Lecture> lectures = schedule.get(parts[1]);
            if (lectures != null && lectures.remove(toRemove)) {
                return "Lecture removed successfully";
            }
            return "Error: Lecture not found";
        }
    }

    private String getEarlyLectures() {
        StringBuilder result = new StringBuilder("Early Lectures:\n");
        synchronized (schedule) {
            schedule.values().stream()
                    .flatMap(List::stream)
                    .filter(Lecture::isEarlyLecture)
                    .forEach(lecture -> result.append(lecture).append("\n"));
        }
        return result.toString().trim();
    }

    private void sendSchedule(PrintWriter out) {
        synchronized (schedule) {
            if (schedule.isEmpty()) {
                out.println("No lectures scheduled");
            } else {
                schedule.values().stream()
                        .flatMap(List::stream)
                        .forEach(lecture -> out.println(lecture));
            }
            out.println("END");
        }
    }

    private boolean isClash(Lecture newLecture) {
        return schedule.values().stream()
                .flatMap(List::stream)
                .anyMatch(lecture -> lecture.clashesWith(newLecture));
    }

    private void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    private static class Lecture {
        private final String module, date, startTime, endTime, room;

        public Lecture(String module, String date, String startTime, String endTime, String room) {
            this.module = module;
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
            this.room = room;
        }

        public boolean clashesWith(Lecture other) {
            return this.date.equals(other.date) && this.room.equals(other.room) &&
                    (this.startTime.equals(other.startTime) || this.endTime.equals(other.endTime));
        }

        public boolean isEarlyLecture() {
            return Integer.parseInt(startTime.split(":")[0]) < 12;
        }

        @Override
        public String toString() {
            return String.format("%s: %s from %s to %s in %s",
                    module, date, startTime, endTime, room);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Lecture)) return false;
            Lecture lecture = (Lecture) o;
            return module.equals(lecture.module) &&
                    date.equals(lecture.date) &&
                    startTime.equals(lecture.startTime) &&
                    endTime.equals(lecture.endTime) &&
                    room.equals(lecture.room);
        }

        @Override
        public int hashCode() {
            return Objects.hash(module, date, startTime, endTime, room);
        }
    }
}