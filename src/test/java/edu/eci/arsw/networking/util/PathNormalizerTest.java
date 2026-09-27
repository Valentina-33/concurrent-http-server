package edu.eci.arsw.networking.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathNormalizerTest {

    @Test
    void rootPathNormalizesToEmptyString() {
        assertEquals(Optional.of(""), PathNormalizer.normalize("/"));
    }

    @Test
    void simpleFileNameIsUnchanged() {
        assertEquals(Optional.of("app.js"), PathNormalizer.normalize("/app.js"));
    }

    @Test
    void nestedPathIsPreserved() {
        assertEquals(Optional.of("images/logo.png"), PathNormalizer.normalize("/images/logo.png"));
    }

    @Test
    void directTraversalAboveRootIsRejected() {
        assertTrue(PathNormalizer.normalize("/../secret.txt").isEmpty());
    }

    @Test
    void deepTraversalAboveRootIsRejected() {
        assertTrue(PathNormalizer.normalize("/../../../etc/passwd").isEmpty());
    }

    @Test
    void traversalThatDescendsThenEscapesIsRejected() {
        assertTrue(PathNormalizer.normalize("/images/../../secret.txt").isEmpty());
    }

    @Test
    void traversalThatStaysInsideRootIsResolved() {
        assertEquals(Optional.of("app.js"), PathNormalizer.normalize("/images/../app.js"));
    }

    @Test
    void backslashIsRejectedAsPotentialTraversal() {
        assertTrue(PathNormalizer.normalize("/images\\..\\secret.txt").isEmpty());
    }

    @Test
    void nullByteIsRejected() {
        assertTrue(PathNormalizer.normalize("/app.js\0.html").isEmpty());
    }

    @Test
    void repeatedSlashesAreCollapsed() {
        assertEquals(Optional.of("images/logo.png"), PathNormalizer.normalize("//images///logo.png"));
    }
}
