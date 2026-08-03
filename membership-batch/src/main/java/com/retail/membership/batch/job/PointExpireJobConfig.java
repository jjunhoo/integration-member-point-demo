package com.retail.membership.batch.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 만료 lot 잔여를 정리하는 Spring Batch Job (chunk oriented).
 *
 * <ul>
 *   <li>Reader — 만료 대상 userId</li>
 *   <li>Processor — {@code expirePointsForUser}</li>
 *   <li>Writer — 청크 집계 로그</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class PointExpireJobConfig {

    private static final int CHUNK_SIZE = 50;

    private final PointExpireUserReader pointExpireUserReader;
    private final PointExpireProcessor pointExpireProcessor;
    private final PointExpireWriter pointExpireWriter;

    @Bean
    public Job pointExpireJob(JobRepository jobRepository, Step pointExpireStep) {
        return new JobBuilder("pointExpireJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(pointExpireStep)
                .build();
    }

    @Bean
    public Step pointExpireStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("pointExpireStep", jobRepository)
                .<String, PointExpireResult>chunk(CHUNK_SIZE, transactionManager)
                .reader(pointExpireUserReader)
                .processor(pointExpireProcessor)
                .writer(pointExpireWriter)
                .build();
    }
}
