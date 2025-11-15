package com.candyrealms.candycore.modules.heads.currencies;

import com.candyrealms.candycore.AnubisCore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Token manager backed by VulcanToolsAPI (via VulcanLoader/VulcanAPI).
 * Uses reflection to avoid hard dependency at compile/load time.
 */
public class VulcanTokenManager {

    private final AnubisCore plugin;
    private FileConfiguration config;

    // Currency key in VulcanTools; defaults to "tokens"
    private String currencyKey;
    private long minTokenBalance;

    public VulcanTokenManager(AnubisCore plugin) {
        this.plugin = plugin;
        this.config = plugin.getHeadsCFG().getConfig();
        this.currencyKey = config.getString("vulcan-tokens-key", "tokens");
        this.minTokenBalance = config.getLong("min-token-balance", 0L);
        if (debugEnabled()) {
            debug("Constructed VulcanTokenManager | key='" + currencyKey + "' min-balance=" + minTokenBalance + " token-deduction=" + config.getInt("token-deduction", 0) + "%");
        }
    }

    public void reloadConfig() {
        this.config = plugin.getHeadsCFG().getConfig();
        this.currencyKey = config.getString("vulcan-tokens-key", "tokens");
        this.minTokenBalance = config.getLong("min-token-balance", 0L);
    }

