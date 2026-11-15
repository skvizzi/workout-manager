# DESIGN – Менеджер спортивних тренувань

## 1. Специфікація

**Користувач:** тренер або спортсмен, який веде облік тренувань.

**Дані:**
- спортсмен (`Athlete`) – ім'я, вага 25–250 кг; ім'я унікальне без урахування регістру;
- вправа (`Exercise`) – назва, кількість підходів і повторень (більше 0);
- тренування (`Workout`) – ідентифікатор UUID, спортсмен, дата, тривалість 1–600 хв, інтенсивність 1–10, список вправ;
  три типи: `CardioWorkout` (дистанція 0–300 км), `StrengthWorkout` (тоннаж 0–100 000 кг), `StretchingWorkout` (ознака інвентарю).

**Дії користувача:**
- додати, редагувати, видалити спортсмена і тренування (CRUD);
- шукати тренування за ім'ям спортсмена, фільтрувати за типом і періодом дат, сортувати за будь-яким стовпцем таблиці (`FilteredList` + `SortedList`);
- імпортувати й експортувати дані в JSON (Jackson) у фоновому потоці;
- згенерувати 10 000 тестових записів у фоновому потоці;
- розрахувати тижневе навантаження спортсменів за період в одному потоці (T1) і на пулі потоків (Tn), отримати прискорення S = T1/Tn і XML-звіт `weekly_load_report.xml` (JAXB).

**Архітектура MVC:**

| Шар | Класи | Роль |
|---|---|---|
| Model | `LoadCalculable`, `Athlete`, `Exercise`, `Workout`, `CardioWorkout`, `StrengthWorkout`, `StretchingWorkout` | доменні дані, перевірка значень у сеттерах, поліморфний розрахунок калорій |
| Service | `WorkoutService`, `DataGenerator` | CRUD у пам'яті під `ReentrantLock`, розрахунок навантаження в `ExecutorService`, тестові дані |
| Repository | `JsonStorage`, `StorageData`, `XmlReportExporter` | JSON-файл даних, XML-звіт |
| Controller | `MainController` | обробка подій, фонові `Task`, оновлення інтерфейсу |
| View | `main-view.fxml`, `MainApp`, `Launcher` | вікно з таблицею, формами і `ProgressBar` |

**ООП:** абстракція – абстрактний клас `Workout` і інтерфейс `LoadCalculable`; наслідування – три типи тренувань від `Workout`;
поліморфізм – `calculateCalories()` і `getTypeName()` викликаються через змінну типу `Workout` у таблиці та в `WorkoutService.computeLoadIndex()`;
інкапсуляція – приватні поля, перевірка в сеттерах з `IllegalArgumentException`.

**Структура JSON** (`trainings.json`):

```json
{
  "athletes": [ { "name": "Анна Мельник", "weightKg": 64.0 } ],
  "workouts": [
    {
      "workoutType": "CARDIO",
      "id": "6f1c…",
      "athlete": { "name": "Анна Мельник", "weightKg": 64.0 },
      "date": "2026-11-10",
      "durationMinutes": 45,
      "intensity": 7,
      "exercises": [ { "title": "Інтервальний біг", "sets": 3, "reps": 12 } ],
      "distanceKm": 8.0
    }
  ]
}
```

Поле `workoutType` (`CARDIO`, `STRENGTH`, `STRETCHING`) визначає клас під час читання (`@JsonTypeInfo`, `@JsonSubTypes`).

Кожне тренування зберігає в JSON власну копію спортсмена. Після читання `JsonStorage.linkWorkoutsToAthletes()` замінює ці копії об'єктами зі списку `athletes` (порівняння імені без урахування регістру), а відсутніх спортсменів додає до списку. Тому перейменування і видалення спортсмена після перезапуску застосунку змінює всі його тренування.

**Структура XML** (`weekly_load_report.xml`):

```xml
<weeklyLoadReport>
    <period>2026-09-15 - 2026-11-14</period>
    <athleteLoad>
        <athleteName>Ірина Кравченко</athleteName>
        <totalLoadScore>877.45</totalLoadScore>
    </athleteLoad>
</weeklyLoadReport>
```

