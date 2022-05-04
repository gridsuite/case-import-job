package org.gridsuite.cases.importer.job;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

@Configuration
@EnableBatchProcessing
@Component
public class BatchConfig extends JobExecutionListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchConfig.class);
    private static final String INSERT_FILE = "INSERT INTO files (filename, origin, import_date) VALUES (:filename, :origin, :importedDate)";

    private PlatformConfig platformConfig;
    private AcquisitionServer acquisitionServer;
    private CaseImportServiceRequester caseImportServiceRequester;
    private ModuleConfig moduleConfigAcquisitionServer;

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    private JobLauncher jobLauncher;

    @Bean
    public void setUp() {
        try {
            platformConfig = PlatformConfig.defaultConfig();
            moduleConfigAcquisitionServer = platformConfig.getModuleConfig("acquisition-server");
            acquisitionServer = new AcquisitionServer(moduleConfigAcquisitionServer.getStringProperty("url"),
                    moduleConfigAcquisitionServer.getStringProperty("username"),
                    moduleConfigAcquisitionServer.getStringProperty("password"));
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    @DependsOn("setUp")
    public Job job(JobBuilderFactory jobBuilderFactory, Step step) throws Exception {
        try {
            String jobName = "case-import-job: " + System.currentTimeMillis();
            Job job = jobBuilderFactory.get(jobName)
                    .incrementer(new RunIdIncrementer())
                    .listener(this)
                    .flow(step)
                    .end()
                    .build();

            JobParameters param = new JobParametersBuilder().addString("JobID",
                    String.valueOf(System.currentTimeMillis())).toJobParameters();
            jobLauncher.run(job, param);

            return job;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    @DependsOn("setUp")
    public Step getFilesFromServer(JdbcBatchItemWriter<FileImported> writer, JobRepository jobRepository) throws Exception {
        return new StepBuilderFactory(jobRepository, getTransactionManager()).get("get files from server")
                .<FileUrl, FileImported>chunk(100)
                .reader(reader())
                .processor(processor())
                .writer(writer)
                .allowStartIfComplete(true)
                .build();
    }

    @StepScope
    @Bean
    @DependsOn("setUp")
    public ItemReader<FileUrl> reader() {
        return new ItemReader<>() {
            private Iterator<FileUrl> iteratorFileUrl;
            @Override
            public FileUrl read() {
                if (iteratorFileUrl == null) {
                    iteratorFileUrl = getFiles().iterator();
                }

                return iteratorFileUrl.hasNext() ? iteratorFileUrl.next() : null;
            }
        };
    }

    @Bean
    @DependsOn("setUp")
    public FileImportedItemProcessor processor() {
        try {
            ModuleConfig moduleConfigCaseServer = platformConfig.getModuleConfig("case-server");
            caseImportServiceRequester = new CaseImportServiceRequester(moduleConfigCaseServer.getStringProperty("url"));
            acquisitionServer = new AcquisitionServer(moduleConfigAcquisitionServer.getStringProperty("url"),
                    moduleConfigAcquisitionServer.getStringProperty("username"),
                    moduleConfigAcquisitionServer.getStringProperty("password"));
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }

        return new FileImportedItemProcessor(acquisitionServer, caseImportServiceRequester);
    }

    @Bean
    @DependsOn("setUp")
    public JdbcBatchItemWriter<FileImported> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<FileImported>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql(INSERT_FILE)
                .dataSource(dataSource)
                .build();
    }

    @Bean
    @DependsOn("setUp")
    public JobRepository getJobRepository(DataSource dataSource) {
        try {
            JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
            factory.setDataSource(dataSource);
            factory.setTransactionManager(getTransactionManager());
            factory.afterPropertiesSet();
            return factory.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    @DependsOn("setUp")
    public PlatformTransactionManager getTransactionManager() {
        return new ResourcelessTransactionManager();
    }

    private List<FileUrl> getFiles() {
        try  {
            acquisitionServer.close();
            acquisitionServer.open();
            String casesDirectory = moduleConfigAcquisitionServer.getStringProperty("cases-directory");
            List<FileUrl> filesToAcquire = acquisitionServer.listFileUrls(casesDirectory, true);
            return filesToAcquire;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        LOGGER.info("===== JOB EXECUTION SUMMARY =====");
        LOGGER.info("{} files already imported", processor().getFilesAlreadyImported().size());
        LOGGER.info("{} files successfully imported", processor().getFilesImported().size());
        processor().getFilesImported().forEach(f -> LOGGER.info("File '{}' successfully imported", f));
        LOGGER.info("{} files import failed", processor().getFilesImportFailed().size());
        processor().getFilesImportFailed().forEach(f -> LOGGER.info("File '{}' import failed !!", f));
        LOGGER.info("=================================");
    }
}
