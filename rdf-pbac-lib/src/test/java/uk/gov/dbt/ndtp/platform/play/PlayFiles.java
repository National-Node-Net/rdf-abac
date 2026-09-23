// SPDX-License-Identifier: Apache-2.0
// Originally developed by Telicent Ltd.; subsequently adapted, enhanced, and maintained by the National Digital Twin Programme.
/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/*
 *  Modifications made by the National Digital Twin Programme (NDTP)
 *  © Crown Copyright 2025. This work has been developed by the National Digital Twin Programme
 *  and is legally attributed to the Department for Business and Trade (UK) as the governing entity.
 */


package uk.gov.dbt.ndtp.platform.play;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.atlas.io.IOX;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility functions for keeping HTTP and HTTP-like requests in message files.
 * <pre>
 * Headers
 * -- blank line --
 * body
 * </pre>
 */
public class PlayFiles {

    public static final Logger LOG = LoggerFactory.getLogger("Files");
    private static boolean SORTED = true;
    private static Set<String> directoryExclusions = Set.of("src", "target");

    private static Predicate<Path> includeFile = path ->{
        String fn = path.getFileName().toString();
        if ( fn.startsWith(".") )
            return false;
        return true;
    };

    private static Predicate<Path> includeDirectory = path ->{
        String fn = path.getFileName().toString();
        if ( fn.startsWith(".") )
            return false;
        if ( directoryExclusions.contains(fn) )
            return false;
        return true;
    };

    /**
     * Apply an action to every file in a directory tree (with directory exclusions).
     * Return the total time in milliseconds taken.
     */
    public static long action(String directory, Consumer<MessageRequest> action, Consumer<Map<String, String>> dftHeaders) {
        return org.apache.jena.atlas.lib.Timer.time(()->{
            Function<Path, MessageRequest> function = p->fileToMessageRequest(p, dftHeaders);
            Stream<MessageRequest> requests = files(directory).map(function);
            requests.forEach(action);
        });
    }

    /**
     * Send files to consumer.
     * <p>
     * If the filename has extension ".msg", it is assumed to
     * be a complete messages with headers and body. It is converted into a {@link MessageRequest}.
     * <p>
     * For other file extensions, the file is interpreted as as RDF data. A content type is determined,
     * and used as the header. In addition, the headers are passed to a handler for additional headers to be added.
     * The handler can be "null" for no action.
     */
    public static MessageRequest fileToMessageRequest(Path path, Consumer<Map<String, String>> moreHeaders) {
        String datafile = path.toString();
        String ext = FileUtils.getFilenameExt(datafile);
        //FmtLog.info(LOG, "%s", datafile);
        if ( "msg".equals(ext) )
            return MessageRequest.fromFile(path);
        Lang lang = RDFLanguages.fileExtToLang(ext);
        if ( lang == null ) {
            FmtLog.error(LOG, "Not recognized : %s", datafile);
            return null;
        }
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put(HttpNames.hContentType, lang.getHeaderString());
        if ( moreHeaders != null )
            moreHeaders.accept(headerMap);
        return MessageRequest.create(datafile, headerMap);
    }

    /**
     * Return a stream of files, sorted by directory, then by filename within each directory.
     * The stream is eager on directories and lazy on files.
     */
    public static Stream<Path> files(String directory) {
        List<Path> dirs = directories(directory, 0, Integer.MAX_VALUE);
        Stream<Path> results = null;
        for ( Path dir : dirs ) {
            Stream<Path> r1 = files1(dir);
            results = concat(results,  r1);
        }
        return results;
    }

    /** File in one directory */
    private static Stream<Path> files1(Path directory) {
        try {
            // Passed back - out of scope.
            @SuppressWarnings("resource")
            Stream<Path> paths = Files.list(directory);
            Comparator<Path> comparator = (p1,p2) -> p1.getFileName().compareTo(p2.getFileName());
            Stream<Path> files = paths.filter(Files::isRegularFile).filter(includeFile);
            if ( SORTED )
                files = files.sorted(comparator);
            return files;
        } catch (IOException e) {
            throw IOX.exception(e);
        }
    }

    private static <T> Stream<T> concat(Stream<T> a, Stream<T> b) {
        if ( a == null && b == null )
            return null;
        if ( a == null )
            return b;
        if ( b == null )
            return a;
        return Stream.concat(a, b);
    }

    /** Return a list of directories sorted by path name.
     * Control the recursion depth by maxDepth.
     * maxDepth == -1 for any depth.
     */
    private static List<Path> directories(String directory, int depth, int maxDepth) {
        List<Path> acc = new ArrayList<>();
        Path pDir = Paths.get(directory);
        directories(acc, pDir, depth, maxDepth);
        return acc;
    }

    // Accumulate directories.
    private static void directories(List<Path> acc, Path directory, int depth, int maxDepth) {
        acc.add(directory);
        if ( maxDepth > 0 && depth > maxDepth )
            return;
        depth++;
        try (Stream<Path> paths = Files.list(directory)) {
            // Have to scan for directories.
            List<Path> dirs = paths.filter(Files::isDirectory).filter(includeDirectory).collect(Collectors.toList());
            // Not faster?
//        try (Stream<Path> paths = Files.find(directory, maxDepth-depth, (path, x)->Files.isDirectory(path))){
//            List<Path> dirs = paths.filter(inclusions).collect(Collectors.toList());
            if ( SORTED )
                Collections.sort(dirs, Path::compareTo);
            for( Path d : dirs )
                directories(acc, d, depth, maxDepth);
        } catch (IOException e) {
            throw new RuntimeIOException(e.getMessage(), e);
        }
    }
}
