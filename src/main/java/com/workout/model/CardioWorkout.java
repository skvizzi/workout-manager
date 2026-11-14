package com.workout.model;

import java.time.LocalDate;
import java.util.List;

// Кардіотренування з обліком пройденої дистанції
public class CardioWorkout extends Workout {
    private double distanceKm;

    public CardioWorkout() {}

    public CardioWorkout(Athlete athlete, LocalDate date, int durationMinutes, int intensity, List<Exercise> exercises, double distanceKm) {
        super(athlete, date, durationMinutes, intensity, exercises);
        setDistanceKm(distanceKm);
    }

    @Override
    public String getTypeName() {
        return "Кардіо";
    }

    // Розрахунок калорій з урахуванням ваги спортсмена та дистанції
    @Override
    public double calculateCalories() {
        double weight = getAthlete() != null ? getAthlete().getWeightKg() : 70.0;
        double raw = getDurationMinutes() * getIntensity() * 0.08 * (weight / 70.0) + distanceKm * 12.0;
        return Math.round(raw * 10.0) / 10.0;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        if (distanceKm < 0 || distanceKm > 300) {
            throw new IllegalArgumentException("Дистанція має бути від 0 до 300 км");
        }
        this.distanceKm = distanceKm;
    }
}
