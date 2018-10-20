package dukeofurl.dukeofurl;

import org.nibor.autolink.LinkExtractor;
import org.nibor.autolink.LinkSpan;
import org.nibor.autolink.LinkType;

import java.util.ArrayList;
import java.util.List;

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
        LinkExtractor linkExtractor = LinkExtractor.builder().build();
        Iterable<LinkSpan> linkSpanIterable = linkExtractor.extractLinks(string);
        List<String> urlList = new ArrayList<>();

        for (LinkSpan linkSpan : linkSpanIterable) {
            if (LinkType.URL.equals(linkSpan.getType())) {
                String url = string.substring(linkSpan.getBeginIndex(), linkSpan.getEndIndex());
                urlList.add(url);
            }
        }

        return urlList;
    }
}
