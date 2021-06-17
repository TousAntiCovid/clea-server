package fr.gouv.clea.integrationtests.service;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleaBatchService {

    private final ApplicationProperties applicationProperties;

    /**
     * Trigger the Cluster detection batch of Clea Server.
     */
    public void triggerNewClusterIdenfication() throws IOException, InterruptedException {
        final List<String> batchTriggerCommand = applicationProperties.getBatch().getCommand();
        final ProcessBuilder builder = new ProcessBuilder(batchTriggerCommand);
        builder.directory(Path.of(System.getenv("CLEA_ROOT_DIR")).toFile());
        Process process = builder.start();
        StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), log::debug);
        Executors.newSingleThreadExecutor().submit(streamGobbler);
        boolean hasExited = process.waitFor(applicationProperties.getBatch().getTimeoutInSeconds(), TimeUnit.SECONDS);
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
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }
}
