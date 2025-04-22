package org.example.eventdrivenprogrammingproject1;

import java.util.Objects;

public class Lecture {
    private final String module;
    private final String date;
    private final String startTime;
    private final String endTime;
    private final String room;

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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Lecture)) return false;
        Lecture other = (Lecture) obj;
        return module.equals(other.module) && date.equals(other.date) &&
                startTime.equals(other.startTime) && endTime.equals(other.endTime) &&
                room.equals(other.room);
    }

    @Override
    public int hashCode() {
        return Objects.hash(module, date, startTime, endTime, room);
    }

    @Override
    public String toString() {
        return module + ": " + date + " from " + startTime + " to " + endTime + " in " + room;
    }

    public static class IncorrectActionException extends Exception {
        public IncorrectActionException(String message) {
            super(message);
        }
    }
}