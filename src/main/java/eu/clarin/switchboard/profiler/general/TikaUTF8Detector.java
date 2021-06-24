package eu.clarin.switchboard.profiler.general;

import com.google.common.io.ByteStreams;
import eu.clarin.switchboard.profiler.api.UTF8Detector;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class TikaUTF8Detector implements UTF8Detector {
    private static final Logger LOGGER = LoggerFactory.getLogger(TikaUTF8Detector.class);
    public static final int MAX_FILE_READ = 4 * 1024;

    org.apache.tika.detect.EncodingDetector encodingDetector;

    public TikaUTF8Detector(TikaConfig config) {
        encodingDetector = config.getEncodingDetector();
    }

    public boolean isUTF8(File file) throws IOException {
        try (TikaInputStream inputStream = TikaInputStream.get(file.toPath())) {
            Metadata metadata = new Metadata();
            metadata.add(TikaMetadataKeys.RESOURCE_NAME_KEY, file.getName());
            Charset charset = encodingDetector.detect(inputStream, metadata);
            if (charset == null) {
                return false;
            }
            if (charset == StandardCharsets.ISO_8859_1 && contentIsASCII(file)) {
                return true;
            }
            if (charset == StandardCharsets.US_ASCII) {
                return true;
            }
            return charset == StandardCharsets.UTF_8;
        }
    }

    private static boolean contentIsASCII(File file) throws IOException {
        try(InputStream fis = new BufferedInputStream(new FileInputStream(file));
            InputStream is = ByteStreams.limit(fis, MAX_FILE_READ)) {
            for (byte b: ByteStreams.toByteArray(is)) {
                // ASCII is [0..127] and a byte is signed [-128..127]
                // so it's enough to test for negative values
                if (b < 0) {
                    return false;
                }
            }
        }
        return true;
    }
}
