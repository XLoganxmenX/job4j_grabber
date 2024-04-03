package ru.job4j.grabber;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Grabber implements Grab {
    private Properties cfg;

    public Grabber() { }


    @Override
    public void init(Parse parse, Store store, Scheduler scheduler) throws SchedulerException {
        int time = Integer.parseInt(cfg.getProperty("time"));

        JobDataMap data = new JobDataMap();
        data.put("store", store);
        data.put("parse", parse);
        JobDetail job = newJob(GrabJob.class)
                .usingJobData(data)
                .build();
        SimpleScheduleBuilder times = simpleSchedule()
                .withIntervalInSeconds(time)
                .repeatForever();
        Trigger trigger = newTrigger()
                .startNow()
                .withSchedule(times)
                .build();
        scheduler.scheduleJob(job, trigger);
    }

    public void web(Store store) {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(Integer.parseInt(cfg.getProperty("port")))) {
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    try (OutputStream out = socket.getOutputStream()) {
                        out.write("HTTP/1.1 200 OK\r\n\r\n".getBytes(StandardCharsets.UTF_8));

                        out.write("<html lang=\"ru\"><head><meta charset=\"utf-8\"></head><body>"
                                .getBytes(StandardCharsets.UTF_8));
                        for (Post post : store.getAll()) {
                            out.write("<div><p>".getBytes(StandardCharsets.UTF_8));
                            out.write(postToHTML(post).getBytes(StandardCharsets.UTF_8));
                            out.write("</p></div>".getBytes(StandardCharsets.UTF_8));
                        }

                        out.write("</body></head>".getBytes(StandardCharsets.UTF_8));
                    } catch (IOException io) {
                        io.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String postToHTML(Post post) {
        return String.format("%s<br /><p style=\"font-size: 150%%;\">%s</p><a href=\"%s\">%s</a><br />%s<br />",
                post.getCreated(), post.getTitle(), post.getLink(), post.getLink(), post.getDescription());
    }

    public void cfg() throws IOException {
        var config = new Properties();
        try (InputStream input = Grabber.class.getClassLoader()
                .getResourceAsStream("db/rabbit.properties")) {
            config.load(input);
            cfg = config;
        }
    }

    public Scheduler scheduler() throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        return scheduler;
    }

    public Store store() {
        return new PsqlStore(cfg);
    }

    public static class GrabJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            JobDataMap map = context.getJobDetail().getJobDataMap();
            Store store = (Store) map.get("store");
            Parse parse = (Parse) map.get("parse");

            List<Post> parsedPosts = parse.list("https://career.habr.com");
            parsedPosts.forEach(store::save);
        }
    }

    public static void main(String[] args) throws Exception {
        Grabber grab = new Grabber();
        grab.cfg();
        Scheduler scheduler = grab.scheduler();
        Store store = grab.store();
        grab.init(new HabrCareerParse(new HabrCareerDateTimeParser()), store, scheduler);
        grab.web(store);
    }
}