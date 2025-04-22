package org.example.eventdrivenprogrammingproject1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.Map;


public class ClientHandler extends Thread {
    private final Socket socket;
    private final Map<String, List<Lecture>> schedule;
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket, Map<String, List<Lecture>> schedule) {
        this.socket = socket;
        this.schedule = schedule;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String request;
            while ((request = in.readLine()) != null) {
                System.out.println("Received: " + request);
                handleRequest(request);
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    private void handleRequest(String request) {
        String[] parts = request.split("; ");
        String action = parts[0];

        try {
            switch (action) {
                case "Add Lecture":
                    addLecture(parts);
                    break;
                case "Remove Lecture":
                    removeLecture(parts);
                    break;
                case "Display Schedule":
                    displaySchedule();
                    break;
                default:
                    out.println("Error: Invalid action: " + action);
            }
        } catch (Exception e) {
            out.println("Error: " + e.getMessage());
        }
    }

    private void addLecture(String[] parts) {
        if (parts.length != 6) {
            out.println("Invalid Add Lecture request format.");
            return;
        }

        String module = parts[1];
        String date = parts[2];
        String startTime = parts[3];
        String endTime = parts[4];
        String room = parts[5];

        Lecture newLecture = new Lecture(module, date, startTime, endTime, room);
        List<Lecture> lectures = schedule.computeIfAbsent(module, k -> new ArrayList<>());

        if (isClash(newLecture)) {
            out.println("Scheduling clash detected.");
        } else {
            lectures.add(newLecture);
            out.println("Lecture added successfully.");
            printAllLectures();
        }
    }

    private boolean isClash(Lecture newLecture) {
        for (List<Lecture> lectures : schedule.values()) {
            for (Lecture lecture : lectures) {
                if (lecture.clashesWith(newLecture)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeLecture(String[] parts) {
        if (parts.length != 6) {
            out.println("Invalid Remove Lecture request format.");
            return;
        }

        String module = parts[1];
        String date = parts[2];
        String startTime = parts[3];
        String endTime = parts[4];
        String room = parts[5];

        Lecture lectureToRemove = new Lecture(module, date, startTime, endTime, room);
        List<Lecture> lectures = schedule.get(module);

        if (lectures != null && lectures.removeIf(lecture -> lecture.equals(lectureToRemove))) {
            out.println("Lecture removed. Freed: " + room + " on " + date + " from " + startTime + " to " + endTime);
            printAllLectures();
        } else {
            out.println("Lecture not found.");
        }
    }

    private void displaySchedule() {
        StringBuilder sb = new StringBuilder();
        if (schedule.isEmpty()) {
            sb.append("No lectures found.");
        } else {
            for (Map.Entry<String, List<Lecture>> entry : schedule.entrySet()) {
                for (Lecture lecture : entry.getValue()) {
                    sb.append(lecture).append("\n");
                }
            }
        }
        out.println(sb.toString().trim());
        out.println("END"); // Marks the end of data
    }

    private void printAllLectures() {
        System.out.println("Current Lecture Schedule:");
        for (Map.Entry<String, List<Lecture>> entry : schedule.entrySet()) {
            System.out.println("Module: " + entry.getKey());
            for (Lecture lecture : entry.getValue()) {
                System.out.println("  " + lecture);
            }
        }
    }
}