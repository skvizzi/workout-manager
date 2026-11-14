package com.workout.model;

import java.time.LocalDate;
import java.util.List;

// Тренування з розтяжки з відміткою про додатковий інвентар
public class StretchingWorkout extends Workout {
    private boolean useEquipment;

    public StretchingWorkout() {}

    public StretchingWorkout(Athlete athlete, LocalDate date, int durationMinutes, int intensity, List<Exercise> exercises, boolean useEquipment) {
        super(athlete, date, durationMinutes, intensity, exercises);
        this.useEquipment = useEquipment;
    }

    @Override
    public String getTypeName() {
        return "Розтяжка";
    }

    // Розрахунок калорій для стретчингу з коефіцієнтом інвентарю
    @Override
    public double calculateCalories() {
        double factor = useEquipment ? 1.15 : 1.0;
        double raw = getDurationMinutes() * getIntensity() * 0.035 * factor;
        return Math.round(raw * 10.0) / 10.0;
    }

    public boolean isUseEquipment() {
        return useEquipment;
    }

    public void setUseEquipment(boolean useEquipment) {
        this.useEquipment = useEquipment;
    }
}
