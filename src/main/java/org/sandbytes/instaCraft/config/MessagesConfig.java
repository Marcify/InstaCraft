package org.sandbytes.instaCraft.config;

import org.sandbytes.instaCraft.InstaCraft;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MessagesConfig extends BaseConfig {

    private static final MessagesConfig instance = new MessagesConfig();
    private final Set<String> warnedKeys = ConcurrentHashMap.newKeySet();

    private MessagesConfig() {
        super("messages.yml");
    }

    /**
     * Resolves a value by trying the flat key first, then "messages." prefixed key.
     * Returns null if neither exists.
     */
    private String resolve(String key) {
        String value = config.getString(key);
        if (value != null) return value;
        return config.getString("messages." + key);
    }

    public String getPrefix() {
        String prefix = resolve("prefix");
        return prefix != null ? prefix : "&6[InstaCraft] &r";
    }

    /**
     * Checks if the prefix should be used in messages.
     */
    public boolean usePrefix() {
        return config.getBoolean("use-prefix", true);
    }

    /**
     * Gets a message by key, prepending the prefix if enabled.
     */
    public String getMessage(String key) {
        String prefix = usePrefix() ? getPrefix() : "";
        String value = resolve(key);
        if (value != null) return prefix + value;
        logMissingKey(key);
        return prefix + "&cMissing message: " + key;
    }

    /**
     * Gets a raw message by key without the prefix.
     */
    public String getRawMessage(String key) {
        String value = resolve(key);
        if (value != null) return value;
        logMissingKey(key);
        return "&cMissing message: " + key;
    }

    private void logMissingKey(String key) {
        if (warnedKeys.add(key)) {
            InstaCraft.getInstance().getLogger()
                    .warning("[Messages] Key not found: " + key);
        }
    }

    @Override
    public synchronized void reload() {
        warnedKeys.clear();
        super.reload();
    }

    public static MessagesConfig getInstance() {
        return instance;
    }
}
