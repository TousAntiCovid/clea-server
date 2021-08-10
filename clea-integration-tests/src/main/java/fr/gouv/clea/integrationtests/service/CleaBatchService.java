package fr.gouv.clea.integrationtests.service;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleaBatchService {

    private static final long BATCH_EXECUTION_TIMEOUT_IN_SECONDS = 30;

    private final ApplicationProperties applicationProperties;

    /**
     * Trigger the Cluster detection batch of Clea Server.
     */
    public void triggerNewClusterIdenfication() throws IOException, InterruptedException {
        final var batchTriggerCommand = applicationProperties.getBatch().getCommand().split(" ");
        final var builder = new ProcessBuilder(batchTriggerCommand);
        builder.directory(Path.of(".").toFile());
        final var process = builder.start();
        final var background = Executors.newFixedThreadPool(2);
        background.submit(new StreamGobbler(process.getInputStream(), log::info));
        background.submit(new StreamGobbler(process.getErrorStream(), log::info));
        boolean hasExited = process.waitFor(BATCH_EXECUTION_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        background.shutdownNow();
        if (!hasExited) {
            throw new RuntimeException("Cluster detection trigger timeout");
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("Cluster detection trigger failed");
        }
    }

    private static class StreamGobbler implements Runnable {

        private final InputStream inputStream;

        private final Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .forEach(consumer);
        }
    }
}
