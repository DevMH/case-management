package com.devmh.config;

import com.devmh.model.Case;
import com.devmh.model.LegacyCase;
import com.devmh.persistence.CaseRepository;
import com.devmh.persistence.LegacyCaseMapper;
import com.devmh.persistence.LegacyCaseRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.NonNull;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.PlatformTransactionManager;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
@RequiredArgsConstructor
class MigrationBatchConfig {

    private final LegacyCaseRepository legacyRepo;

    private final CaseRepository caseRepo;

    private final LegacyCaseMapper mapper;

    private final EntityManagerFactory entityManagerFactory;

    private final JavaMailSender mailSender;

    @Value("${migration.log.path:migration-errors.log}")
    private String logFilePath;

    @Value("${migration.alert.email.to:admin@example.com}")
    private String alertEmailTo;

    @Value("${migration.alert.email.from:no-reply@example.com}")
    private String alertEmailFrom;

    @Bean
    public Job migrationJob(JobRepository jobRepository, Step migrationStep) {
        return new JobBuilder("migrationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobExecutionListener())
                .start(migrationStep)
                .build();
    }

    @Bean
    public Step migrationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("migrationStep", jobRepository)
                .<LegacyCase, Case>chunk(100, transactionManager)
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .faultTolerant()
                .skipPolicy(customSkipPolicy())
                .listener(loggingSkipListener())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public JpaPagingItemReader<LegacyCase> itemReader() {
        JpaPagingItemReader<LegacyCase> reader = new JpaPagingItemReader<>();
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setQueryString("SELECT l FROM LegacyCase l");
        reader.setPageSize(100);
        reader.setSaveState(true);
        reader.setName("legacyCaseReader");
        return reader;
    }

    @Bean
    public ItemProcessor<LegacyCase, Case> itemProcessor() {
        return mapper::toElasticsearchCase;
    }

    @Bean
    public RepositoryItemWriter<Case> itemWriter() {
        return new RepositoryItemWriterBuilder<Case>()
                .repository(caseRepo)
                .methodName("save")
                .build();
    }

    @Bean
    public SkipListener<LegacyCase, Case> loggingSkipListener() {
        return new SkipListener<>() {
            @Override
            public void onSkipInRead(@NonNull Throwable t) {
                logToFile("read", null, t);
            }

            @Override
            public void onSkipInWrite(@NonNull Case item, @NonNull Throwable t) {
                logToFile("write", item, t);
            }

            @Override
            public void onSkipInProcess(@NonNull LegacyCase item, @NonNull Throwable t) {
                logToFile("process", item, t);
            }

            private void logToFile(String phase, Object item, Throwable t) {
                try (PrintWriter writer = new PrintWriter(new FileWriter(logFilePath, true))) {
                    writer.printf("[SKIP][%s] Item: %s, Error: %s%n", phase.toUpperCase(), item, t.getMessage());
                } catch (IOException e) {
                    System.err.println("Could not write to log: " + e.getMessage());
                }
            }
        };
    }

    @Bean
    public SkipPolicy customSkipPolicy() {
        return (throwable, skipCount) -> true;
    }

    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("migration-task-async");
        /*
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("MigrationTask-");
        executor.initialize();
        return executor;
         */
    }

    @Bean
    public JobExecutionListener jobExecutionListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(@NonNull JobExecution jobExecution) {}

            @Override
            public void afterJob(@NonNull JobExecution jobExecution) {
                if (jobExecution.getStatus().isUnsuccessful()) {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(alertEmailFrom);
                    message.setTo(alertEmailTo);
                    message.setSubject("[ALERT] Data Migration Failed");
                    message.setText("The data migration job failed with status: " + jobExecution.getStatus());
                    try {
                        mailSender.send(message);
                    } catch (Exception e) {
                        System.err.println("Failed to send alert email: " + e.getMessage());
                    }
                }
            }
        };
    }
}
