package ru.job4j.grabber;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store {

    private Connection connection;

    public PsqlStore(Properties config) {
        try {
            Class.forName(config.getProperty("driver-class-name"));
            connection = DriverManager.getConnection(
                    config.getProperty("url"),
                    config.getProperty("username"),
                    config.getProperty("password")
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO grabber_service.post(name, text, link, created) VALUES(?, ?, ?, ?)"
                        + "ON CONFLICT(link) DO NOTHING;",
                Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getDescription());
            statement.setString(3, post.getLink());
            statement.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            statement.execute();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    post.setId(generatedKeys.getInt(1));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> postFromDB = null;
        try (PreparedStatement statement =
                     connection.prepareStatement("SELECT * FROM grabber_service.post")) {

            try (ResultSet resultSet = statement.executeQuery()) {
                postFromDB = extractPost(resultSet);
            }
        } catch (SQLException | ElementNotFoundException e) {
            e.printStackTrace();
        }

        return postFromDB;
    }

    @Override
    public Post findById(int id) {
        Post postFromDB = null;
        try (PreparedStatement statement =
                     connection.prepareStatement("SELECT * FROM grabber_service.post WHERE id = ?")) {
            statement.setInt(1, id);

            try (ResultSet resultItem = statement.executeQuery()) {
                postFromDB = extractPost(resultItem).get(0);
            }
        } catch (SQLException | ElementNotFoundException e) {
            e.printStackTrace();
        }
        return postFromDB;
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    private List<Post> extractPost(ResultSet queryResult) throws SQLException, ElementNotFoundException {
        List<Post> postsFromDB = null;
        if (!queryResult.next()) {
            throw new ElementNotFoundException("Items not found");
        } else {
            postsFromDB = new LinkedList<>();
            do {
                postsFromDB.add(createPost(queryResult));
            } while (queryResult.next());
        }
        return postsFromDB;
    }

    private Post createPost(ResultSet queryResult) throws SQLException {
        return new Post(
                queryResult.getInt("id"),
                queryResult.getString("name"),
                queryResult.getString("link"),
                queryResult.getString("text"),
                queryResult.getTimestamp("created").toLocalDateTime()
        );
    }
}