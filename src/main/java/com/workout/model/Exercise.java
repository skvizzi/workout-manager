package com.workout.model;

// Окрема вправа у складі тренування
public class Exercise {
    private String title;
    private int sets;
    private int reps;

    public Exercise() {}

    public Exercise(String title, int sets, int reps) {
        setTitle(title);
        setSets(sets);
        setReps(reps);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Назва вправи не може бути порожньою");
        }
        this.title = title.trim();
    }

    public int getSets() {
        return sets;
    }

    public void setSets(int sets) {
        if (sets <= 0) {
            throw new IllegalArgumentException("Кількість підходів має бути більшою за 0");
        }
        this.sets = sets;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        if (reps <= 0) {
            throw new IllegalArgumentException("Кількість повторень має бути більшою за 0");
        }
        this.reps = reps;
    }

    @Override
    public String toString() {
        return title + " (" + sets + "x" + reps + ")";
    }
}
