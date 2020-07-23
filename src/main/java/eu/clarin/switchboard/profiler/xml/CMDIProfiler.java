package eu.clarin.switchboard.profiler.xml;

import eu.clarin.switchboard.profiler.api.Profile;
import eu.clarin.switchboard.profiler.api.Profiler;
import eu.clarin.switchboard.profiler.api.ProfilingException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class CMDIProfiler implements Profiler {
    public final static String XMLNAME_ROOT_CMDI = "CMD";
    public final static String NS_PREFIX_ROOT_CMDI = "http://www.clarin.eu/cmd/";

    public final static String MEDIATYPE_CMDI = "application/x-cmdi+xml";

    XMLInputFactory xmlInputFactory;

    public CMDIProfiler() {
        this(XMLInputFactory.newInstance());
    }

    public CMDIProfiler(XMLInputFactory factory) {
        this.xmlInputFactory = factory;
    }

    @Override
    public List<Profile> profile(File file) throws IOException, ProfilingException {
        XmlUtils.XmlFeatures xmlFeatures;
        Profile.Builder profileBuilder = Profile.builder().certain();

        XMLEventReader xmlReader = XmlUtils.newReader(xmlInputFactory, file);
        try {
            xmlFeatures = XmlUtils.goAfterRoot(xmlReader);
            if (XMLNAME_ROOT_CMDI.equals(xmlFeatures.rootName.getLocalPart()) &&
                    xmlFeatures.rootName.getNamespaceURI() != null &&
                    xmlFeatures.rootName.getNamespaceURI().startsWith(NS_PREFIX_ROOT_CMDI)
            ) {
                profileBuilder.mediaType(MEDIATYPE_CMDI);
            }
        } finally {
            XmlUtils.close(xmlReader);
        }
        return Collections.singletonList(profileBuilder.build());
    }
}
