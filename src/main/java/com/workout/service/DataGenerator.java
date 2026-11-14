package com.workout.service;

import com.workout.model.*;
import com.workout.repository.StorageData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Генератор тестових наборів тренувань на 50+ і 10 000+ записів
public class DataGenerator {
    private static final int MAX_COUNT = 1_000_000;
    private static final int DAYS_BACK = 60;
    private static final long SEED = 42;

    private static final String[] NAMES = {
            "Олександр Коваль", "Марія Бондаренко", "Дмитро Шевченко",
            "Анна Мельник", "Максим Ткаченко", "Ірина Кравченко",
            "Артем Олійник", "Софія Поліщук"
    };

    private static final String[] EXERCISES = {
            "Присідання зі штангою", "Жим гантелей", "Інтервальний біг",
            "Планка", "Станова тяга", "Розтяжка спини", "Випади"
    };

    // ШІ: Claude Code (Claude Opus) – перевірка кількості записів, іменовані константи замість чисел у коді.
    // Змінено: адаптовано під модель Workout, перевірено обробку винятків та логування.
    public static StorageData generate(int count) {
        if (count <= 0 || count > MAX_COUNT) {
            throw new IllegalArgumentException("Кількість записів має бути від 1 до " + MAX_COUNT);
        }
        Random rnd = new Random(SEED);
        List<Athlete> athletes = new ArrayList<>();
        for (String name : NAMES) {
            athletes.add(new Athlete(name, 58 + rnd.nextInt(35)));
        }

        LocalDate today = LocalDate.now();
        List<Workout> workouts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Athlete athlete = athletes.get(rnd.nextInt(athletes.size()));
            LocalDate date = today.minusDays(rnd.nextInt(DAYS_BACK));
            int duration = 20 + rnd.nextInt(95);
            int intensity = 1 + rnd.nextInt(10);
            List<Exercise> exercises = List.of(
                    new Exercise(EXERCISES[rnd.nextInt(EXERCISES.length)], 3 + rnd.nextInt(3), 8 + rnd.nextInt(10))
            );

            Workout workout = switch (i % 3) {
                case 0 -> new CardioWorkout(athlete, date, duration, intensity, exercises, 2 + rnd.nextInt(15));
                case 1 -> new StrengthWorkout(athlete, date, duration, intensity, exercises, 400 + rnd.nextInt(2500));
                default -> new StretchingWorkout(athlete, date, duration, intensity, exercises, rnd.nextBoolean());
            };
            workouts.add(workout);
        }
        return new StorageData(athletes, workouts);
    }
}
