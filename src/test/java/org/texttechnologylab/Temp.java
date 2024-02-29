package org.texttechnologylab;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

public class Temp {

    @Test
    public void test() throws ResourceInitializationException, CASException, SAXException {
        JCas jCas = JCasFactory.createJCas();
        XmiCasSerializer.serialize(jCas.getCas(), System.out);
    }
}