    public double getDeductedTokens(Player player) {
        try {
            Object currency = getCurrencyManager();
            if (currency == null) {
                if (debugEnabled()) debug("CurrencyManager unavailable (VulcanToolsAPI not ready). Skipping token deduction.");
                return 0D;
            }

            boolean debug = debugEnabled();
            if (debug) {
                debug("Begin token deduction for '" + player.getName() + "'");
                debug("Configured key='" + safeKey(currencyKey) + "' min-balance=" + minTokenBalance + " deduction=" + config.getInt("token-deduction", 0) + "%");
                debug("CurrencyManager class='" + currency.getClass().getName() + "'");
            }

            String key = resolveBestCurrencyKey(currency, player, debug);
            long apiBalance = getBalance(currency, player, key);
            long papiBalance = readPapiTokens(player, debug);
            long balance = Math.max(apiBalance, papiBalance);
            if (debug) debug("Resolved balance key='" + key + "' api=" + apiBalance + ", papiBest=" + papiBalance + ", used=" + balance);
            if (balance < Math.max(0L, minTokenBalance)) return 0D;

            double deductionPercent = ((double) config.getInt("token-deduction", 0) / 100D);
            long deduction = Math.max(0L, Math.round(balance * deductionPercent));
            if (deduction <= 0) return 0D;
            if (debug) debug("Deducting tokens key='" + key + "' percent=" + (deductionPercent*100D) + "% amount=" + deduction);

            boolean removed = tryRemove(currency, player, key, deduction, debug);
            if (!removed) {
                // Try other keys in case the selected key does not allow removal but papi indicates a balance
                String[] keys = new String[]{ safeKey(currencyKey), "tokens", "coins", "token", "upgradetools_tokens", "vulcantools_tokens", "vulcan_tokens" };
                for (String k : keys) {
                    if (k == null || k.isEmpty() || k.equals(key)) continue;
                    if (tryRemove(currency, player, k, deduction, debug)) { key = k; removed = true; break; }
                }
            }
            return removed ? (double) deduction : 0D;
        } catch (Throwable t) {
            if (debugEnabled()) debug("Unexpected error during token deduction: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            return 0D;
        }
    }

    public void addTokens(Player player, double tokens) {
        try {
            Object currency = getCurrencyManager();
            if (currency == null) {
                if (debugEnabled()) debug("CurrencyManager unavailable; cannot add tokens. amount=" + tokens);
                return;
            }
            long amount = Math.max(0L, Math.round(tokens));
            if (amount <= 0) return;
            boolean debug = debugEnabled();
            String key = resolveBestCurrencyKey(currency, player, debug);
            if (debug) debug("Adding tokens key='" + key + "' amount=" + amount);
            giveCurrency(currency, player, key, amount);
        } catch (Throwable ignored) {
        }
    }

    private boolean tryRemove(Object currencyMgr, Player player, String key, long amount, boolean debug) {
        try {
            removeCurrency(currencyMgr, player, key, amount);
            if (debug) debug("removeCurrency success key='" + key + "' amount=" + amount);
            return true;
        } catch (Throwable t) {
            if (debug) debug("removeCurrency failed key='" + key + "' reason=" + t.getClass().getSimpleName() + " - " + t.getMessage());
            return false;
        }
    }

    private long readPapiTokens(OfflinePlayer player, boolean debug) {
        try {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return 0L;
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            java.lang.reflect.Method setPlaceholders = papi.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            String[] placeholders = new String[]{"upgradetools_tokens", "vulcantools_tokens", "vulcan_tokens", "tokens", "mantichoes_tokens"};
            long best = 0L; String bestRaw = ""; String bestPh = "";
            for (String ph : placeholders) {
                String fmt = "%" + ph + "%";
                String raw = String.valueOf(setPlaceholders.invoke(null, player, fmt));
                String cleaned = raw.replaceAll(",", "").trim();
                long val;
                try { val = Long.parseLong(cleaned); }
                catch (NumberFormatException nfe) {
                    try { val = (long) Double.parseDouble(cleaned); } catch (NumberFormatException nfe2) { val = 0L; }
                }
                if (debug) debug("PAPI probe '" + fmt + "' raw='" + raw + "' parsed=" + val);
                if (val > best) { best = val; bestRaw = raw; bestPh = ph; }
            }
            if (debug) debug("PAPI best tokens placeholder '" + bestPh + "' value=" + best + " (raw='" + bestRaw + "')");
            return best;
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private String resolveBestCurrencyKey(Object currencyMgr, Player player, boolean debug) {
        String[] keys = new String[]{
                safeKey(currencyKey),
                "tokens",
                "coins",
                "token",
                "upgradetools_tokens"
        };
        long best = Long.MIN_VALUE;
        String bestKey = keys[0];
        for (String k : keys) {
            if (k == null || k.isEmpty()) continue;
            try {
                long bal = getBalance(currencyMgr, player, k);
                if (debug) {
                    org.bukkit.Bukkit.getConsoleSender().sendMessage("[AnubisCore-Heads] Probe currency key='" + k + "' balance=" + bal);
                }
                if (bal > best) {
                    best = bal;
                    bestKey = k;
                }
            } catch (Throwable ignored) {}
        }
        return bestKey;
    }

    private String safeKey(String s) { return s == null ? "" : s.trim(); }

    private Object getCurrencyManager() {
        try {
            Class<?> apiClass = Class.forName("net.vulcandev.vulcanapi.vulcantools.VulcanToolsAPI");
            try {
                java.lang.reflect.Method isAvailable = apiClass.getMethod("isAvailable");
                Object avail = isAvailable.invoke(null);
                if (avail instanceof Boolean && !((Boolean) avail)) return null;
            } catch (NoSuchMethodException ignored) {
                // If method missing, assume available when class is present
            }

            java.lang.reflect.Method getInstance = apiClass.getMethod("getInstance");
            Object api = getInstance.invoke(null);
            java.lang.reflect.Method getCurrencyManager = apiClass.getMethod("getCurrencyManager");
            return getCurrencyManager.invoke(api);
        } catch (Throwable t) {
            if (debugEnabled()) debug("VulcanToolsAPI not available: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            return null;
        }
    }

    private long getBalance(Object currencyMgr, OfflinePlayer player, String key) throws Exception {
        java.lang.reflect.Method m = currencyMgr.getClass().getMethod("getBalance", OfflinePlayer.class, String.class);
        m.setAccessible(true);
        Object res = m.invoke(currencyMgr, player, key);
        if (res instanceof Number) return ((Number) res).longValue();
        return 0L;
    }

    private void giveCurrency(Object currencyMgr, OfflinePlayer player, String key, long amount) throws Exception {
        java.lang.reflect.Method m = currencyMgr.getClass().getMethod("giveCurrency", OfflinePlayer.class, String.class, long.class);
        m.setAccessible(true);
        m.invoke(currencyMgr, player, key, amount);
    }

    private void removeCurrency(Object currencyMgr, OfflinePlayer player, String key, long amount) throws Exception {
        java.lang.reflect.Method m = currencyMgr.getClass().getMethod("removeCurrency", OfflinePlayer.class, String.class, long.class);
        m.setAccessible(true);
        m.invoke(currencyMgr, player, key, amount);
    }

    private boolean debugEnabled() {
        try { return plugin.getHeadsCFG().getConfig().getBoolean("debug-vulcan-tokens", false); }
        catch (Throwable ignored) { return false; }
    }

    private void debug(String msg) {
        try {
            String pref = "[AnubisCore-Heads] ";
            plugin.getLogger().info("[Heads] " + msg);
            org.bukkit.Bukkit.getConsoleSender().sendMessage(pref + msg);
        } catch (Throwable ignored) {}
    }
}
