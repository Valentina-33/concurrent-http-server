package edu.eci.arsw.networking.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentTypesTest {

    @Test
    void htmlResolvesToTextHtml() {
        assertEquals("text/html; charset=utf-8", ContentTypes.resolve("index.html"));
    }

    @Test
    void jsResolvesToTextJavascript() {
        assertEquals("text/javascript; charset=utf-8", ContentTypes.resolve("app.js"));
    }

    @Test
    void pngResolvesToImagePng() {
        assertEquals("image/png", ContentTypes.resolve("images/logo.png"));
    }

    @Test
    void jpgAndJpegBothResolveToImageJpeg() {
        assertEquals("image/jpeg", ContentTypes.resolve("banner.jpg"));
        assertEquals("image/jpeg", ContentTypes.resolve("banner.jpeg"));
    }

    @Test
    void unknownExtensionFallsBackToOctetStream() {
        assertEquals("application/octet-stream", ContentTypes.resolve("archive.zip"));
    }

    @Test
    void missingExtensionFallsBackToOctetStream() {
        assertEquals("application/octet-stream", ContentTypes.resolve("Makefile"));
    }

    @Test
    void isSupportedExtensionReflectsTheSameMap() {
        assertTrue(ContentTypes.isSupportedExtension("index.html"));
        assertFalse(ContentTypes.isSupportedExtension("archive.zip"));
    }
}
