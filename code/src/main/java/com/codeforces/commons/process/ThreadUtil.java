package com.codeforces.commons.process;

import org.apache.log4j.Logger;

import java.util.Date;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.03.11
 */
public class ThreadUtil {
    private static final Logger logger = Logger.getLogger(ThreadUtil.class);

    private ThreadUtil() {
        throw new UnsupportedOperationException();
    }

    public static Thread newThread(
            String name, Runnable runnable, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        Thread thread = new Thread(runnable, name);
        thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        return thread;
    }

    public static Thread newThread(
            String name, Runnable runnable, Thread.UncaughtExceptionHandler uncaughtExceptionHandler, long stackSize) {
        Thread thread = new Thread(null, runnable, name, stackSize);
        thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        return thread;
    }

    public static Thread newThread(Runnable runnable, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        return newThread(
                "Unnamed thread created by " + Thread.currentThread().toString() + " at " + new Date() + '.',
                runnable, uncaughtExceptionHandler
        );
    }

    public static Thread newThread(
            Runnable runnable, Thread.UncaughtExceptionHandler uncaughtExceptionHandler, long stackSize) {
        return newThread(
                "Unnamed thread created by " + Thread.currentThread().toString() + " at " + new Date() + '.',
                runnable, uncaughtExceptionHandler, stackSize
        );
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            // No operations.
        }
    }

    public static <T> T execute(Operation<T> operation, int attemptCount, ExecutionStrategy strategy) throws Throwable {
        ensureArguments(operation, attemptCount, strategy);

        for (int attemptIndex = 1; attemptIndex <= attemptCount; ++attemptIndex) {
            try {
                return operation.run();
            } catch (Throwable e) {
                if (strategy.getUnsuccessHandler() != null) {
                    strategy.getUnsuccessHandler().handle(attemptIndex);
                }

                if (attemptIndex < attemptCount) {
                    if (attemptIndex == 1) {
                        logger.info("Iteration #1 has been failed: " + e.getMessage(), e);
                    } else {
                        logger.warn("Iteration #" + attemptIndex + " has been failed: " + e.getMessage(), e);
                    }
                    sleep(strategy.getDelayTimeMillis(attemptIndex));
                } else {
                    logger.error("Iteration #" + attemptIndex + " has been failed: " + e.getMessage(), e);
                    throw e;
                }
            }
        }

        throw new RuntimeException("This line shouldn't be executed.");
    }

    private static <T> void ensureArguments(Operation<T> operation, int attemptCount, ExecutionStrategy strategy) {
        if (operation == null) {
            throw new IllegalArgumentException("Argument 'operation' can't be 'null'.");
        }

        if (attemptCount < 1) {
            throw new IllegalArgumentException("Argument 'attemptCount' should be positive.");
        }

        if (strategy == null) {
            throw new IllegalArgumentException("Argument 'strategy' can't be 'null'.");
        }
    }

    public interface Operation<T> {
        T run() throws Throwable;
    }

    /**
     * Action to be executed after each unsuccessful attempt to execute operation.
     */
    public interface UnsuccessHandler {
        void handle(int attemptIndex);
    }

    public static class ExecutionStrategy {
        private final long delayTimeMillis;
        private final Type type;
        private final UnsuccessHandler unsuccessHandler;

        public ExecutionStrategy(long delayTimeMillis, Type type) {
            this(delayTimeMillis, type, null);
        }

        public ExecutionStrategy(long delayTimeMillis, Type type, UnsuccessHandler unsuccessHandler) {
            ensureArguments(delayTimeMillis, type);

            this.delayTimeMillis = delayTimeMillis;
            this.type = type;
            this.unsuccessHandler = unsuccessHandler;
        }

        private static void ensureArguments(long delayTimeMillis, Type type) {
            if (delayTimeMillis < 1) {
                throw new IllegalArgumentException("Argument 'delayTimeMillis' should be positive.");
            }

            if (type == null) {
                throw new IllegalArgumentException("Argument 'type' can't be 'null'.");
            }
        }

        public long getDelayTimeMillis() {
            return delayTimeMillis;
        }

        /**
         * @param attemptIndex 1-based attempt index.
         * @return Delay time according to attempt index and strategy type.
         */
        public long getDelayTimeMillis(int attemptIndex) {
            if (attemptIndex < 1) {
                throw new IllegalArgumentException("Argument 'attemptNumber' should be positive.");
            }

            switch (type) {
                case CONSTANT:
                    return delayTimeMillis;
                case LINEAR:
                    return delayTimeMillis * attemptIndex;
                case SQUARE:
                    return delayTimeMillis * attemptIndex * attemptIndex;
                default:
                    throw new IllegalArgumentException("Unknown strategy type '" + type + "'.");
            }
        }

        /**
         * @return Returns action to be executed after each unsuccessful attempt to execute operation.
         */
        public UnsuccessHandler getUnsuccessHandler() {
            return unsuccessHandler;
        }

        public Type getType() {
            return type;
        }

        public enum Type {
            /**
             * Same delay interval between execution attempts.
             */
            CONSTANT,

            /**
             * Delay interval grows from attempt to attempt with a linear law.
             */
            LINEAR,

            /**
             * Delay interval grows from attempt to attempt with a square law.
             */
            SQUARE
        }
    }
}