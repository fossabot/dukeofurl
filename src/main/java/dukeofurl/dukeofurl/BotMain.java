package dukeofurl.dukeofurl;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;

import java.util.List;

/**
 * The class containing the main method to bootstrap the bot
 */
public class BotMain {
    public static void main(String[] args) {
        List<Configuration> configurationList = BotConfigurationHelper.buildConfigurationList();

        configurationList.forEach(configuration -> {
            PircBotX pircBotX = new PircBotX(configuration);
            BotRunner botRunner = new BotRunner(pircBotX);
            new Thread(botRunner).start();
        });
    }
}
