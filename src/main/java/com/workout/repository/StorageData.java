package com.workout.repository;

import com.workout.model.Athlete;
import com.workout.model.Workout;

import java.util.ArrayList;
import java.util.List;

// Контейнер для серіалізації списків спортсменів і тренувань у JSON
public class StorageData {
    private List<Athlete> athletes = new ArrayList<>();
    private List<Workout> workouts = new ArrayList<>();

    public StorageData() {}

    public StorageData(List<Athlete> athletes, List<Workout> workouts) {
        this.athletes = athletes;
        this.workouts = workouts;
    }

    public List<Athlete> getAthletes() {
        return athletes;
    }

    public void setAthletes(List<Athlete> athletes) {
        this.athletes = athletes;
    }

    public List<Workout> getWorkouts() {
        return workouts;
    }

    public void setWorkouts(List<Workout> workouts) {
        this.workouts = workouts;
    }
}