**Багатопоточність:**
- фонові задачі `javafx.concurrent.Task` у `MainController`: імпорт JSON, експорт JSON, генерація 10 000 записів, розрахунок T1/Tn із записом XML;
- `runInBackground()` запускає задачу в потоці-демоні та прив'язує `ProgressBar` до `task.progressProperty()`;
- спільні дані – списки `athletes` і `workouts` у `WorkoutService`, їх захищає `ReentrantLock lock`; у `calculateLoadParallel()` спільна карта `sharedTotals` захищена окремим `ReentrantLock mergeLock`;
- інтерфейс оновлюється лише в потоці JavaFX: обробники `setOnSucceeded` / `setOnFailed` і `Platform.runLater()`;
- помилки фонових задач перехоплює `setOnFailed`: запис `LOG.severe` у журнал і вікно з повідомленням.

## 2. UML-діаграма класів

### PlantUML

```plantuml
@startuml
skinparam classAttributeIconSize 0

package com.workout.model {
  interface LoadCalculable <<interface>> {
    +calculateCalories() : double
    +calculateWeeklyLoadScore() : double
  }

  abstract class Workout {
    -id : String
    -athlete : Athlete
    -date : LocalDate
    -durationMinutes : int
    -intensity : int
    -exercises : List<Exercise>
    #Workout()
    #Workout(athlete : Athlete, date : LocalDate, durationMinutes : int, intensity : int, exercises : List<Exercise>)
    +{abstract} getTypeName() : String
    +calculateWeeklyLoadScore() : double
    +getId() : String
    +setId(id : String) : void
    +getAthlete() : Athlete
    +setAthlete(athlete : Athlete) : void
    +getDate() : LocalDate
    +setDate(date : LocalDate) : void
    +getDurationMinutes() : int
    +setDurationMinutes(durationMinutes : int) : void
    +getIntensity() : int
    +setIntensity(intensity : int) : void
    +getExercises() : List<Exercise>
    +setExercises(exercises : List<Exercise>) : void
  }

  class CardioWorkout {
    -distanceKm : double
    +getTypeName() : String
    +calculateCalories() : double
    +getDistanceKm() : double
    +setDistanceKm(distanceKm : double) : void
  }

  class StrengthWorkout {
    -liftedWeightKg : double
    +getTypeName() : String
    +calculateCalories() : double
    +getLiftedWeightKg() : double
    +setLiftedWeightKg(liftedWeightKg : double) : void
  }

  class StretchingWorkout {
    -useEquipment : boolean
    +getTypeName() : String
    +calculateCalories() : double
    +isUseEquipment() : boolean
    +setUseEquipment(useEquipment : boolean) : void
  }

  class Athlete {
    -name : String
    -weightKg : double
    +Athlete(name : String, weightKg : double)
    +getName() : String
    +setName(name : String) : void
    +getWeightKg() : double
    +setWeightKg(weightKg : double) : void
    +toString() : String
    +equals(o : Object) : boolean
    +hashCode() : int
  }

  class Exercise {
    -title : String
    -sets : int
    -reps : int
    +Exercise(title : String, sets : int, reps : int)
    +getTitle() : String
    +setTitle(title : String) : void
    +getSets() : int
    +setSets(sets : int) : void
    +getReps() : int
    +setReps(reps : int) : void
    +toString() : String
  }
}

package com.workout.service {
  class WorkoutService {
    -athletes : List<Athlete>
    -workouts : List<Workout>
    -lock : ReentrantLock
    +replaceAll(newAthletes : List<Athlete>, newWorkouts : List<Workout>) : void
    +addAthlete(athlete : Athlete) : void
    +updateAthlete(target : Athlete, newName : String, newWeight : double) : void
    +removeAthlete(target : Athlete) : void
    +addWorkout(workout : Workout) : void
    +updateWorkout(id : String, updated : Workout) : void
    +removeWorkout(workout : Workout) : void
    +getAthletesSnapshot() : List<Athlete>
    +getWorkoutsSnapshot() : List<Workout>
    +calculateLoadSingleThread(from : LocalDate, to : LocalDate) : Map<String, Double>
    +calculateLoadParallel(from : LocalDate, to : LocalDate, threads : int) : Map<String, Double>
    -computeLoadIndex(w : Workout) : double
  }

  class DataGenerator {
    -{static} MAX_COUNT : int
    -{static} DAYS_BACK : int
    -{static} SEED : long
    -{static} NAMES : String[]
    -{static} EXERCISES : String[]
    +{static} generate(count : int) : StorageData
  }
}

package com.workout.repository {
  class StorageData {
    -athletes : List<Athlete>
    -workouts : List<Workout>
    +getAthletes() : List<Athlete>
    +setAthletes(athletes : List<Athlete>) : void
    +getWorkouts() : List<Workout>
    +setWorkouts(workouts : List<Workout>) : void
  }

  class JsonStorage {
    -{static} LOG : Logger
    -mapper : ObjectMapper
    +save(file : File, data : StorageData) : void
    +load(file : File) : StorageData
    -linkWorkoutsToAthletes(data : StorageData) : void
  }

  class XmlReportExporter {
    -{static} LOG : Logger
    +export(file : File, from : LocalDate, to : LocalDate, loads : Map<String, Double>) : void
  }

  class "XmlReportExporter.ReportRoot" as ReportRoot {
    -period : String
    -entries : List<AthleteEntry>
  }

  class "XmlReportExporter.AthleteEntry" as AthleteEntry {
    -athleteName : String
    -totalLoadScore : double
  }
}

package com.workout.controller {
  class MainController {
    -{static} LOG : Logger
    -service : WorkoutService
    -jsonStorage : JsonStorage
    -xmlExporter : XmlReportExporter
    -masterWorkouts : ObservableList<Workout>
    -masterAthletes : ObservableList<Athlete>
    -filteredWorkouts : FilteredList<Workout>
    -progressBar : ProgressBar
    -statusLabel : Label
    +initialize() : void
    -initDefaultJsonFile() : void
    -setupTable() : void
    -setupFilters() : void
    -setupSelectionListeners() : void
    -applyFilters() : void
    -onResetFilters() : void
    -onAddWorkout() : void
    -onUpdateWorkout() : void
    -onDeleteWorkout() : void
    -onAddAthlete() : void
    -onUpdateAthlete() : void
    -onDeleteAthlete() : void
    -onImportJson() : void
    -onExportJson() : void
    -onGenerate10k() : void
    -onBenchmarkAndXmlReport() : void
    -runInBackground(task : Task<?>, threadName : String) : void
    -buildWorkoutFromForm() : Workout
    -refreshCollections() : void
    -showError(msg : String) : void
  }
}

package com.workout {
  class MainApp {
    +start(stage : Stage) : void
  }
  class Launcher {
    +{static} main(args : String[]) : void
  }
}

LoadCalculable <|.. Workout
Workout <|-- CardioWorkout
Workout <|-- StrengthWorkout
Workout <|-- StretchingWorkout
Workout "0..*" --> "1" Athlete : athlete
Workout "1" *-- "0..*" Exercise : exercises

WorkoutService "1" o-- "0..*" Athlete
WorkoutService "1" o-- "0..*" Workout
StorageData "1" o-- "0..*" Athlete
StorageData "1" o-- "0..*" Workout
ReportRoot "1" *-- "0..*" AthleteEntry

JsonStorage ..> StorageData : читає / пише
XmlReportExporter ..> ReportRoot : створює
DataGenerator ..> StorageData : створює

MainController "1" --> "1" WorkoutService
MainController "1" --> "1" JsonStorage
MainController "1" --> "1" XmlReportExporter
MainController ..> DataGenerator
MainApp ..> MainController : FXML
Launcher ..> MainApp
@enduml
```

