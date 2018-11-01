package dukeofurl.dukeofurl;

import org.nibor.autolink.LinkExtractor;
import org.nibor.autolink.LinkSpan;
import org.nibor.autolink.LinkType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * Helper class to extract a list of URLs from a string
 */
class UrlExtractor {
    /**
     * Extracts a list of URLs from a string
     * @param string The string to extract URLs out of
     * @return The list of URLs
     */
    static List<String> extracUrls(String string) {
        return StreamSupport.stream(LinkExtractor.builder().build().extractLinks(string).spliterator(), false)
                .filter(l -> l.getType().equals(LinkType.URL))
                .map(l -> string.substring(l.getBeginIndex(), l.getEndIndex()))
                .collect(toList());
    }
}
