package dev.jettro.knowledge.ingest;

import com.embabel.agent.rag.ingestion.TikaHierarchicalContentReader;
import com.embabel.agent.rag.ingestion.policy.NeverRefreshExistingDocumentContentPolicy;
import com.embabel.agent.rag.lucene.LuceneSearchOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
public class IngestController {
    private static final Logger logger = LoggerFactory.getLogger(IngestController.class);

    private final LuceneSearchOperations searchOperations;
    private final String configuredDataDirectory;

    public IngestController(LuceneSearchOperations searchOperations,
                            @Value("${knowledge.data-directory:}") String configuredDataDirectory) {
        this.searchOperations = searchOperations;
        this.configuredDataDirectory = configuredDataDirectory;
    }

    @PostMapping("/ingest")
    public String ingestData() {
        var dataPath = resolveDataPath();
        logger.info("Using ingest data directory {}", dataPath.toAbsolutePath().normalize());
        int count = 0;
        try (var stream = Files.list(dataPath)) {
            var files = stream.filter(Files::isRegularFile).toList();
            for (Path file : files) {
                var fileUri = file.toAbsolutePath().toUri().toString();
                var ingested = NeverRefreshExistingDocumentContentPolicy.INSTANCE.ingestUriIfNeeded(
                        searchOperations,
                        new TikaHierarchicalContentReader(),
                        fileUri
                );
                if (ingested != null) {
                    count++;
                }
            }
        } catch (java.io.IOException e) {
            logger.error("Error reading data directory", e);
            return "Error reading data directory: " + e.getMessage();
        }

        return "Successfully ingested " + count + " files";
    }

    private Path resolveDataPath() {
        List<Path> candidates = configuredDataDirectory == null || configuredDataDirectory.isBlank()
                ? List.of(Path.of("./data"), Path.of("../data"))
                : List.of(Path.of(configuredDataDirectory));

        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(normalized)) {
                return normalized;
            }
        }
        return candidates.getFirst().toAbsolutePath().normalize();
    }

}
