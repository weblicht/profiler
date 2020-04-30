/*
 * Adds support for profiling FoLiA XML
 * by Maarten van Gompel (proycon) - proycon@anaproy.nl
 *
 */

package eu.clarin.switchboard.profiler.xml;

import eu.clarin.switchboard.profiler.api.*;
import eu.clarin.switchboard.profiler.utils.LanguageCode;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class FoliaProfiler implements Profiler {
    public final static String XMLNAME_FOLIA_ROOT = "FoLiA";

    public final static String NAMESPACE_FOLIA = "http://ilk.uvt.nl/folia";
    public final static String MEDIATYPE_FOLIA = "application/folia+xml";

    public static final String FEATURE_VERSION = "version";
    public static final String FEATURE_PROVENANCE = "provenance"; //comma seperated list of tools that processed this document
    public static final String FEATURE_LANGUAGE = "language";

    private final static String XMLNAME_VERSION = "version";
    private final static String XMLNAME_METADATA = "metadata";
    private final static String XMLNAME_DECLARATIONS = "annotations";
    private final static String XMLNAME_PROVENANCE = "provenance";
    private final static String XMLNAME_METAFIELD = "meta";
    private final static String XMLNAME_PROCESSOR = "processor";

    private final static String XMLNAME_SET = "set";


    XMLInputFactory xmlInputFactory;

    public FoliaProfiler() {
        this(XMLInputFactory.newInstance());
    }

    public FoliaProfiler(XMLInputFactory factory) {
        this.xmlInputFactory = factory;
    }

    @Override
    public List<Profile> profile(File file) throws IOException, ProfilingException {
        XMLEventReader xmlReader = XmlUtils.newReader(xmlInputFactory, file);

        Profile.Builder profileBuilder = Profile.builder().certain();
        try {
            boolean isInDocument = false;
            boolean isInMetadata = false;
            boolean isInProvenance = false;
            boolean isInDeclarations = false;
            boolean hasMetadata = false;
            boolean hasDeclarations = false;
            boolean hasProvenance = false;
            boolean hasErrors = false;
            String processors_flattened = ""; //comma separated list of names of tools through which this document has gone during processing (the original is a full provenance tree structure and contains a lot of details, this is a simplified flattened representation
            String inMetaField = null;
            String metaFieldValue = "";
            while (xmlReader.hasNext()) {
                XMLEvent event = xmlReader.nextEvent();
                if (event.isStartElement()) {
                    StartElement element = event.asStartElement();
                    String elementName = element.getName().getLocalPart();
                    if (elementName.equals(XMLNAME_FOLIA_ROOT)) {
                        profileBuilder.mediaType(MEDIATYPE_FOLIA);
                        Attribute attribute = element.getAttributeByName(new QName(XMLNAME_VERSION));
                        if (attribute == null) {
                            throw new ProfilingException("FoLiA document has no version attribute!");
                        }
                        profileBuilder.feature(FEATURE_VERSION, attribute.getValue());
                        hasErrors = true;
                        isInDocument = true;
                        //TODO LATER: check namespace? [low priority]
                    } else if (elementName.equals(XMLNAME_METADATA) && isInDocument)  {
                        isInMetadata = true;
                        hasMetadata = true;
                        Attribute srcAttribute = element.getAttributeByName(new QName("src"));
                        if (srcAttribute != null) {
                            //links to an external metadata file (e.g. CMDI): URL (or local path)
                            profileBuilder.feature("externalMetadata", srcAttribute.getValue());
                            Attribute typeAttribute = element.getAttributeByName(new QName("type"));
                            if (typeAttribute != null) {
                                //specified the type of the external metadata (e.g. cmdi)
                                profileBuilder.feature("externalMetadataType", typeAttribute.getValue());
                            }
                            //TODO LATER: if a FoLiA document uses an external metadata file,
                            // rather than an inline native metadata
                            // then it might need to be retrieved and parsed to get all the metadata!
                        }
                    } else if (elementName.equals(XMLNAME_DECLARATIONS) && isInMetadata)  {
                        isInDeclarations = true;
                    } else if (elementName.equals(XMLNAME_PROVENANCE) && isInMetadata)  {
                        isInProvenance = true;
                        hasProvenance = true;
                    } else if (!isInDocument) {
                        //invalid root tag!
                        throw new ProfilingException("Invalid root tag for FoLiA document!");
                    } else if ((isInDeclarations) && elementName.endsWith("-annotation")) {
                        Attribute attribute = element.getAttributeByName(new QName(XMLNAME_SET));


                        //the feature name is exactly the same as the element name in the declaration, so you get
                        //features like: token-annotation, lemma-annotation, text-annotation, dependency-annotation, etc..
                        String featureName = elementName;

                        //the feature value is a comma separated list of sets (as there can be multiple)
                        //or, in case an annotation layer is not associated with any set, the value is empty.
                        //(sets are usually URLs to set definitions)
                        if (attribute == null) {
                            profileBuilder.feature(featureName, "");
                        } else {
                            if (profileBuilder.hasFeature(featureName)) {
                                String value = profileBuilder.getFeature(featureName);
                                if (!value.isEmpty()) {
                                    profileBuilder.feature(featureName, value.concat(",").concat(attribute.getValue()));
                                } else {
                                    profileBuilder.feature(featureName, attribute.getValue());
                                }
                            } else {
                                profileBuilder.feature(featureName, attribute.getValue());
                            }
                        }


                        //FoLiA >v2 holds more information (list of annotators per annotation type/set), but
                        //those are currently not propagated to the profiler yet (probably overkill)

                    } else if ((isInMetadata) && (!isInProvenance) && elementName.equals(XMLNAME_METAFIELD)) {
                        //make available any arbitrary metadata from the FoLiA document to the profiler
                        //(note: this is not a controlled vocabulary!)
                        Attribute attribute = element.getAttributeByName(new QName("id"));
                        if (attribute != null) {
                            inMetaField = attribute.getValue();
                            metaFieldValue = "";
                        }
                    } else if (isInProvenance && elementName.equals(XMLNAME_PROCESSOR)) {
                        Attribute attribute = element.getAttributeByName(new QName("name"));
                        if (attribute != null) {
                            if (!processors_flattened.isEmpty()) {
                                processors_flattened += ",";
                            }
                            processors_flattened += attribute.getValue();
                        }
                    }
                } else if (inMetaField != null && event.isCharacters()) {
                    metaFieldValue += event.asCharacters().getData();
                } else if (event.isEndElement()) {
                    String elementName = event.asEndElement().getName().getLocalPart();
                    if (elementName.equals(XMLNAME_METADATA)) {
                        //prevent parsing the entire document (may be large!)
                        //just stop after metadata header
                        isInMetadata = false;
                        break;
                    } else if (elementName.equals(XMLNAME_DECLARATIONS)) {
                        isInDeclarations = false;
                    } else if (elementName.equals(XMLNAME_PROVENANCE)) {
                        isInProvenance = false;
                    } else if (elementName.equals(XMLNAME_METAFIELD) && (inMetaField != null)) {
                        if (inMetaField.equals("language") || inMetaField.equals("lang")) {
                            //this is a controlled vocabulary for the profiler, but not for FoLiA
                            //we'll do some guesses:
                            if (metaFieldValue.length() == 2) {
                                //assume iso-639-1
                                String lang = LanguageCode.iso639_1to639_3(metaFieldValue);
                                profileBuilder.feature(FEATURE_LANGUAGE, lang);
                            } else if (metaFieldValue.length() == 3) {
                                //assume iso-639-3
                                profileBuilder.feature(FEATURE_LANGUAGE, metaFieldValue);
                            } else {
                                //don't add the feature because we're not sure what it represents
                            }
                        } else {
                            profileBuilder.feature(inMetaField, metaFieldValue);
                        }
                        inMetaField = null;
                    }
                }
            }
            if (hasProvenance && !processors_flattened.isEmpty()) {
                profileBuilder.feature(FEATURE_PROVENANCE, processors_flattened);
            }
        } catch (XMLStreamException ex) {
            throw new ProfilingException("xml stream error", ex);
        }


        return Collections.singletonList(profileBuilder.build());
    }

}
