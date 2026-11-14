package com.workout.controller;

import com.workout.model.*;
import com.workout.repository.*;
import com.workout.service.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.logging.*;

// Контролер головного вікна застосунку за архітектурою MVC
public class MainController {
    private static final Logger LOG = Logger.getLogger(MainController.class.getName());

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterBox;
    @FXML private DatePicker filterFromPicker;
    @FXML private DatePicker filterToPicker;

    @FXML private TableView<Workout> workoutTable;
    @FXML private TableColumn<Workout, String> colAthlete;
    @FXML private TableColumn<Workout, String> colType;
    @FXML private TableColumn<Workout, String> colDate;
    @FXML private TableColumn<Workout, Number> colDuration;
    @FXML private TableColumn<Workout, Number> colIntensity;
    @FXML private TableColumn<Workout, Number> colCalories;
    @FXML private TableColumn<Workout, String> colExercises;

    @FXML private ComboBox<Athlete> athleteBox;
    @FXML private ComboBox<String> workoutTypeBox;
    @FXML private DatePicker datePicker;
    @FXML private TextField durationField;
    @FXML private TextField intensityField;
    @FXML private TextField extraParamField;
    @FXML private TextField exerciseTitleField;

    @FXML private TextField newAthleteNameField;
    @FXML private TextField newAthleteWeightField;

    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;

    private final WorkoutService service = new WorkoutService();
    private final JsonStorage jsonStorage = new JsonStorage();
    private final XmlReportExporter xmlExporter = new XmlReportExporter();

    private final ObservableList<Workout> masterWorkouts = FXCollections.observableArrayList();
    private final ObservableList<Athlete> masterAthletes = FXCollections.observableArrayList();
    private FilteredList<Workout> filteredWorkouts;

    static {
        try {
            FileHandler fh = new FileHandler("workout_manager.log", true);
            fh.setFormatter(new SimpleFormatter());
            LOG.addHandler(fh);
            LOG.setUseParentHandlers(false);
        } catch (IOException ignored) {}
    }

    // Початкове налаштування таблиці, фільтрів та стартового файлу trainings.json
    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        setupSelectionListeners();

        workoutTypeBox.getItems().addAll("Кардіо", "Силове", "Розтяжка");
        workoutTypeBox.getSelectionModel().selectFirst();
        athleteBox.setItems(masterAthletes);
        datePicker.setValue(LocalDate.now());

