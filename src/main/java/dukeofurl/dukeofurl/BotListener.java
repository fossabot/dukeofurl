package dukeofurl.dukeofurl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.InviteEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.List;
import java.util.Locale;

/**
 * The event handler for the bot
 */
class BotListener extends ListenerAdapter {
    private static final Logger LOGGER = LogManager.getLogger(BotListener.class);

    @Override
    public void onGenericMessage(GenericMessageEvent event) {
        try {
            String message = event.getMessage();
            Locale locale = event.getBot().getConfiguration().getLocale();

            User user = event.getUser();
            if (user != null) {
                String nick = user.getNick();
                BotConfiguration botConfiguration = (BotConfiguration) event.getBot().getConfiguration();
                List<String> ignoredNicks = botConfiguration.getIgnoredNicks();
                if (ignoredNicks != null && ignoredNicks.contains(nick)) {
                    return;
                }
            }

            UrlExtractor.extracUrls(message).stream().flatMap(s -> MetadataExtractor.extractMetadata(s, locale).stream()).forEach(event::respondWith);
        } catch (RuntimeException exception) {
            LOGGER.error("Generic exception on listener", exception);
        }
    }

    @Override
    public void onInvite(InviteEvent event) {
        String channel = event.getChannel();
        if (event.getBot().getConfiguration().getAutoJoinChannels().containsKey(channel)) {
            event.getBot().send().joinChannel(channel);
        }
    }
}
