package org.texttechnologylab.engine;

import org.texttechnologylab.mdd.data.EdgeDataPoint;
import org.texttechnologylab.mdd.data.SentenceDataPoint;

public class DummyEngineEdge extends DummyEngine {

    @Override
    protected SentenceDataPoint createSentenceDataPoint() {
        return new EdgeDataPoint();
    }

}