### Mermaid

```mermaid
classDiagram
    direction TB

    class LoadCalculable {
        <<interface>>
        +calculateCalories() double
        +calculateWeeklyLoadScore() double
    }

    class Workout {
        <<abstract>>
        -String id
        -Athlete athlete
        -LocalDate date
        -int durationMinutes
        -int intensity
        -List~Exercise~ exercises
        +getTypeName()* String
        +calculateWeeklyLoadScore() double
        +getId() String
        +setId(String id) void
        +getAthlete() Athlete
        +setAthlete(Athlete athlete) void
        +getDate() LocalDate
        +setDate(LocalDate date) void
        +getDurationMinutes() int
        +setDurationMinutes(int durationMinutes) void
        +getIntensity() int
        +setIntensity(int intensity) void
        +getExercises() List~Exercise~
        +setExercises(List~Exercise~ exercises) void
    }

    class CardioWorkout {
        -double distanceKm
        +getTypeName() String
        +calculateCalories() double
        +getDistanceKm() double
        +setDistanceKm(double distanceKm) void
    }

    class StrengthWorkout {
        -double liftedWeightKg
        +getTypeName() String
        +calculateCalories() double
        +getLiftedWeightKg() double
        +setLiftedWeightKg(double liftedWeightKg) void
    }

    class StretchingWorkout {
        -boolean useEquipment
        +getTypeName() String
        +calculateCalories() double
        +isUseEquipment() boolean
        +setUseEquipment(boolean useEquipment) void
    }

    class Athlete {
        -String name
        -double weightKg
        +getName() String
        +setName(String name) void
        +getWeightKg() double
        +setWeightKg(double weightKg) void
        +toString() String
        +equals(Object o) boolean
        +hashCode() int
    }

    class Exercise {
        -String title
        -int sets
        -int reps
        +getTitle() String
        +setTitle(String title) void
        +getSets() int
        +setSets(int sets) void
        +getReps() int
        +setReps(int reps) void
        +toString() String
    }

    class WorkoutService {
        -List~Athlete~ athletes
        -List~Workout~ workouts
        -ReentrantLock lock
        +replaceAll(List~Athlete~ newAthletes, List~Workout~ newWorkouts) void
        +addAthlete(Athlete athlete) void
        +updateAthlete(Athlete target, String newName, double newWeight) void
        +removeAthlete(Athlete target) void
        +addWorkout(Workout workout) void
        +updateWorkout(String id, Workout updated) void
        +removeWorkout(Workout workout) void
        +getAthletesSnapshot() List~Athlete~
        +getWorkoutsSnapshot() List~Workout~
        +calculateLoadSingleThread(LocalDate from, LocalDate to) Map~String,Double~
        +calculateLoadParallel(LocalDate from, LocalDate to, int threads) Map~String,Double~
        -computeLoadIndex(Workout w) double
    }

    class DataGenerator {
        -int MAX_COUNT$
        -int DAYS_BACK$
        -long SEED$
        -String[] NAMES$
        -String[] EXERCISES$
        +generate(int count)$ StorageData
    }

    class StorageData {
        -List~Athlete~ athletes
        -List~Workout~ workouts
        +getAthletes() List~Athlete~
        +setAthletes(List~Athlete~ athletes) void
        +getWorkouts() List~Workout~
        +setWorkouts(List~Workout~ workouts) void
    }

    class JsonStorage {
        -Logger LOG$
        -ObjectMapper mapper
        +save(File file, StorageData data) void
        +load(File file) StorageData
        -linkWorkoutsToAthletes(StorageData data) void
    }

    class XmlReportExporter {
        -Logger LOG$
        +export(File file, LocalDate from, LocalDate to, Map~String,Double~ loads) void
    }

    class ReportRoot {
        -String period
        -List~AthleteEntry~ entries
    }

    class AthleteEntry {
        -String athleteName
        -double totalLoadScore
    }

    class MainController {
        -Logger LOG$
        -WorkoutService service
        -JsonStorage jsonStorage
        -XmlReportExporter xmlExporter
        -ObservableList~Workout~ masterWorkouts
        -ObservableList~Athlete~ masterAthletes
        -FilteredList~Workout~ filteredWorkouts
        -ProgressBar progressBar
        -Label statusLabel
        +initialize() void
        -initDefaultJsonFile() void
        -setupTable() void
        -setupFilters() void
        -setupSelectionListeners() void
        -applyFilters() void
        -onResetFilters() void
        -onAddWorkout() void
        -onUpdateWorkout() void
        -onDeleteWorkout() void
        -onAddAthlete() void
        -onUpdateAthlete() void
        -onDeleteAthlete() void
        -onImportJson() void
        -onExportJson() void
        -onGenerate10k() void
        -onBenchmarkAndXmlReport() void
        -runInBackground(Task task, String threadName) void
        -buildWorkoutFromForm() Workout
        -refreshCollections() void
        -showError(String msg) void
    }

    class MainApp {
        +start(Stage stage) void
    }

    class Launcher {
        +main(String[] args)$ void
    }

    LoadCalculable <|.. Workout
    Workout <|-- CardioWorkout
    Workout <|-- StrengthWorkout
    Workout <|-- StretchingWorkout
    Workout "0..*" --> "1" Athlete : athlete
    Workout "1" *-- "0..*" Exercise : exercises
    WorkoutService "1" o-- "0..*" Athlete
    WorkoutService "1" o-- "0..*" Workout
    StorageData "1" o-- "0..*" Athlete
    StorageData "1" o-- "0..*" Workout
    ReportRoot "1" *-- "0..*" AthleteEntry
    JsonStorage ..> StorageData
    XmlReportExporter ..> ReportRoot
    DataGenerator ..> StorageData
    MainController "1" --> "1" WorkoutService
    MainController "1" --> "1" JsonStorage
    MainController "1" --> "1" XmlReportExporter
    MainController ..> DataGenerator
    MainApp ..> MainController
    Launcher ..> MainApp
```

