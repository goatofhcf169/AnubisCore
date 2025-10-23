package com.candyrealms.candycore.utils;

import org.bukkit.ChatColor;

/**
 * Utility to center chat messages approximately using 1.8 font widths.
 * Borrowed width values from common Spigot community resources.
 */
public final class CenterChatUtil {

    private CenterChatUtil() {}

    private static final int CENTER_PX = 154; // Half width of chat line

    public static String center(String message) {
        if (message == null || message.isEmpty()) return "";
        String colored = ColorUtil.color(message);

        // Calculate pixel width and detect formatting
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;
        boolean boldAtStart = false;

        // Capture initial formatting prefix (e.g., §d§l§m)
        StringBuilder prefix = new StringBuilder();
        boolean atStart = true;

        // Build visible text (strip color codes) for detection
        StringBuilder visible = new StringBuilder();

        for (int i = 0; i < colored.length(); i++) {
            char c = colored.charAt(i);
            if (c == ChatColor.COLOR_CHAR) {
                previousCode = true;
                // include the color marker and the next char in the prefix if still atStart
                if (atStart) {
                    prefix.append(c);
                }
                continue;
            } else if (previousCode) {
                previousCode = false;
                ChatColor color = ChatColor.getByChar(c);
                if (color != null) {
                    isBold = (color == ChatColor.BOLD);
                    if (atStart) {
                        prefix.append(c);
                        if (color == ChatColor.BOLD) boldAtStart = true;
                    }
                }
                continue;
            }

            atStart = false;
            visible.append(c);
            DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
            messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
            messagePxSize++;
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CENTER_PX - halvedMessageSize;

        // If the line is a dash-only separator, pad using dashes not spaces
        String vis = visible.toString().trim();
        boolean dashOnly = !vis.isEmpty() && vis.replace("-", "").trim().isEmpty();
        if (dashOnly && toCompensate > 0) {
            int dashWidth = (boldAtStart ? DefaultFontInfo.MINUS.getBoldLength() : DefaultFontInfo.MINUS.getLength()) + 1;
            int count = (int) Math.ceil((double) toCompensate / (double) dashWidth);
            StringBuilder left = new StringBuilder();
            left.append(prefix);
            for (int i = 0; i < count; i++) left.append('-');
            return left.toString() + colored;
        }

        // Default: pad with spaces
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }
        return sb.toString() + colored;
    }

    // 1.8.8 font widths
    private enum DefaultFontInfo {
        A('A', 5),
        a('a', 5),
        B('B', 5),
        b('b', 5),
        C('C', 5),
        c('c', 5),
        D('D', 5),
        d('d', 5),
        E('E', 5),
        e('e', 5),
        F('F', 5),
        f('f', 4),
        G('G', 5),
        g('g', 5),
        H('H', 5),
        h('h', 5),
        I('I', 3),
        i('i', 1),
        J('J', 5),
        j('j', 5),
        K('K', 5),
        k('k', 4),
        L('L', 5),
        l('l', 1),
        M('M', 5),
        m('m', 5),
        N('N', 5),
        n('n', 5),
        O('O', 5),
        o('o', 5),
        P('P', 5),
        p('p', 5),
        Q('Q', 5),
        q('q', 5),
        R('R', 5),
        r('r', 5),
        S('S', 5),
        s('s', 5),
        T('T', 5),
        t('t', 4),
        U('U', 5),
        u('u', 5),
        V('V', 5),
        v('v', 5),
        W('W', 5),
        w('w', 5),
        X('X', 5),
        x('x', 5),
        Y('Y', 5),
        y('y', 5),
        Z('Z', 5),
        z('z', 5),
        NUM_1('1', 5),
        NUM_2('2', 5),
        NUM_3('3', 5),
        NUM_4('4', 5),
        NUM_5('5', 5),
        NUM_6('6', 5),
        NUM_7('7', 5),
        NUM_8('8', 5),
        NUM_9('9', 5),
        NUM_0('0', 5),
        EXCLAMATION_POINT('!', 1),
        AT_SYMBOL('@', 6),
        NUM_SIGN('#', 5),
        DOLLAR_SIGN('$', 5),
        PERCENT('%', 5),
        UP_ARROW('^', 5),
        AMPERSAND('&', 5),
        ASTERISK('*', 5),
        LEFT_PARENTHESIS('(', 4),
        RIGHT_PARENTHESIS(')', 4),
        MINUS('-', 5),
        UNDERSCORE('_', 5),
        PLUS_SIGN('+', 5),
        EQUALS_SIGN('=', 5),
        LEFT_CURL_BRACE('{', 4),
        RIGHT_CURL_BRACE('}', 4),
        LEFT_BRACKET('[', 3),
        RIGHT_BRACKET(']', 3),
        COLON(':', 1),
        SEMI_COLON(';', 1),
        DOUBLE_QUOTE('"', 3),
        SINGLE_QUOTE('\'', 1),
        LEFT_ARROW('<', 4),
        RIGHT_ARROW('>', 4),
        QUESTION_MARK('?', 5),
        SLASH('/', 5),
        BACK_SLASH('\\', 5),
        LINE('|', 1),
        TILDE('~', 5),
        TICK('`', 2),
        PERIOD('.', 1),
        COMMA(',', 1),
        SPACE(' ', 3),
        DEFAULT('a', 4);

        private final char character;
        private final int length;

        DefaultFontInfo(char character, int length) {
            this.character = character;
            this.length = length;
        }

        public int getLength() {
            return this.length;
        }

        public int getBoldLength() {
            if (this == SPACE) return this.getLength();
            return this.length + 1;
        }

        public static DefaultFontInfo getDefaultFontInfo(char c) {
            for (DefaultFontInfo dFI : values()) {
                if (dFI.character == c) return dFI;
            }
            return DEFAULT;
        }
    }
}
