package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

public class AlertRabbit {

    private Properties getPropertiesFrom(String file) {
        Properties config = new Properties();
        try (InputStream input = AlertRabbit.class.getClassLoader().getResourceAsStream(file)) {
            config.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return config;
    }

    public static void main(String[] args) {
        try {
            AlertRabbit alertRabbit = new AlertRabbit();
            int intervalInSecond = Integer.parseInt(
                    alertRabbit.getPropertiesFrom("rabbit.properties")
                               .getProperty("rabbit.interval")
            );

            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDetail job = newJob(Rabbit.class).build();
            SimpleScheduleBuilder times = simpleSchedule()
                    .withIntervalInSeconds(intervalInSecond)
                    .repeatForever();

            Trigger trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException se) {
            se.printStackTrace();
        }
    }

    public static class Rabbit implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            System.out.println("Rabbit runs here ...");
        }
    }
}