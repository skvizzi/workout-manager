package com.workout.model;

import java.util.Objects;

// Дані спортсмена з перевіркою імені та ваги
public class Athlete {
    private String name;
    private double weightKg;

    public Athlete() {}

    public Athlete(String name, double weightKg) {
        setName(name);
        setWeightKg(weightKg);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Ім'я спортсмена не може бути порожнім");
        }
        this.name = name.trim();
    }

    public double getWeightKg() {
        return weightKg;
    }

    // Перевірка допустимих меж ваги спортсмена
    public void setWeightKg(double weightKg) {
        if (weightKg < 25 || weightKg > 250) {
            throw new IllegalArgumentException("Вага має бути в межах від 25 до 250 кг");
        }
        this.weightKg = weightKg;
    }

    @Override
    public String toString() {
        return name + " (" + weightKg + " кг)";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Athlete other)) return false;
        return Objects.equals(
                name == null ? "" : name.toLowerCase(),
                other.name == null ? "" : other.name.toLowerCase()
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(name == null ? "" : name.toLowerCase());
    }
}
