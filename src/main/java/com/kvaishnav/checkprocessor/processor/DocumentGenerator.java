package com.kvaishnav.checkprocessor.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Generates processing documents for a check image.
 *
 * <p><strong>TODO — implementation pending.</strong>
 * Document types and generation logic will be specified in a follow-up.
 * This stub returns an empty list so the rest of the pipeline (email + Drive
 * upload) works end-to-end before document generation is defined.
 *
 * <p>When implemented, this class should:
 * <ol>
 *   <li>Accept the check image path and any extracted metadata.</li>
 *   <li>Produce one or more document files (PDF, DOCX, etc.) in a temp location.</li>
 *   <li>Return the paths to those temp files — callers are responsible for deletion.</li>
 * </ol>
 */
public class DocumentGenerator {

    private static final Logger log = LoggerFactory.getLogger(DocumentGenerator.class);

    /**
     * Generates documents for the given check image.
     *
     * @param checkFileName original filename of the check
     * @param checkImagePath path to the downloaded check image
     * @return list of generated document temp-file paths
     *         (currently always empty — pending specification)
     */
    public List<Path> generate(String checkFileName, Path checkImagePath) {
        // TODO: implement document generation once types are specified
        log.debug("DocumentGenerator: no documents generated for '{}' (not yet implemented)",
                checkFileName);
        return Collections.emptyList();
    }
}
