package com.workout.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.workout.model.Workout;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

// Читання та збереження набору тренувань у форматі JSON через Jackson
public class JsonStorage {
    private static final Logger LOG = Logger.getLogger(JsonStorage.class.getName());

    private final ObjectMapper mapper;

    public JsonStorage() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    // ШІ: Claude Code (Claude Opus) – атомарне збереження через тимчасовий файл і перевірка вхідних даних.
    // Змінено: адаптовано під модель Workout, перевірено обробку винятків та логування.
    public void save(File file, StorageData data) throws IOException {
        if (file == null || data == null) {
            throw new IOException("Не вказано файл або дані для збереження");
        }
        Path target = file.toPath().toAbsolutePath();
        Path tempFile = target.resolveSibling(target.getFileName() + ".tmp");
        mapper.writeValue(tempFile.toFile(), data);
        Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        LOG.info("Збережено у JSON: " + target + ", тренувань: " + data.getWorkouts().size());
    }

    // ШІ: Claude Code (Claude Opus) – зрозуміле повідомлення про синтаксичну помилку і перевірка кожного запису.
    // Змінено: адаптовано під модель Workout, перевірено обробку винятків та логування.
    public StorageData load(File file) throws IOException {
        if (file == null || !file.exists() || file.length() == 0) {
            throw new IOException("Файл не знайдено або він порожній");
        }
        StorageData data;
        try {
            data = mapper.readValue(file, StorageData.class);
        } catch (JsonProcessingException e) {
            LOG.warning("Пошкоджений JSON " + file.getName() + ": " + e.getOriginalMessage());
            throw new IOException("Файл не відповідає формату JSON застосунку: " + e.getOriginalMessage(), e);
        }
        if (data == null || data.getWorkouts() == null || data.getAthletes() == null) {
            throw new IOException("Некоректна структура JSON-файлу");
        }
        for (Workout workout : data.getWorkouts()) {
            if (workout == null || workout.getAthlete() == null || workout.getDate() == null) {
                throw new IOException("У файлі є тренування без спортсмена або дати");
            }
        }
        LOG.info("Прочитано з JSON: " + file.getName() + ", тренувань: " + data.getWorkouts().size());
        return data;
    }
}
