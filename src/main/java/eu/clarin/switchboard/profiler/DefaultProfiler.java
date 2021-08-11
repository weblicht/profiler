package eu.clarin.switchboard.profiler;

import eu.clarin.switchboard.profiler.api.Profile;
import eu.clarin.switchboard.profiler.api.Profiler;
import eu.clarin.switchboard.profiler.api.ProfilingException;
import eu.clarin.switchboard.profiler.api.TextExtractor;
import eu.clarin.switchboard.profiler.general.OptimaizeLanguageDetector;
import eu.clarin.switchboard.profiler.general.TikaProfiler;
import eu.clarin.switchboard.profiler.general.TikaTextExtractor;
import eu.clarin.switchboard.profiler.general.TikaUTF8Detector;
import eu.clarin.switchboard.profiler.json.JsonProfiler;
import eu.clarin.switchboard.profiler.text.TextProfiler;
import eu.clarin.switchboard.profiler.utils.LanguageCode;
import eu.clarin.switchboard.profiler.xml.XmlProfiler;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * The default root Profiler implementation, which calls in turn all the specialized profilers.
 */
public class DefaultProfiler implements Profiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProfiler.class);

    private final Profiler tikaProfiler;
    private final Profiler xmlProfiler;
    private final Profiler jsonProfiler;
    private final Profiler textProfiler;
    private final TextExtractor textExtractor;
    private final OptimaizeLanguageDetector languageDetector;
    private final TikaUTF8Detector utf8Detector;

    public DefaultProfiler() throws TikaException, IOException, SAXException {
        TikaConfig tikaConfig = new TikaConfig(this.getClass().getResourceAsStream("/tikaConfig.xml"));
        tikaProfiler = new TikaProfiler(tikaConfig);
        xmlProfiler = new XmlProfiler();
        jsonProfiler = new JsonProfiler();
        textProfiler = new TextProfiler();
        textExtractor = new TikaTextExtractor(tikaConfig);
        languageDetector = new OptimaizeLanguageDetector();
        utf8Detector = new TikaUTF8Detector(tikaConfig);
    }

    public TextExtractor getTextExtractor() {
        return textExtractor;
    }

    public List<Profile> profile(File file) throws IOException, ProfilingException {
        List<Profile> profiles;
        Profile firstProfile;

        // ask Tika to detect the mediatype; it will not return any language
        {
            profiles = tikaProfiler.profile(file);
            LOGGER.debug("file " + file.getName() + "; tika returned: " + profiles);
            assert profiles.size() == 1;
            firstProfile = profiles.get(0);
        }

        // depending on detected mediatype, call specific profilers
        if (firstProfile.isMediaType(MediaType.APPLICATION_XML)) {
            List<Profile> xmlProfiles = xmlProfiler.profile(file);
            LOGGER.debug("file " + file.getName() + "; xmlprof returned: " + xmlProfiles);
            if (xmlProfiles != null && !xmlProfiles.isEmpty()) {
                profiles = xmlProfiles;
                firstProfile = profiles.get(0);
            }
        } else if (firstProfile.isMediaType(MediaType.APPLICATION_JSON)) {
            List<Profile> jsonProfiles = jsonProfiler.profile(file);
            LOGGER.debug("file " + file.getName() + "; jsonprof returned: " + jsonProfiles);
            if (jsonProfiles != null && !jsonProfiles.isEmpty()) {
                profiles = jsonProfiles;
                firstProfile = profiles.get(0);
            }
        } else if (firstProfile.isMediaType(MediaType.TEXT_PLAIN)) {
            List<Profile> textProfiles = textProfiler.profile(file);
            LOGGER.debug("file " + file.getName() + "; textprof returned: " + textProfiles);
            if (textProfiles != null && !textProfiles.isEmpty()) {
                profiles = textProfiles;
                firstProfile = profiles.get(0);
            }
            if (firstProfile != null && firstProfile.isMediaType(MediaType.TEXT_PLAIN)) {
                boolean isUTF8 = utf8Detector.isUTF8(file);
                if (isUTF8) {
                    firstProfile = Profile.builder(firstProfile).feature(Profile.FEATURE_IS_UTF8, "true").build();
                }
            }
            LOGGER.debug("file " + file.getName() + "; first text profile is: " + firstProfile);
        }

        // detect language for the mediatypes where this makes sense
        String language = firstProfile.getFeature(Profile.FEATURE_LANGUAGE);
        if (language == null || LanguageCode.UNDETERMINED.equals(language)) {
            String text = null;
            try {
                text = textExtractor.extractText(file, firstProfile.getMediaType());
            } catch (Exception xc) {
                LOGGER.info("Cannot convert media to text for detecting the language: " + xc.getMessage());
            }

            if (text != null) {
                language = languageDetector.detect(text);
                LOGGER.debug("file " + file.getName() + "; detected language: " + language);
                firstProfile = Profile.builder(firstProfile).language(language).build();
                profiles = new ArrayList<>(profiles);
                profiles.set(0, firstProfile);
            }
        }

        return profiles;
    }
}