`ReportRoot` і `AthleteEntry` – вкладені статичні класи `XmlReportExporter`. Конструктори без параметрів, які потрібні Jackson і JAXB, на діаграмі не показано. У `MainController` з полів `@FXML` показано лише `progressBar` і `statusLabel`, решта – поля форми і стовпці таблиці.

Де відрендерити: planttext.com або plantuml.com (PlantUML), mermaid.live (Mermaid), draw.io: Arrange → Insert → Advanced → PlantUML / Mermaid.

## 3. Блок-схеми (ДСТУ ISO 5807:2016)

Позначення в Mermaid: `([ ])` – термінатор, `[ ]` – процес, `{ }` – рішення, `[/ /]` – введення/виведення даних, `[[ ]]` – визначений процес (виклик методу), `{{ }}` – підготовка.
Mermaid не має символу межі циклу і не контролює перетини ліній. Фінальний рисунок для записки краще перемалювати в draw.io (бібліотека «Блок-схема»): цикли позначити межами циклу «Цикл по частинах» і «Цикл по тренуваннях», лінії повернення вгору – зі стрілками.

### 3.1 `WorkoutService.calculateLoadParallel(from, to, threads)`

Алгоритм:
1. Отримати копію списку тренувань `snapshot` через `getWorkoutsSnapshot()` (під блокуванням `lock`).
2. Якщо список порожній – повернути порожню карту.
3. Обчислити розмір частини `chunkSize = max(1, ⌈n / threads⌉)`, створити список задач `tasks`, спільну карту `sharedTotals` і блокування `mergeLock`.
4. Для кожної частини `snapshot.subList(i, min(i + chunkSize, n))` додати в `tasks` задачу `Callable`. Задача рахує власну карту `localPart`: для кожного тренування в межах періоду `[from; to]` додає до суми спортсмена значення `computeLoadIndex(w)`. Потім задача бере `mergeLock`, додає `localPart` у `sharedTotals` і знімає блокування в `finally`.
5. Створити пул `Executors.newFixedThreadPool(threads)`, запустити всі задачі `invokeAll(tasks)`.
6. Для кожного `Future` викликати `get()`: так виняток із задачі потрапляє у викликача (`ExecutionException`).
7. У `finally` закрити пул `pool.shutdown()`.
8. Повернути `sharedTotals`.

