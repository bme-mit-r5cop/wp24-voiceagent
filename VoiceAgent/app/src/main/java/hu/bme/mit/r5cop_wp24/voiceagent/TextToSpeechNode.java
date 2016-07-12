package hu.bme.mit.r5cop_wp24.voiceagent;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

import java.util.Locale;


/**
 * Created by steve on 7/12/16.
 */
public class TextToSpeechNode implements TextToSpeech.OnInitListener {

    private String LOG_TAG = "TextToSpeechNode";
    private final String topic = "Text2Speech";

    TextToSpeech tts;
    public TextToSpeechNode(Context context, ConnectedNode node) {
        tts = new TextToSpeech(context, this);

        Subscriber<std_msgs.String> subscriber = node.newSubscriber(topic, std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                Log.d(LOG_TAG, "I heard: \"" + message.getData() + "\"");
                tts.speak(message.getData(), TextToSpeech.QUEUE_ADD, null, message.getData());
            }
        });
    }

    @Override
    public void onInit(int status) {
        tts.setLanguage(new Locale("en_US"));
    }

    public void shutdown() {
        tts.shutdown();
    }
}