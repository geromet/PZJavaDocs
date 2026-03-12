/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

public final class PZXmlParserException
extends Exception {
    public PZXmlParserException() {
    }

    public PZXmlParserException(String message) {
        super(message);
    }

    public PZXmlParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public PZXmlParserException(Throwable cause) {
        super(cause);
    }

    @Override
    public String toString() {
        String base = super.toString();
        Object toString2 = base;
        Throwable cause = this.getCause();
        if (cause != null) {
            toString2 = base + System.lineSeparator() + "  Caused by:" + System.lineSeparator() + "    " + String.valueOf(cause);
        }
        return toString2;
    }
}

