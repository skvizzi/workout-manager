# Менеджер спортивних тренувань

Курсова робота з дисципліни «Об'єктно-орієнтоване програмування», ВСП «ОТФК ОНТУ».
Виконав здобувач освіти групи 3РП-11 Ковальський Олександр Сергійович.

Настільний застосунок для обліку спортсменів і тренувань трьох типів: кардіо, силові, розтяжка.
Застосунок рахує витрачені калорії для кожного тренування, тижневе навантаження спортсменів
за обраний період (в одному потоці і на пулі потоків із заміром прискорення S = T1/Tn),
зберігає дані в JSON і формує звіт навантаження в XML.

## Стек

- Java 25, JavaFX 25 (FXML), Maven
- Jackson (JSON), JAXB (XML)
- `javafx.concurrent.Task`, `ExecutorService`, `ReentrantLock`
- `java.util.logging` (журнал `workout_manager.log`)

## Запуск

Потрібні JDK 25 і Maven 3.9+. З кореня проєкту:

```bash
mvn javafx:run
```

Під час першого запуску застосунок створює `trainings.json` з 60 тестовими записами.
Кнопка «10 000 записів» генерує великий набір у фоновому потоці, кнопка
«Розрахунок T1/Tn + XML» вимірює час розрахунку і зберігає `weekly_load_report.xml`.

## Тестові файли з пошкодженими даними (`data/`)

Тека `data/` містить три файли для перевірки обробки помилок під час імпорту
(кнопка «Імпорт JSON», метод `JsonStorage.load()`). Жоден з них не змінює поточних даних:
імпорт завершується повідомленням про помилку, а записи в таблицях залишаються попередніми.

| Файл | Що пошкоджено | Що відбувається |
|---|---|---|
| `broken_syntax.json` | обірваний JSON: немає `]` і `}` у кінці | Jackson кидає `JsonProcessingException`, `load()` перетворює його на `IOException` «Файл не відповідає формату JSON застосунку: Unexpected end-of-input…» |
| `invalid_values.json` | синтаксис правильний, але `intensity` = 25, `durationMinutes` = -10 | сеттер `Workout.setIntensity()` кидає `IllegalArgumentException`, Jackson обгортає його в `JsonMappingException`, повідомлення: «…Інтенсивність має бути від 1 до 10» |
| `missing_athlete.json` | у тренуванні немає поля `athlete` | перевірка в `load()` дає `IOException` «У файлі є тренування без спортсмена або дати» |

Кожна помилка також пишеться в журнал (`LOG.warning` для синтаксису і значень).

## Структура

```
src/main/java/com/workout/
  Launcher.java, MainApp.java   запуск JavaFX
  model/                        LoadCalculable, Athlete, Exercise, Workout (abstract),
                                CardioWorkout, StrengthWorkout, StretchingWorkout
  service/                      WorkoutService (CRUD, розрахунок навантаження), DataGenerator
  repository/                   JsonStorage, StorageData, XmlReportExporter
  controller/                   MainController
src/main/resources/com/workout/main-view.fxml
data/                           тестові файли з пошкодженими даними
```

Специфікація, UML-діаграма класів і блок-схеми – у `DESIGN.md`,
журнал використання ШІ – у `AI_LOG.md`.
