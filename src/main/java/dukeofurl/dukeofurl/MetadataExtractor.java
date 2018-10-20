package dukeofurl.dukeofurl;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;

/**
 * Helper class to extract metadata from the resource behind a particular URL
 */
class MetadataExtractor {
    private static final Logger LOGGER = LogManager.getLogger(MetadataExtractor.class);

    private static final String DURATION_ATTRIBUTE = "content";
    private static final String DURATION_SELECTOR = "meta[itemprop=duration]";
    private static final String EXCEPTION_ERROR_FETCHING = "Error fetching: ";
    private static final String EXCEPTION_RECEIVED_RETURN_CODE = "Received return code: ";
    private static final String EXCEPTION_RECEIVED_UNKNOWN_MIME_TYPE = "Received mime type: ";
    private static final String MIME_TYPE_IMAGE_JPEG = "image/jpeg";
    private static final String MIME_TYPE_SEPARATOR = "/";
    private static final String MIMME_TYPE_IMAGE_GIF = "image/gif";
    private static final String OEMBED_AUTHOR_NAME = "author_name";
    private static final String OEMBED_TITLE = "title";
    private static final String OEMBED_TYPE = "type";
    private static final String OEmBED_PROVIDER_NAME = "provider_name";
    private static final String RESOURCE_BUNDLE = "messages";
    private static final String RESOURCE_FORMAT_DATE = "format.date";
    private static final String RESOURCE_FORMAT_DURATION_HOURS = "format.duration.hours";
    private static final String RESOURCE_FORMAT_DURATION_MINUTES = "format.duration.minutes";
    private static final String RESOURCE_URL_IMAGE = "url.image";
    private static final String RESOURCE_URL_TITLE = "url.title";
    private static final String RESOURCE_URL_TRACK = "url.track";
    private static final String RESOURCE_URL_VIDEO = "url.video";
    private static final String SINGLE_SPACE = " ";
    private static final String SOUNDCLOUD_COMMENTS_COUNT_ATTRIBUTE = "content";
    private static final String SOUNDCLOUD_COMMENTS_COUNT_SELECTOR = "meta[property=soundcloud:comments_count]";
    private static final String SOUNDCLOUD_DOWNLOAD_COUNT_ATTRIBUTE = "content";
    private static final String SOUNDCLOUD_DOWNLOAD_COUNT_SELECTOR = "meta[property=soundcloud:download_count]";
    private static final String SOUNDCLOUD_PLAY_COUNT_ATTRIBUTE = "content";
    private static final String SOUNDCLOUD_PLAY_COUNT_SELECTOR = "meta[property=soundcloud:play_count]";
    private static final String TIME_PUBDATE_SELECTOR = "time[pubdate]";
    private static final String WHITESPACE_CHARACTERS = "[\\t\\n\\r ]+";
    private static final String YOUTUBE_LONG_URL = "https://www.youtube.com/";
    private static final String YOUTUBE_OEMBED_URL = "https://www.youtube.com/oembed?url=";
    private static final String YOUTUBE_SHORT_URL = "https://youtu.be/";
    private static final String mIME_TYPE_IMAGE_PNG = "image/png";
    private static final int PLAIN_TEXT_MAX_LENGTH = 80;

    /**
     * Extracts metadata from the resource behind a particular URL
     * @param url The URL of the resource
     * @param locale The locale to use for message formatting
     * @return The formatted metadata strings
     */
    static List<String> extractMetadata(String url, Locale locale) {
        if (url.startsWith(YOUTUBE_LONG_URL) || url.startsWith(YOUTUBE_SHORT_URL)) {
            url = YOUTUBE_OEMBED_URL + url;
        }

        HttpGet httpGet = new HttpGet(url);
        List<String> metadataList = new ArrayList<>();
        ResourceBundle resourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE, locale);

