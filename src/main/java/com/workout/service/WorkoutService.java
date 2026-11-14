package com.workout.service;

import com.workout.model.Athlete;
import com.workout.model.Workout;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

// Сервіс управління спортсменами, тренуваннями та розрахунку навантаження
public class WorkoutService {
    private final List<Athlete> athletes = new ArrayList<>();
    private final List<Workout> workouts = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    // Повна заміна даних у пам'яті під час імпорту або генерації
    public void replaceAll(List<Athlete> newAthletes, List<Workout> newWorkouts) {
        lock.lock();
        try {
            athletes.clear();
            workouts.clear();
            if (newAthletes != null) athletes.addAll(newAthletes);
            if (newWorkouts != null) workouts.addAll(newWorkouts);
        } finally {
            lock.unlock();
        }
    }

    public void addAthlete(Athlete athlete) {
        lock.lock();
        try {
            if (athletes.contains(athlete)) {
                throw new IllegalArgumentException("Спортсмен із таким ім'ям вже є у списку");
            }
            athletes.add(athlete);
        } finally {
            lock.unlock();
        }
    }

    // Оновлення даних спортсмена та синхронізація у пов'язаних тренуваннях
    public void updateAthlete(Athlete target, String newName, double newWeight) {
        lock.lock();
        try {
            if (target == null) {
                throw new IllegalArgumentException("Спортсмена не обрано");
            }
            target.setName(newName);
            target.setWeightKg(newWeight);
        } finally {
            lock.unlock();
        }
    }

    public void removeAthlete(Athlete target) {
        lock.lock();
        try {
            athletes.remove(target);
            workouts.removeIf(w -> w.getAthlete().equals(target));
        } finally {
            lock.unlock();
        }
    }

    // Додавання тренування з перевіркою на дублікати
    public void addWorkout(Workout workout) {
        lock.lock();
        try {
            boolean duplicate = workouts.stream().anyMatch(w ->
                    w.getAthlete().equals(workout.getAthlete()) &&
                    w.getDate().equals(workout.getDate()) &&
                    w.getTypeName().equals(workout.getTypeName()) &&
                    w.getDurationMinutes() == workout.getDurationMinutes()
            );
            if (duplicate) {
                throw new IllegalArgumentException("Таке тренування вже записано на цю дату");
            }
            workouts.add(workout);
        } finally {
            lock.unlock();
        }
    }

    // Заміна відредагованого тренування за ідентифікатором
    public void updateWorkout(String id, Workout updated) {
        lock.lock();
        try {
            for (int i = 0; i < workouts.size(); i++) {
                if (workouts.get(i).getId().equals(id)) {
                    updated.setId(id);
                    workouts.set(i, updated);
                    return;
                }
            }
            throw new IllegalArgumentException("Запис для оновлення не знайдено");
        } finally {
            lock.unlock();
        }
    }

    public void removeWorkout(Workout workout) {
        lock.lock();
        try {
            workouts.remove(workout);
        } finally {
            lock.unlock();
        }
    }

    public List<Athlete> getAthletesSnapshot() {
        lock.lock();
        try {
            return new ArrayList<>(athletes);
        } finally {
            lock.unlock();
        }
    }

    public List<Workout> getWorkoutsSnapshot() {
        lock.lock();
        try {
            return new ArrayList<>(workouts);
        } finally {
            lock.unlock();
        }
    }

    // Однопотоковий розрахунок тижневого навантаження для заміру T1
    public Map<String, Double> calculateLoadSingleThread(LocalDate from, LocalDate to) {
        List<Workout> snapshot = getWorkoutsSnapshot();
        Map<String, Double> totals = new HashMap<>();
        for (Workout w : snapshot) {
            if (!w.getDate().isBefore(from) && !w.getDate().isAfter(to)) {
                double score = computeLoadIndex(w);
                totals.merge(w.getAthlete().getName(), score, Double::sum);
            }
        }
        return totals;
    }

    // Багатопотоковий розрахунок навантаження через ExecutorService та ReentrantLock
    public Map<String, Double> calculateLoadParallel(LocalDate from, LocalDate to, int threads)
            throws InterruptedException, ExecutionException {
        List<Workout> snapshot = getWorkoutsSnapshot();
        if (snapshot.isEmpty()) return Collections.emptyMap();

        int chunkSize = Math.max(1, (snapshot.size() + threads - 1) / threads);
        List<Callable<Void>> tasks = new ArrayList<>();
        Map<String, Double> sharedTotals = new HashMap<>();
        ReentrantLock mergeLock = new ReentrantLock();

        for (int i = 0; i < snapshot.size(); i += chunkSize) {
            List<Workout> sub = snapshot.subList(i, Math.min(i + chunkSize, snapshot.size()));
            tasks.add(() -> {
                Map<String, Double> localPart = new HashMap<>();
                for (Workout w : sub) {
                    if (!w.getDate().isBefore(from) && !w.getDate().isAfter(to)) {
                        double score = computeLoadIndex(w);
                        localPart.merge(w.getAthlete().getName(), score, Double::sum);
                    }
                }
                mergeLock.lock();
                try {
                    localPart.forEach((athlete, val) -> sharedTotals.merge(athlete, val, Double::sum));
                } finally {
                    mergeLock.unlock();
                }
                return null;
            });
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Void>> futures = pool.invokeAll(tasks);
            for (Future<Void> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdown();
        }
        return sharedTotals;
    }

    private double computeLoadIndex(Workout w) {
        double value = w.calculateWeeklyLoadScore() + w.calculateCalories() * 0.05;
        for (int i = 0; i < 450; i++) {
            value = Math.sqrt(value * value + 0.0001);
        }
        return value;
    }
}
