# Profiler

The Profiler is a java library able to profile (i.e. determine the mediatype, format variant and language) of an arbitrary file.

## How to use the Profiler

```java
File myFile = new File("/path/to/my/file");
Profiler profiler = new DefaultProfiler();
List<Profile> detectedProfiles = profiler.profile(myFile);
```

## API

A profiler is a Java class satisfying the Profiler interface 
([Profiler.java](./src/main/java/eu/clarin/switchboard/profiler/api/Profiler.java)). The profiler interface specifies a single function: 

```java 
List<Profile> profile(File file) throws IOException, ProfilingException;
```
A profiler can return multiple [Profile](./src/main/java/eu/clarin/switchboard/profiler/api/Profile.java) objects for a single file when there is ambiguity in the data. However, the order of returned profiles is important, and the profile with the highest confidence should be first on the list. 

The [Profile](./src/main/java/eu/clarin/switchboard/profiler/api/Profile.java) is a simple data object for storing a data profile (e.g. mediatype, language, version, other features). Use the static `builder()` function to make a profile builder, or the nested `Profile.Flat` class for serialization/deserialization.

## The default profiler

A profiler can be specialized for detection of a single format, or be more general and perform just a few simple tests then delegate the detection to other specialized profilers. The main profiler ([DefaultProfiler](./src/main/java/eu/clarin/switchboard/profiler/DefaultProfiler.java)) invokes the Apache Tika library for detecting the general mediatype, then invokes specialized profilers for various formats (xml, text). These profilers in turn invoke more specialized profilers.

## Adding a specialized profiler

To add your own specialized profiler, first add a separate class with its implementation, then find the place where a call to your profiler should be inserted, starting from the [DefaultProfiler](./src/main/java/eu/clarin/switchboard/profiler/DefaultProfiler.java). 

For instance, a profiler for an xml subformat would be placed in the `eu.clarin.switchboard.profiler.xml` package, and a call to it would be placed in the more general [XmlProfiler](./src/main/java/eu/clarin/switchboard/profiler/xml/XmlProfiler.java), in the `profile` method.