package hu.bme.mit.r5cop_wp24.voiceagent;

import android.util.Log;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.SubscriberListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import acl.AcceptedPattern;
import acl.GeneralMessage;
import acl.SubscribeMessage;
import acl.Text2SpeechMessage;

/**
 * Created by steve on 7/12/16.
 */
public class SpeechRecognitionDispatcher {

    private String LOG_TAG = "SpeechRecognitionDispatcher";

    public void reset() {
        registeredRegExps.clear();
        publishers.clear();
    }

    public static class RegExpWithPriority implements Comparable<RegExpWithPriority> {
        String topic;
        String regexp;
        int priority;

        public RegExpWithPriority(String regexp, int priority, String topic) {
            this.regexp = regexp;
            this.priority = priority;
            this.topic = topic;
        }

        public boolean matches(String recognizedString) {
            return recognizedString.matches(regexp);
        }

        @Override
        public int compareTo(RegExpWithPriority another) {
            return this.priority < another.priority ? -1 : (this.priority == another.priority ? 0 : 1);
        }

        @Override
        public boolean equals(Object x) {
            if (!RegExpWithPriority.class.isInstance(x))
                return false;
            RegExpWithPriority xx = (RegExpWithPriority)x;
            return regexp.equals(xx.regexp) && (priority == xx.priority) && topic.equals(xx.topic);
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 31).
                    append(regexp).
                    append(priority).
                    append(topic).
                    toHashCode();
        }

        public static RegExpWithPriority fromAcceptedPattern(AcceptedPattern ap, String topic) {
            return new RegExpWithPriority(ap.getMask(), ap.getPriorty(), topic);
        }
    }
    ConnectedNode node;

    List<RegExpWithPriority> registeredRegExps;
    HashMap<String, Publisher<std_msgs.String>> publishers;

    public SpeechRecognitionDispatcher(ConnectedNode node) {
        this.node = node;
        registeredRegExps = new ArrayList<>();
        publishers = new HashMap<>();


    }

    public void dispatch(String recognizedString) {
        ArrayList<String> matchingTopics = new ArrayList<>();

        int priority = Integer.MIN_VALUE;
        for (RegExpWithPriority rewp : registeredRegExps) {
            if (rewp.priority < priority)
                break;
            if (rewp.matches(recognizedString)) {
                priority = rewp.priority;
                matchingTopics.add(rewp.topic);
            }
        }


        for (String topic : matchingTopics) {
            Publisher<std_msgs.String> pub = publishers.get(topic);
            std_msgs.String str = pub.newMessage();

            GeneralMessage srm = new GeneralMessage("SpeechRecognitionNode", topic, recognizedString);

            String msg = srm.toJson(); //serialize message to string
            str.setData(msg);
            ScreenLogger.d(LOG_TAG, "Recognition sent: " + str.getData());
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

        //remove existing regexps for this topic
        List<RegExpWithPriority> deletelist = new ArrayList<>();
        for(RegExpWithPriority rx : registeredRegExps) {
            if (rx.topic.equals(topic)) {
                deletelist.add(rx);
            }
        }
        registeredRegExps.removeAll(deletelist);

        //add all new regexps
        for (AcceptedPattern ap : ssm.getAcceptedPatterns()) {
            RegExpWithPriority rx = RegExpWithPriority.fromAcceptedPattern(ap, topic);
            registeredRegExps.add(rx);
        }

        //order in descending order
        Collections.sort(registeredRegExps);
        Collections.reverse(registeredRegExps);


        if (!publishers.containsKey(topic)) {
            Publisher<std_msgs.String> pub = node.newPublisher(topic, std_msgs.String._TYPE);
            publishers.put(topic, pub);
        }
    }

    public List<RegExpWithPriority> getRegisteredRegexps() {
        return registeredRegExps;
    }
}
