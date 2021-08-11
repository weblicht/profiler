package eu.clarin.switchboard.profiler;

import eu.clarin.switchboard.profiler.api.TextExtractor;
import eu.clarin.switchboard.profiler.general.TikaTextExtractor;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class TextExtractorTest {
    static String expected = "The CLARIN center federation consists of universities and research institutes " +
            "throughout Europe. These centers provide an infrastructure for the distributed access " +
            "and processing of written, spoken and multi-modal language data.";

    TextExtractor textExtractor;

    @Before
    public void setUp() throws Exception {
        TikaConfig config = new TikaConfig(this.getClass().getResourceAsStream("/tikaConfig.xml"));
        textExtractor = new TikaTextExtractor(config);
    }

    @Test
    public void pdf() throws Exception {
        String fileName = "./src/test/resources/pdf/test.pdf";
        String converted = textExtractor.extractText(Paths.get(fileName).toFile(), "application/pdf");
        // pdf is a lossy format, we have to normalize spacing characters
        converted = converted.replaceAll("\\s+", " ");
        assertEquals(expected, converted);
    }

    @Test
    public void doc() throws Exception {
        String fileName = "./src/test/resources/doc/test.doc";
        String converted = textExtractor.extractText(Paths.get(fileName).toFile(), "application/msword");
        assertEquals(expected, converted);
    }

    @Test
    public void rtf() throws Exception {
        String fileName = "./src/test/resources/rtf/test.rtf";
        String converted = textExtractor.extractText(Paths.get(fileName).toFile(), "application/rtf");
        assertEquals(expected, converted);
    }
}
