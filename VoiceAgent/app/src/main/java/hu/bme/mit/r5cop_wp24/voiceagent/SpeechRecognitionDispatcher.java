package hu.bme.mit.r5cop_wp24.voiceagent;

import org.json.JSONArray;
import org.json.JSONException;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.SubscriberListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import acl.AcceptedPattern;
import acl.SpeechRecognitionMessage;
import acl.SubscribeMessage;
import acl.Text2SpeechMessage;

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
            return recognizedString.matches(regexp);
        }

        @Override
        public int compareTo(RegExpWithPriority another) {
            return Integer.compare(this.priority, another.priority);
        }

        public static RegExpWithPriority fromAcceptedPattern(AcceptedPattern ap) {
            return new RegExpWithPriority(ap.getMask(), ap.getPriorty());
        }
    }
    ConnectedNode node;

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

            SpeechRecognitionMessage srm = new SpeechRecognitionMessage("SpeechRecognitionNode", topic, recognizedString);

            String msg = srm.toJson(); //serialize message to string
            str.setData(msg);
            pub.publish(str);
        }
    }

    public void updateRegistrations(String message) {
        SubscribeMessage ssm = null;
        try {
            ssm = new SubscribeMessage(message);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }


        String topic = ssm.getRecognitionTopic();
        ArrayList<RegExpWithPriority> regexplist = new ArrayList<>();
        //ArrayList<RegExpWithPriority> regexplist =
        //regexplist.add(new RegExpWithPriority("*",0));
        for (AcceptedPattern ap : ssm.getAcceptedPatterns()) {
            regexplist.add(RegExpWithPriority.fromAcceptedPattern(ap));
        }

        Collections.sort(regexplist);
        registeredRegExps.put(topic, regexplist);

        if (!publishers.containsKey(topic)) {
            Publisher<std_msgs.String> pub = node.newPublisher(topic, std_msgs.String._TYPE);
            publishers.put(topic, pub);
        }
    }
}
