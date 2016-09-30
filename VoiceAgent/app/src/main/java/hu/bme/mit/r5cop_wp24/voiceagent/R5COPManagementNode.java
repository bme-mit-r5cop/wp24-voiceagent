package hu.bme.mit.r5cop_wp24.voiceagent;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import org.json.JSONException;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

import acl.GeneralMessage;
import acl.Text2SpeechMessage;

/**
 * Created by steve on 7/21/16.
 */
public class R5COPManagementNode {
    private String LOG_TAG = "R5COPManagementNode";
    private final String topic = "R5COP_Management";

    OnManagementMessageListener l;

    TextToSpeech tts;
    public R5COPManagementNode(Context context, ConnectedNode node) {

        Subscriber<std_msgs.String> subscriber = node.newSubscriber(topic, std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                ScreenLogger.d(LOG_TAG, "R5COP_Management received: " + message.getData());
                try {
                    GeneralMessage msg = new GeneralMessage(message.getData());
                    String strmsg = msg.getContent();
                    if (l != null) {
                        l.onManagementMessage(strmsg);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void setOnManagementMessageListener(OnManagementMessageListener l) {
        this.l = l;
    }

    public static interface OnManagementMessageListener {
        public void onManagementMessage(String s);
    }
}
