package com.devmh.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MigrationJobStarter implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final Job migrationJob;

    public MigrationJobStarter(JobLauncher jobLauncher, Job migrationJob) {
        this.jobLauncher = jobLauncher;
        this.migrationJob = migrationJob;
    }

    @Override
    public void run(String... args) throws Exception {
        jobLauncher.run(migrationJob, new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis()) // ensures uniqueness
                .toJobParameters());
    }
}
