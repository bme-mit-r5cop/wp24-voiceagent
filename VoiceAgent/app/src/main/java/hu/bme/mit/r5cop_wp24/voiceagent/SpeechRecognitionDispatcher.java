package hu.bme.mit.r5cop_wp24.voiceagent;

import org.json.JSONArray;
import org.ros.node.ConnectedNode;

/**
 * Created by steve on 7/12/16.
 */
public class SpeechRecognitionDispatcher {

    ConnectedNode node;
    JSONArray array;

    public SpeechRecognitionDispatcher(ConnectedNode node) {
        this.node = node;
    }

    public void dispatch(String t) {
    }

    public void updateRegistrations(String s) {
    }
}
