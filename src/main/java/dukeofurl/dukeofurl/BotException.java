package dukeofurl.dukeofurl;

/**
 * Generic unhandled exception used for rethrowing handled exceptions
 */
class BotException extends RuntimeException {
    BotException(Throwable cause) {
        super(cause);
    }

    BotException(String message) {
        super(message);
    }
}
