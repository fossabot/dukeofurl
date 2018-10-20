package dukeofurl.dukeofurl;

import org.pircbotx.Configuration;

import java.util.List;

/**
 * Class extending pircbotx's configuration class with some extra properties
 */
class BotConfiguration extends Configuration {
    private List<String> ignoredNicks;

    private BotConfiguration(Builder builder) {
        super(builder);
        this.ignoredNicks = builder.getIgnoredNicks();
    }

    List<String> getIgnoredNicks() {
        return ignoredNicks;
    }

    static class Builder extends Configuration.Builder {
        private List<String> ignoredNicks;

        List<String> getIgnoredNicks() {
            return ignoredNicks;
        }

        void setIgnoredNicks(List<String> ignoreNicks) {
            this.ignoredNicks = ignoreNicks;
        }

        public BotConfiguration buildConfiguration() {
            return new BotConfiguration(this);
        }
    }
}
