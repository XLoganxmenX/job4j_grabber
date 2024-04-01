package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class HabrCareerParse implements Parse{

    private static final String SOURCE_LINK = "https://career.habr.com";
    public static final String PREFIX = "/vacancies?page=";
    public static final String SUFFIX = "&q=Java%20developer&type=all";
    public static final int MAX_PAGES = 5;
    private final DateTimeParser dateTimeParser;
    private int idCounter = 0;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    private String retrieveDescription(String link) {
        Connection linkConnection = Jsoup.connect(link);
        Document document = null;
        try {
            document = linkConnection.get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Objects.requireNonNull(document).select(".vacancy-description__text").text();
    }

    private List<Post> parsePage(String link) {
        List<Post> vacanciesFromPage = new LinkedList<>();

        Connection connection = Jsoup.connect(link);
        Document document = null;
        try {
            document = connection.get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Elements rows = Objects.requireNonNull(document).select(".vacancy-card__inner");

        rows.forEach(row -> {
            Element postDateElement = row.select(".vacancy-card__date").first();
            Element postDate = postDateElement.child(0);
            Element titleElement = row.select(".vacancy-card__title").first();
            Element linkElement = titleElement.child(0);
            String vacancyName = titleElement.text();
            String vacancyLink = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
            Post parsedPost = new Post(
                    idCounter++,
                    vacancyName,
                    vacancyLink,
                    retrieveDescription(vacancyLink),
                    dateTimeParser.parse(postDate.attr("datetime"))
            );

            System.out.println(parsedPost);
            vacanciesFromPage.add(parsedPost);
        });

        return vacanciesFromPage;
    }

    @Override
    public List<Post> list(String link) {
        List<Post> parsedVacancies = new LinkedList<>();

        for (int pageNumber = 1; pageNumber <= MAX_PAGES; pageNumber++) {
            String fullLink = "%s%s%d%s".formatted(link, PREFIX, pageNumber, SUFFIX);
            parsedVacancies.addAll(parsePage(fullLink));
        }
        return parsedVacancies;
    }

    public static void main(String[] args) {
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        habrCareerParse.list(SOURCE_LINK);
    }
}