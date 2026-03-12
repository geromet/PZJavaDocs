/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import org.w3c.dom.Element;
import zombie.util.PZXmlUtil;

public class PZForEeachElementXmlParseException
extends RuntimeException {
    private Element xmlElement;

    public PZForEeachElementXmlParseException() {
    }

    public PZForEeachElementXmlParseException(String message) {
        super(message);
    }

    public PZForEeachElementXmlParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public PZForEeachElementXmlParseException(Throwable cause) {
        super(cause);
    }

    public PZForEeachElementXmlParseException(String message, Element element, Throwable cause) {
        super(message, cause);
        this.xmlElement = element;
    }

    @Override
    public String toString() {
        Throwable cause;
        Object toString2 = super.toString();
        if (this.xmlElement != null) {
            toString2 = (String)toString2 + System.lineSeparator();
            toString2 = (String)toString2 + " xmlElement:" + PZXmlUtil.elementToPrettyStringSafe(this.xmlElement);
        }
        if ((cause = this.getCause()) != null) {
            toString2 = (String)toString2 + System.lineSeparator() + "  Caused by:" + System.lineSeparator() + "    " + String.valueOf(cause);
        }
        return toString2;
    }
}