```mermaid
flowchart TD
    A([Початок]) --> B[[snapshot = getWorkoutsSnapshot]]
    B --> C{snapshot порожній?}
    C -- так --> D[/Повернути порожню карту/]
    D --> Z([Кінець])
    C -- ні --> E{{chunkSize = max 1, n / threads з округленням угору; tasks, sharedTotals, mergeLock; i = 0}}
    E --> F{i < n?}
    F -- так --> G[sub = snapshot.subList i, min i + chunkSize, n]
    G --> H[Додати задачу Callable для sub у tasks]
    H --> I[i = i + chunkSize]
    I --> F
    F -- ні --> J[pool = newFixedThreadPool threads]
    J --> K[[pool.invokeAll tasks]]
    K --> L[[f.get для кожного Future]]
    L --> M[pool.shutdown]
    M --> N[/Повернути sharedTotals/]
    N --> Z

    subgraph T [Задача Callable для частини sub]
        T1([Початок задачі]) --> T2{{localPart = нова HashMap}}
        T2 --> T3{Є ще тренування w у sub?}
        T3 -- так --> T4{дата w у межах from..to?}
        T4 -- так --> T5[[score = computeLoadIndex w]]
        T5 --> T6[localPart.merge ім'я спортсмена, score]
        T6 --> T3
        T4 -- ні --> T3
        T3 -- ні --> T7[mergeLock.lock]
        T7 --> T8[Додати localPart у sharedTotals]
        T8 --> T9[mergeLock.unlock]
        T9 --> T10([Кінець задачі])
    end
```

