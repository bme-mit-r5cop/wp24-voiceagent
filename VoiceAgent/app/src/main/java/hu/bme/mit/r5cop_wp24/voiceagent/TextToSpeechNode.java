package hu.bme.mit.r5cop_wp24.voiceagent;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import org.json.JSONException;
import org.ros.internal.node.topic.SubscriberIdentifier;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.DefaultPublisherListener;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.PublisherListener;
import org.ros.node.topic.Subscriber;

import java.util.HashMap;
import java.util.Locale;

import acl.GeneralMessage;
import acl.SubscribeMessage;
import acl.Text2SpeechMessage;


/**
 * Created by steve on 7/12/16.
 */
public class TextToSpeechNode implements TextToSpeech.OnInitListener {

    private String LOG_TAG = "TextToSpeechNode";
    private final String topic = "Text2Speech";
    private final String topicRepeat = "Text2SpeechRepeat";

    private boolean hasRepeatRegistered = false;
    private String lastMessage = "I havent said anything.";

    Publisher<std_msgs.String> repeatRegisterPublisher;
    Subscriber<std_msgs.String> repeatSubscriber;

    public interface OnTextToSpeechMessageListener {
        void onTextToSpeechMessage(String msg);
    }

    OnTextToSpeechMessageListener l;


    TextToSpeech tts;
    public TextToSpeechNode(Context context, final ConnectedNode node) {
        tts = new TextToSpeech(context, this);

        registerRepeat(node);

        Subscriber<std_msgs.String> subscriber = node.newSubscriber(topic, std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                ScreenLogger.d(LOG_TAG, "TTS received: " + message.getData());
                try {
                    if (!hasRepeatRegistered) {
                        registerRepeat(node);
                    }

                    Text2SpeechMessage ttsmsg = new Text2SpeechMessage(message.getData());
                    String speakString = ttsmsg.getText();
                    lastMessage = speakString;
                    if (Build.VERSION.SDK_INT >= 21) {
                        tts.speak(speakString, TextToSpeech.QUEUE_ADD, null, speakString);
                    }
                    else {
                        HashMap<String, String> mm = new HashMap<String, String>();
                        mm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, speakString);
                        tts.speak(speakString, TextToSpeech.QUEUE_ADD, mm);
                    }



                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if (l != null)
                    l.onTextToSpeechMessage(utteranceId);
            }

            @Override
            public void onDone(String utteranceId) {

            }

            @Override
            public void onError(String utteranceId) {

            }
        });
    }

    public void setOnTextToSpeechMessageListener(OnTextToSpeechMessageListener l) {
        this.l = l;
    }

    private void registerRepeat(final ConnectedNode node) {
        ScreenLogger.d(LOG_TAG, "registerRepeat");
        hasRepeatRegistered = true;

        if (repeatSubscriber != null)
            repeatSubscriber.shutdown();
        repeatSubscriber = node.newSubscriber(topicRepeat, std_msgs.String._TYPE);
        repeatSubscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                ScreenLogger.d(LOG_TAG, "TTS repeat received: " + message.getData());
            try {
                GeneralMessage ttsmsg = new GeneralMessage(message.getData());
                if (Build.VERSION.SDK_INT >= 21) {
                    tts.speak(lastMessage, TextToSpeech.QUEUE_ADD, null, lastMessage);
                }
                else {
                    HashMap<String, String> mm = new HashMap<String, String>();
                    mm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, lastMessage);
                    tts.speak(lastMessage, TextToSpeech.QUEUE_ADD, mm);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            }
        });

        if (repeatRegisterPublisher != null)
            repeatRegisterPublisher.shutdown();
        repeatRegisterPublisher = node.newPublisher("SpeechRecognitionRegister", std_msgs.String._TYPE);
        publishRegisterRepeat(repeatRegisterPublisher);
        repeatRegisterPublisher.addListener(new DefaultPublisherListener<std_msgs.String>() {
            @Override
            public void onNewSubscriber(Publisher<std_msgs.String> publisher, SubscriberIdentifier subscriberIdentifier) {
                ScreenLogger.d(LOG_TAG,"new SpeechRecognitionRegister subscriber");
                publishRegisterRepeat(publisher);
            }
        });
    }

    private void publishRegisterRepeat(Publisher<std_msgs.String> p) {
        ScreenLogger.d(LOG_TAG, "publishRegisterRepeat");
        std_msgs.String msg = p.newMessage();
        SubscribeMessage sm = new SubscribeMessage("TextToSpeechNode", "SpeechRecognitionRegister", topicRepeat);
        sm.addAcceptedPattern(".*repeat.*",-100);
        sm.addAcceptedPattern(".*say again.*",-100);
        msg.setData(sm.toJson());
        p.publish(msg);
    }


    @Override
    public void onInit(int status) {
        tts.setLanguage(new Locale("en_US"));
    }

    public void shutdown() {
        tts.shutdown();
    }

    public void reset() {
        lastMessage = "I havent said anything.";
        hasRepeatRegistered = false;
        tts.stop();
    }
}
