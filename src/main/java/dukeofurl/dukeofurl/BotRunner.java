package dukeofurl.dukeofurl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;

import java.io.IOException;

/**
 * Runnable to start the bot inside a thread
 */
class BotRunner implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(BotRunner.class);

    private PircBotX pircBotX;

    BotRunner(PircBotX pircBotX) {
        this.pircBotX = pircBotX;
    }

    public void run() {
        try {
            pircBotX.startBot();
        } catch (IOException | IrcException exception) {
            LOGGER.error("An exception occurred.", exception);
        }
    }
}
