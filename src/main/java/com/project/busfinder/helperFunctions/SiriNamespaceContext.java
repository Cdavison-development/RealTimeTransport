package com.project.busfinder.helperFunctions;

import javax.xml.namespace.NamespaceContext;
import javax.xml.XMLConstants;
import java.util.Iterator;

public class SiriNamespaceContext implements NamespaceContext {
    @Override
    public String getNamespaceURI(String prefix) {
        if ("siri".equals(prefix)) {
            return "http://www.siri.org.uk/siri";
        }
        return XMLConstants.NULL_NS_URI;
    }

    @Override
    public String getPrefix(String uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator getPrefixes(String uri) {
        throw new UnsupportedOperationException();
    }
}