### 3.2 `MainController.onImportJson()`

Алгоритм (потік JavaFX і фоновий потік):
1. Показати `FileChooser` з фільтром `*.json`.
2. Якщо файл не обрано – вийти.
3. Вивести в рядок стану «Імпорт JSON...».
4. Створити `Task<StorageData>`, призначити обробники `setOnSucceeded` і `setOnFailed`.
5. Викликати `runInBackground(task, "import-json")`: `ProgressBar` прив'язується до прогресу задачі, задача стартує в потоці-демоні.
6. У фоновому потоці: `updateProgress(0, 2)` → `jsonStorage.load(file)` (перевірка порожнього файлу, синтаксису і структури) → `updateProgress(1, 2)` → `service.replaceAll(...)` під блокуванням `lock` → `updateProgress(2, 2)`.
7. Успіх: у потоці JavaFX через `Platform.runLater()` оновити колекції таблиці, встановити прогрес 1.0, вивести кількість записів, записати `LOG.info`.
8. Помилка: у потоці JavaFX через `Platform.runLater()` встановити прогрес 0, записати `LOG.severe`, показати вікно з помилкою. Дані в пам'яті не змінюються, бо `replaceAll` не виконувався.

```mermaid
flowchart TD
    A([Початок]) --> B[/Показати FileChooser, отримати file/]
    B --> C{file == null?}
    C -- так --> Z([Кінець])
    C -- ні --> D[/statusLabel: Імпорт JSON.../]
    D --> E[Створити Task, призначити setOnSucceeded і setOnFailed]
    E --> F[[runInBackground task, import-json]]
    F --> Z

    subgraph BG [Фоновий потік import-json]
        G([Початок call]) --> H[updateProgress 0, 2]
        H --> I[[jsonStorage.load file]]
        I --> J{Виняток під час читання?}
        J -- ні --> K[updateProgress 1, 2]
        K --> L[[service.replaceAll athletes, workouts]]
        L --> M[updateProgress 2, 2]
        M --> N([Стан SUCCEEDED])
        J -- так --> O([Стан FAILED])
    end

    subgraph UI [Потік JavaFX, Platform.runLater]
        N --> P[[refreshCollections]]
        P --> Q[/progressBar = 1.0, кількість записів у statusLabel, LOG.info/]
        O --> R[/progressBar = 0, LOG.severe, вікно з помилкою/]
    end
```
