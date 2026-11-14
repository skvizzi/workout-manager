package com.workout.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Абстрактний базовий клас спортивного тренування
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "workoutType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardioWorkout.class, name = "CARDIO"),
        @JsonSubTypes.Type(value = StrengthWorkout.class, name = "STRENGTH"),
        @JsonSubTypes.Type(value = StretchingWorkout.class, name = "STRETCHING")
})
public abstract class Workout implements LoadCalculable {
    private String id;
    private Athlete athlete;
    private LocalDate date;
    private int durationMinutes;
    private int intensity;
    private List<Exercise> exercises = new ArrayList<>();

    protected Workout() {
        this.id = UUID.randomUUID().toString();
    }

    protected Workout(Athlete athlete, LocalDate date, int durationMinutes, int intensity, List<Exercise> exercises) {
        this();
        setAthlete(athlete);
        setDate(date);
        setDurationMinutes(durationMinutes);
        setIntensity(intensity);
        setExercises(exercises);
    }

    @JsonIgnore
    public abstract String getTypeName();

    // Базовий розрахунок індексу тижневого навантаження
    @Override
    public double calculateWeeklyLoadScore() {
        return durationMinutes * intensity * 0.15;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public void setAthlete(Athlete athlete) {
        if (athlete == null) {
            throw new IllegalArgumentException("Потрібно обрати спортсмена");
        }
        this.athlete = athlete;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        if (date == null || date.isAfter(LocalDate.now().plusYears(1))) {
            throw new IllegalArgumentException("Некоректна дата тренування");
        }
        this.date = date;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        if (durationMinutes <= 0 || durationMinutes > 600) {
            throw new IllegalArgumentException("Тривалість має бути від 1 до 600 хв");
        }
        this.durationMinutes = durationMinutes;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        if (intensity < 1 || intensity > 10) {
            throw new IllegalArgumentException("Інтенсивність має бути від 1 до 10");
        }
        this.intensity = intensity;
    }

    public List<Exercise> getExercises() {
        return exercises;
    }

    public void setExercises(List<Exercise> exercises) {
        this.exercises = (exercises == null) ? new ArrayList<>() : new ArrayList<>(exercises);
    }
}
