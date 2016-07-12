package hu.bme.mit.r5cop_wp24.voiceagent;

import org.json.JSONArray;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by steve on 7/12/16.
 */
public class SpeechRecognitionDispatcher {

    static class RegExpWithPriority implements Comparable<RegExpWithPriority> {
        String regexp;
        int priority;

        public RegExpWithPriority(String regexp, int priority) {
            this.regexp = regexp;
            this.priority = priority;
        }

        public boolean matches(String recognizedString) {
            recognizedString.matches(regexp);
        }

        @Override
        public int compareTo(RegExpWithPriority another) {
            return Integer.compare(this.priority, another.priority);
        }
    }
    ConnectedNode node;
    JSONArray array;

    HashMap<String, List<RegExpWithPriority>> registeredRegExps;
    HashMap<String, Publisher<std_msgs.String>> publishers;

    public SpeechRecognitionDispatcher(ConnectedNode node) {
        this.node = node;
        registeredRegExps = new HashMap<>();
        publishers = new HashMap<>();
    }

    public void dispatch(String recognizedString) {
        ArrayList<String> matchingTopics = new ArrayList<>();
        for (String topic : registeredRegExps.keySet()) {
            for (RegExpWithPriority rewp : registeredRegExps.get(topic)) {
                if (rewp.matches(recognizedString)) {
                    matchingTopics.add(topic);
                    break;
                }
            }
        }

        for (String topic : matchingTopics) {
            Publisher<std_msgs.String> pub = publishers.get(topic);
            std_msgs.String str = pub.newMessage();
            //todo: create message
            String msg = "foobar"; //serialize message to string
            str.setData(msg);
            pub.publish(str);
        }
    }

    public void updateRegistrations(String message) {
        //todo: parse message
        String topic = "topic";
        ArrayList<RegExpWithPriority> regexplist = new ArrayList<>();
        regexplist.add(new RegExpWithPriority("*",0));

        Collections.sort(regexplist);
        registeredRegExps.put(topic, regexplist);

        if (!publishers.containsKey(topic)) {
            Publisher<std_msgs.String> pub = node.newPublisher(topic, std_msgs.String._TYPE);
            publishers.put(topic, pub);
        }
    }
}
