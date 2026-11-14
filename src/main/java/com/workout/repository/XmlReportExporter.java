package com.workout.repository;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

// Експорт підсумкового звіту навантаження за період у XML через JAXB
public class XmlReportExporter {
    private static final Logger LOG = Logger.getLogger(XmlReportExporter.class.getName());

    @XmlRootElement(name = "weeklyLoadReport")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ReportRoot {
        private String period;
        @XmlElement(name = "athleteLoad")
        private List<AthleteEntry> entries = new ArrayList<>();

        public ReportRoot() {}

        public ReportRoot(String period, List<AthleteEntry> entries) {
            this.period = period;
            this.entries = entries;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AthleteEntry {
        private String athleteName;
        private double totalLoadScore;

        public AthleteEntry() {}

        public AthleteEntry(String athleteName, double totalLoadScore) {
            this.athleteName = athleteName;
            this.totalLoadScore = Math.round(totalLoadScore * 100.0) / 100.0;
        }
    }

    // ШІ: Claude Code (Claude Opus) – перевірка періоду і даних, сортування спортсменів за навантаженням, запис у лог.
    // Змінено: адаптовано під модель Workout, перевірено обробку винятків та логування.
    public void export(File file, LocalDate from, LocalDate to, Map<String, Double> loads) throws JAXBException {
        if (file == null || from == null || to == null || loads == null) {
            throw new IllegalArgumentException("Не вказано файл, період або дані для XML-звіту");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Початок періоду пізніший за його кінець");
        }
        List<AthleteEntry> entries = loads.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .map(e -> new AthleteEntry(e.getKey(), e.getValue()))
                .toList();

        ReportRoot root = new ReportRoot(from + " - " + to, entries);
        JAXBContext context = JAXBContext.newInstance(ReportRoot.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.marshal(root, file);
        LOG.info("XML-звіт збережено: " + file.getName() + ", спортсменів: " + entries.size());
    }
}
