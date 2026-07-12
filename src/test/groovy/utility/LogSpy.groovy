package utility

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory

/**
 *  A small test fixture which records everything the code under test logs,
 *  so that specifications can assert that no errors were silently
 *  swallowed and logged while a test was running.
 *  <p>
 *  This matters because SwingTree deliberately follows a "log, don't crash"
 *  philosophy in many places, especially around threading and event
 *  processing. A bug in these areas often does not surface as an exception
 *  in the test thread at all, it only leaves an ERROR entry in the log.
 *  By attaching a {@code LogSpy} to the root logger, such silent failures
 *  become visible and assertable.
 *  <p>
 *  Usage:
 *  <pre>{@code
 *      def log = LogSpy.attach()   // typically in a setup() method
 *      ...
 *      log.errors().isEmpty()      // typically in a then: block
 *      ...
 *      log.detach()                // typically in a cleanup() method
 *  }</pre>
 */
final class LogSpy
{
    private final ListAppender<ILoggingEvent> _appender = new ListAppender<>()

    private LogSpy() {}

    /**
     *  Creates a new {@code LogSpy} and attaches it to the root logger
     *  so that it records all subsequent log events.
     *  @return The attached spy, ready for inspection.
     */
    static LogSpy attach() {
        var spy = new LogSpy()
        spy._appender.start()
        _rootLogger().addAppender(spy._appender)
        return spy
    }

    /**
     *  Detaches this spy from the root logger again.
     *  Call this in the {@code cleanup()} section of a specification.
     */
    void detach() {
        _rootLogger().detachAppender(_appender)
        _appender.stop()
    }

    /**
     *  @return All events logged at ERROR level since this spy was attached,
     *          rendered as human readable strings (message plus exception, if any),
     *          so that a failed assertion immediately shows what went wrong.
     */
    List<String> errors() {
        return _appender.list
                .findAll { it.level == Level.ERROR }
                .collect { event ->
                    var throwable = event.throwableProxy
                    var suffix = throwable == null ? "" : " (${throwable.className}: ${throwable.message})"
                    return event.formattedMessage + suffix
                }
    }

    /**
     *  @return All events logged at WARN level since this spy was attached,
     *          rendered as human readable strings, so that specifications can
     *          assert that (or that no) warnings were emitted.
     */
    List<String> warnings() {
        return _appender.list
                .findAll { it.level == Level.WARN }
                .collect { it.formattedMessage }
    }

    private static Logger _rootLogger() {
        return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
    }
}