        initDefaultJsonFile();
    }

    private void initDefaultJsonFile() {
        File defaultFile = new File("trainings.json");
        try {
            if (defaultFile.exists() && defaultFile.length() > 0) {
                StorageData loaded = jsonStorage.load(defaultFile);
                service.replaceAll(loaded.getAthletes(), loaded.getWorkouts());
            } else {
                StorageData initial = DataGenerator.generate(60);
                jsonStorage.save(defaultFile, initial);
                service.replaceAll(initial.getAthletes(), initial.getWorkouts());
            }
        } catch (Exception e) {
            LOG.warning("Не вдалося прочитати trainings.json, створено новий набір: " + e.getMessage());
            StorageData fallback = DataGenerator.generate(60);
            service.replaceAll(fallback.getAthletes(), fallback.getWorkouts());
        }
        refreshCollections();
    }

    private void setupTable() {
        colAthlete.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAthlete().getName()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTypeName()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDate().toString()));
        colDuration.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getDurationMinutes()));
        colIntensity.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getIntensity()));
        colCalories.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().calculateCalories()));
        colExercises.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getExercises().toString()));

        filteredWorkouts = new FilteredList<>(masterWorkouts, p -> true);
        SortedList<Workout> sorted = new SortedList<>(filteredWorkouts);
        sorted.comparatorProperty().bind(workoutTable.comparatorProperty());
        workoutTable.setItems(sorted);
    }

    private void setupFilters() {
        typeFilterBox.getItems().addAll("Всі", "Кардіо", "Силове", "Розтяжка");
        typeFilterBox.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        typeFilterBox.valueProperty().addListener((obs, o, n) -> applyFilters());
        filterFromPicker.valueProperty().addListener((obs, o, n) -> applyFilters());
        filterToPicker.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void setupSelectionListeners() {
        workoutTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected == null) return;
            athleteBox.setValue(selected.getAthlete());
            workoutTypeBox.setValue(selected.getTypeName());
            datePicker.setValue(selected.getDate());
            durationField.setText(String.valueOf(selected.getDurationMinutes()));
            intensityField.setText(String.valueOf(selected.getIntensity()));

            if (selected instanceof CardioWorkout c) {
                extraParamField.setText(String.valueOf(c.getDistanceKm()));
            } else if (selected instanceof StrengthWorkout s) {
                extraParamField.setText(String.valueOf(s.getLiftedWeightKg()));
            } else if (selected instanceof StretchingWorkout st) {
                extraParamField.setText(st.isUseEquipment() ? "1" : "0");
            }

            if (!selected.getExercises().isEmpty()) {
                exerciseTitleField.setText(selected.getExercises().getFirst().getTitle());
            }
        });

        athleteBox.valueProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                newAthleteNameField.setText(selected.getName());
                newAthleteWeightField.setText(String.valueOf(selected.getWeightKg()));
            }
        });
    }

    // Фільтрація списку за спортсменом, типом і датами
    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String type = typeFilterBox.getValue();
        LocalDate from = filterFromPicker.getValue();
        LocalDate to = filterToPicker.getValue();

        filteredWorkouts.setPredicate(w -> {
            if (!query.isEmpty() && !w.getAthlete().getName().toLowerCase().contains(query)) return false;
            if (type != null && !"Всі".equals(type) && !w.getTypeName().equals(type)) return false;
            if (from != null && w.getDate().isBefore(from)) return false;
            if (to != null && w.getDate().isAfter(to)) return false;
            return true;
        });
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        typeFilterBox.getSelectionModel().selectFirst();
        filterFromPicker.setValue(null);
        filterToPicker.setValue(null);
    }

    @FXML
    private void onAddAthlete() {
        try {
            String name = newAthleteNameField.getText();
            double weight = Double.parseDouble(newAthleteWeightField.getText().trim());
            Athlete athlete = new Athlete(name, weight);
            service.addAthlete(athlete);
            masterAthletes.setAll(service.getAthletesSnapshot());
            athleteBox.getSelectionModel().select(athlete);
            statusLabel.setText("Спортсмена додано: " + athlete.getName());
        } catch (Exception e) {
            LOG.warning("Помилка додавання спортсмена: " + e.getMessage());
            showError("Не вдалося додати спортсмена: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdateAthlete() {
        try {
            Athlete selected = athleteBox.getValue();
            String name = newAthleteNameField.getText();
            double weight = Double.parseDouble(newAthleteWeightField.getText().trim());
            service.updateAthlete(selected, name, weight);
            refreshCollections();
            workoutTable.refresh();
            statusLabel.setText("Дані спортсмена оновлено");
        } catch (Exception e) {
            LOG.warning("Помилка оновлення спортсмена: " + e.getMessage());
            showError("Не вдалося оновити спортсмена: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteAthlete() {
        Athlete selected = athleteBox.getValue();
        if (selected == null) return;
        service.removeAthlete(selected);
        refreshCollections();
        statusLabel.setText("Спортсмена та його тренування видалено");
    }

    @FXML
    private void onAddWorkout() {
        try {
            Workout workout = buildWorkoutFromForm();
            service.addWorkout(workout);
            masterWorkouts.add(workout);
            statusLabel.setText("Тренування додано (" + workout.calculateCalories() + " ккал)");
        } catch (Exception e) {
            LOG.warning("Помилка створення тренування: " + e.getMessage());
            showError("Перевірте введені поля: " + e.getMessage());
        }
    }

    @FXML
    private void onUpdateWorkout() {
        Workout selected = workoutTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Оберіть тренування в таблиці для редагування");
            return;
        }
        try {
            Workout updated = buildWorkoutFromForm();
            service.updateWorkout(selected.getId(), updated);
            refreshCollections();
            workoutTable.refresh();
            statusLabel.setText("Тренування оновлено");
        } catch (Exception e) {
            LOG.warning("Помилка редагування тренування: " + e.getMessage());
            showError("Помилка оновлення: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteWorkout() {
        Workout selected = workoutTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        service.removeWorkout(selected);
        masterWorkouts.remove(selected);
        statusLabel.setText("Тренування видалено");
    }

    private Workout buildWorkoutFromForm() {
        Athlete athlete = athleteBox.getValue();
        LocalDate date = datePicker.getValue();
        int duration = Integer.parseInt(durationField.getText().trim());
        int intensity = Integer.parseInt(intensityField.getText().trim());
        double extra = extraParamField.getText().isBlank() ? 0 : Double.parseDouble(extraParamField.getText().trim());
        String exTitle = exerciseTitleField.getText().isBlank() ? "Базова вправа" : exerciseTitleField.getText().trim();

        List<Exercise> exercises = List.of(new Exercise(exTitle, 3, 12));
        String type = workoutTypeBox.getValue();

        return switch (type) {
            case "Кардіо" -> new CardioWorkout(athlete, date, duration, intensity, exercises, extra);
            case "Силове" -> new StrengthWorkout(athlete, date, duration, intensity, exercises, extra);
            default -> new StretchingWorkout(athlete, date, duration, intensity, exercises, extra > 0);
        };
    }

    // Генерація 10 000 записів у фоновому потоці для перевірки швидкодії
    // ШІ: Claude Code (Claude Opus) – запуск через runInBackground, запис результату в лог.
    // Змінено: адаптовано під модель Workout, перевірено обробку винятків та логування.
    @FXML
    private void onGenerate10k() {
        statusLabel.setText("Генерація 10 000 записів...");

        Task<StorageData> task = new Task<>() {
            @Override
            protected StorageData call() {
                return DataGenerator.generate(10_000);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            StorageData data = task.getValue();
            service.replaceAll(data.getAthletes(), data.getWorkouts());
            refreshCollections();
            progressBar.setProgress(1.0);
            statusLabel.setText("Згенеровано 10 000 записів");
            LOG.info("Згенеровано тестовий набір: " + data.getWorkouts().size() + " тренувань");
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            progressBar.setProgress(0);
            LOG.severe("Збій генерації даних: " + task.getException().getMessage());
            showError(task.getException().getMessage());
        }));

        runInBackground(task, "generate-10k");
    }

    // Фоновий імпорт тренувань із JSON-файлу через javafx.concurrent.Task
    // ШІ: Claude Code (Claude Opus) – етапи прогресу через updateProgress, запуск через runInBackground, запис у лог.
    // Змінено: адаптовано під модель Workout, перевірено обробку винятків та логування.
    @FXML
    private void onImportJson() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File file = chooser.showOpenDialog(workoutTable.getScene().getWindow());
        if (file == null) return;

        statusLabel.setText("Імпорт JSON...");

        Task<StorageData> task = new Task<>() {
            @Override
            protected StorageData call() throws Exception {
                updateProgress(0, 2);
                StorageData data = jsonStorage.load(file);
                updateProgress(1, 2);
                service.replaceAll(data.getAthletes(), data.getWorkouts());
                updateProgress(2, 2);
                return data;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            refreshCollections();
            progressBar.setProgress(1.0);
            statusLabel.setText("Імпортовано записів: " + masterWorkouts.size());
            LOG.info("Імпорт JSON завершено: " + file.getName() + ", записів: " + masterWorkouts.size());
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            progressBar.setProgress(0);
            Throwable ex = task.getException();
            LOG.severe("Помилка імпорту JSON: " + ex.getMessage());
            showError("Помилка читання JSON: " + ex.getMessage());
        }));

        runInBackground(task, "import-json");
    }

    // Фоновий експорт поточного набору даних у JSON
    // ШІ: Claude Code (Claude Opus) – запуск через runInBackground, запис результату в лог.
    // Змінено: адаптовано під модель Workout, перевірено обробку винятків та логування.
    @FXML
    private void onExportJson() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("trainings.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File file = chooser.showSaveDialog(workoutTable.getScene().getWindow());
        if (file == null) return;

        StorageData snapshot = new StorageData(service.getAthletesSnapshot(), service.getWorkoutsSnapshot());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                jsonStorage.save(file, snapshot);
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            progressBar.setProgress(1.0);
            statusLabel.setText("Збережено у файл " + file.getName());
            LOG.info("Експорт JSON завершено: " + file.getName());
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            progressBar.setProgress(0);
            LOG.severe("Помилка експорту JSON: " + task.getException().getMessage());
            showError("Не вдалося зберегти JSON: " + task.getException().getMessage());
        }));

        runInBackground(task, "export-json");
    }

    // Порівняння T1/Tn на пулі потоків і збереження звіту навантаження в XML
    // ШІ: Claude Code (Claude Opus) – етапи прогресу, проміжний статус через Platform.runLater, запуск через runInBackground, запис у лог.
    // Змінено: адаптовано під модель Workout, перевірено обробку винятків та логування.
    @FXML
    private void onBenchmarkAndXmlReport() {
        LocalDate from = filterFromPicker.getValue() != null ? filterFromPicker.getValue() : LocalDate.now().minusDays(60);
        LocalDate to = filterToPicker.getValue() != null ? filterToPicker.getValue() : LocalDate.now();
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors());

        statusLabel.setText("Розрахунок тижневого навантаження...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                updateProgress(0, 3);
                long t1Start = System.nanoTime();
                service.calculateLoadSingleThread(from, to);
                double t1Ms = (System.nanoTime() - t1Start) / 1_000_000.0;
                updateProgress(1, 3);
                Platform.runLater(() -> statusLabel.setText(
                        String.format("T1 = %.2f мс, розрахунок на %d потоках...", t1Ms, threads)));

                long tnStart = System.nanoTime();
                Map<String, Double> parallelResult = service.calculateLoadParallel(from, to, threads);
                double tnMs = (System.nanoTime() - tnStart) / 1_000_000.0;
                updateProgress(2, 3);

                double speedup = t1Ms / Math.max(0.01, tnMs);
                File xmlFile = new File("weekly_load_report.xml");
                xmlExporter.export(xmlFile, from, to, parallelResult);
                updateProgress(3, 3);

                return String.format(
                        "Записів: %d | T1 = %.2f мс | Tn (%d потоків) = %.2f мс | Прискорення S = T1/Tn = %.2f | Звіт збережено у weekly_load_report.xml",
                        service.getWorkoutsSnapshot().size(), t1Ms, threads, tnMs, speedup
                );
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            progressBar.setProgress(1.0);
            statusLabel.setText(task.getValue());
            LOG.info(task.getValue());
            Alert info = new Alert(Alert.AlertType.INFORMATION, task.getValue(), ButtonType.OK);
            info.setTitle("Розрахунок навантаження та XML-звіт");
            info.setHeaderText("Результати порівняння потоків");
            info.showAndWait();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            progressBar.setProgress(0);
            LOG.severe("Помилка розрахунку або XML: " + task.getException().getMessage());
            showError("Помилка під час розрахунку: " + task.getException().getMessage());
        }));

        runInBackground(task, "load-benchmark");
    }

    // ШІ: Claude Code (Claude Opus) – спільний запуск фонової задачі: прив'язка ProgressBar до прогресу Task і потік-демон з назвою.
    // Змінено: адаптовано під модель Workout, перевірено обробку винятків та логування.
    private void runInBackground(Task<?> task, String threadName) {
        progressBar.progressProperty().bind(task.progressProperty());
        task.stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED || newState == Worker.State.FAILED
                    || newState == Worker.State.CANCELLED) {
                progressBar.progressProperty().unbind();
            }
        });
        Thread worker = new Thread(task, threadName);
        worker.setDaemon(true);
        worker.start();
    }

    private void refreshCollections() {
        masterAthletes.setAll(service.getAthletesSnapshot());
        masterWorkouts.setAll(service.getWorkoutsSnapshot());
        if (!masterAthletes.isEmpty() && athleteBox.getValue() == null) {
            athleteBox.getSelectionModel().selectFirst();
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setTitle("Помилка");
        alert.setHeaderText("Помилка виконання операції");
        alert.showAndWait();
    }
}