        try (
                CloseableHttpClient httpclient = HttpClients.createDefault();
                CloseableHttpResponse response = httpclient.execute(httpGet);
        ) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new BotException(EXCEPTION_RECEIVED_RETURN_CODE + statusCode);
            }

            HttpEntity httpEntity = response.getEntity();
            ContentType contentType = ContentType.get(httpEntity);
            String mimeType = contentType.getMimeType();
            Charset charset = contentType.getCharset();

            try (InputStream content = httpEntity.getContent()) {
                if (ContentType.APPLICATION_JSON.getMimeType().equals(mimeType)) {
                    String oembedVideo = extractOembedVideo(content, charset, resourceBundle);
                    metadataList.add(oembedVideo);
                } else if (MIME_TYPE_IMAGE_JPEG.equals(mimeType) || mIME_TYPE_IMAGE_PNG.equals(mimeType) || MIMME_TYPE_IMAGE_GIF.equals(mimeType)) {
                    String image = extractImage(content, mimeType, resourceBundle);
                    metadataList.add(image);
                } else if (ContentType.TEXT_PLAIN.getMimeType().equals(mimeType)) {
                    String text = IOUtils.toString(content, charset).trim().replaceAll(WHITESPACE_CHARACTERS, SINGLE_SPACE);
                    text = StringUtils.abbreviate(text, PLAIN_TEXT_MAX_LENGTH);
                    metadataList.add(text);
                } else if (ContentType.APPLICATION_XHTML_XML.getMimeType().equals(mimeType) || ContentType.TEXT_HTML.getMimeType().equals(mimeType)) {
                    String charsetName;
                    if (charset != null) {
                        charsetName = charset.name();
                    } else {
                        charsetName = null;
                    }

                    Document document = Jsoup.parse(content, charsetName, url);

                    String title = extractTitle(document, resourceBundle);
                    if (title != null) {
                        metadataList.add(title);
                    }

                    String media = extractMedia(document, resourceBundle, locale);
                    if (media != null) {
                        metadataList.add(media);
                    }
                } else {
                    throw new BotException(EXCEPTION_RECEIVED_UNKNOWN_MIME_TYPE + mimeType);
                }
            }
        } catch (BotException | IOException exception) {
            LOGGER.error(EXCEPTION_ERROR_FETCHING + url, exception);
        }

        return metadataList;
    }

    /**
     * Generates a text description using oembed metadata of a video
     * @param content The contents of the HTTP response
     * @param charset The charset of the HTTP response
     * @param resourceBundle The resource bundle to use for formatting strings
     * @return A text description of the video
     * @throws IOException
     */
    private static String extractOembedVideo(InputStream content, Charset charset, ResourceBundle resourceBundle) throws IOException {
        String json = IOUtils.toString(content, charset);
        JSONObject jsonObject = new JSONObject(json);

        String authorName = jsonObject.getString(OEMBED_AUTHOR_NAME);
        String providerName = jsonObject.getString(OEmBED_PROVIDER_NAME);
        String title = jsonObject.getString(OEMBED_TITLE);
        String type = WordUtils.capitalize(jsonObject.getString(OEMBED_TYPE));

        String responseFormat = resourceBundle.getString(RESOURCE_URL_VIDEO);
        return String.format(responseFormat, type, providerName, title, authorName);
    }

    /**
     * Generates a text description of an image
     * @param content The contents of the HTTP response
     * @param mimeType The MIME type of the HTTP response
     * @param resourceBundle The resource bundle to use for formatting strings
     * @return A text description of the image
     * @throws IOException
     */
    private static String extractImage(InputStream content, String mimeType, ResourceBundle resourceBundle) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(content);
        String responseFormat = resourceBundle.getString(RESOURCE_URL_IMAGE);
        String type = mimeType.split(MIME_TYPE_SEPARATOR)[1].toUpperCase();
        int height = bufferedImage.getHeight();
        int width = bufferedImage.getWidth();

        return String.format(responseFormat, type, width, height);
    }

    /**
     * Extracts the text inside the title element of an HTML document
     * @param document The HTML document
     * @param resourceBundle The resource bundle to use for formatting strings
     * @return The formatted string containing the title of the document
     */
    private static String extractTitle(Document document, ResourceBundle resourceBundle) {
        String documentTitle = document.title();
        String title;

        if (documentTitle != null && !documentTitle.isEmpty()) {
            String resourceBundleString = resourceBundle.getString(RESOURCE_URL_TITLE);
            title = String.format(resourceBundleString, documentTitle);
        } else {
            title = null;
        }

        return title;
    }

    /**
     * Extracts metadata about media such as a video or an audio track
     * @param document The HTML document to extract metadata out of
     * @param resourceBundle The resource bundle to use for formatting strings
     * @param locale The locale to use for formatting the metadata
     * @return The metadata string or null if nothing could be found
     */
    private static String extractMedia(Document document, ResourceBundle resourceBundle, Locale locale) {
        Element timePubdate = document.select(TIME_PUBDATE_SELECTOR).first();
        String duration = document.select(DURATION_SELECTOR).attr(DURATION_ATTRIBUTE);
        String soundcloudCommentsCount = document.select(SOUNDCLOUD_COMMENTS_COUNT_SELECTOR).attr(SOUNDCLOUD_COMMENTS_COUNT_ATTRIBUTE);
        String soundcloudDownloadCount = document.select(SOUNDCLOUD_DOWNLOAD_COUNT_SELECTOR).attr(SOUNDCLOUD_DOWNLOAD_COUNT_ATTRIBUTE);
        String soundcloudPlayCount = document.select(SOUNDCLOUD_PLAY_COUNT_SELECTOR).attr(SOUNDCLOUD_PLAY_COUNT_ATTRIBUTE);

        String media = null;
        if (!duration.isEmpty() && timePubdate != null && !soundcloudCommentsCount.isEmpty() && !soundcloudDownloadCount.isEmpty() && !soundcloudPlayCount.isEmpty()) {
            String timePubdateText = timePubdate.text();
            media = extractTrack(duration, soundcloudCommentsCount, soundcloudDownloadCount, soundcloudPlayCount, timePubdateText, resourceBundle, locale);
        }

        return media;
    }

    /**
     * Formats metadata about an audio track
     * @param duration The duration
     * @param soundcloudCommentsCount The comments count on soundcloud
     * @param soundcloudDownloadCount The download count on soundcloud
     * @param soundcloudPlayCount The play count on soundcloud
     * @param timePubDate The publication time and date
     * @param resourceBundle The resource bundle to use for formatting strings
     * @param locale The locale to use for formatting the metadata
     * @return The formatted metadata
     */
    private static String extractTrack(String duration, String soundcloudCommentsCount, String soundcloudDownloadCount, String soundcloudPlayCount, String timePubDate, ResourceBundle resourceBundle, Locale locale) {
        String formattedDuration = transformDuration(duration, resourceBundle, locale);
        String formattedSoundcloudCommentsCount = transformLong(soundcloudCommentsCount, locale);
        String formattedSoundcloudDownloadCount = transformLong(soundcloudDownloadCount, locale);
        String formattedSoundcloudPlayCount = transformLong(soundcloudPlayCount, locale);
        String formattedTimePubDate = transformDate(timePubDate, DateTimeFormatter.ISO_DATE_TIME, resourceBundle, locale);
        String responseFormat = resourceBundle.getString(RESOURCE_URL_TRACK);

        return String.format(responseFormat, formattedDuration, formattedTimePubDate, formattedSoundcloudPlayCount, formattedSoundcloudCommentsCount, formattedSoundcloudDownloadCount);
    }

    /**
     * Formats a string containing a long integer to a more human readable string
     * @param string The string containing the unformatted long
     * @param locale The locale used for formatting
     * @return The human readable string
     */
    private static String transformLong(String string, Locale locale) {
        long l = Long.parseLong(string);

        return DecimalFormat.getInstance(locale).format(l);
    }

    /**
     * Formats a string containing a date to a more human readable string
     * @param string The string containing the unformatted date
     * @param dateTimeFormatter The format of the input date
     * @param resourceBundle The resource bundle containing the output date format
     * @param locale The locale used for formatting
     * @return The formatted date
     */
    private static String transformDate(String string, DateTimeFormatter dateTimeFormatter, ResourceBundle resourceBundle, Locale locale) {
        String dateFormat = resourceBundle.getString(RESOURCE_FORMAT_DATE);
        LocalDate localDate = LocalDate.parse(string, dateTimeFormatter);

        return DateTimeFormatter.ofPattern(dateFormat, locale).format(localDate);
    }

    /**
     * Formats a string containing a duration to a more human readable string
     * @param string The ISO-8601 duration
     * @param resourceBundle The resource bundle used for formatting
     * @param locale The locale used for formatting
     * @return The formatted duration
     */
    private static String transformDuration(String string, ResourceBundle resourceBundle, Locale locale) {
        Duration duration = Duration.parse(string);
        LocalTime localTime = LocalTime.MIDNIGHT.plus(duration);

        String format;
        if (localTime.getHour() == 0) {
            format = resourceBundle.getString(RESOURCE_FORMAT_DURATION_MINUTES);
        } else {
            format = resourceBundle.getString(RESOURCE_FORMAT_DURATION_HOURS);
        }

        return DateTimeFormatter.ofPattern(format, locale).format(localTime);
    }
}
