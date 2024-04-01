package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.util.Objects;

public class HabrCareerParse {

    private static final String SOURCE_LINK = "https://career.habr.com";
    public static final String PREFIX = "/vacancies?page=";
    public static final String SUFFIX = "&q=Java%20developer&type=all";

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

    public static void main(String[] args) throws IOException {
        HabrCareerDateTimeParser timeParser = new HabrCareerDateTimeParser();
        HabrCareerParse habrCareerParse = new HabrCareerParse();

        for (int pageNumber = 1; pageNumber <= 1; pageNumber++) {

            String fullLink = "%s%s%d%s".formatted(SOURCE_LINK, PREFIX, pageNumber, SUFFIX);
            Connection connection = Jsoup.connect(fullLink);
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");

            rows.forEach(row -> {
                Element postDateElement = row.select(".vacancy-card__date").first();
                Element postDate = postDateElement.child(0);
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                String vacancyName = titleElement.text();
                String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));

                System.out.printf("%s %s %s %s%n",
                        timeParser.parse(postDate.attr("datetime")),
                        vacancyName,
                        link,
                        habrCareerParse.retrieveDescription(link)
                );
            });
        }

    }
}