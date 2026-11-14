package com.workout.model;

import java.time.LocalDate;
import java.util.List;

// Силове тренування з обліком піднятого тоннажу
public class StrengthWorkout extends Workout {
    private double liftedWeightKg;

    public StrengthWorkout() {}

    public StrengthWorkout(Athlete athlete, LocalDate date, int durationMinutes, int intensity, List<Exercise> exercises, double liftedWeightKg) {
        super(athlete, date, durationMinutes, intensity, exercises);
        setLiftedWeightKg(liftedWeightKg);
    }

    @Override
    public String getTypeName() {
        return "Силове";
    }

    // Розрахунок калорій за тривалістю, тоннажем і кількістю вправ
    @Override
    public double calculateCalories() {
        int count = getExercises().size();
        double raw = getDurationMinutes() * getIntensity() * 0.065 + liftedWeightKg * 0.04 + count * 15.0;
        return Math.round(raw * 10.0) / 10.0;
    }

    public double getLiftedWeightKg() {
        return liftedWeightKg;
    }

    public void setLiftedWeightKg(double liftedWeightKg) {
        if (liftedWeightKg < 0 || liftedWeightKg > 100_000) {
            throw new IllegalArgumentException("Тоннаж має бути від 0 до 100 000 кг");
        }
        this.liftedWeightKg = liftedWeightKg;
    }
}
