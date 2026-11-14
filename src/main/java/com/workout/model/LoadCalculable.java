package com.workout.model;

// Інтерфейс для розрахунку витрачених калорій та балів навантаження
public interface LoadCalculable {
    double calculateCalories();
    double calculateWeeklyLoadScore();
}
