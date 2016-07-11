package hu.bme.mit.r5cop_wp24.voiceagent;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.ros.android.RosActivity;
import org.ros.internal.node.topic.PublisherIdentifier;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.DefaultSubscriberListener;
import org.ros.node.topic.Subscriber;
import org.ros.node.topic.SubscriberListener;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends RosActivity implements RecognitionListener, TextToSpeech.OnInitListener {

    private String LOG_TAG = "MainActivity";

    SpeechRecognizer speechRecognizer;
    Button recButton;
    boolean isRecording = false;
    TextView recResult;
    TextToSpeech tts;

    public MainActivity() {
        super("VoiceAgent", "VoiceAgent");
    }

    protected MainActivity(String notificationTicker, String notificationTitle) {
        super(notificationTicker, notificationTitle);
    }


    public class LanguageDetailsChecker extends BroadcastReceiver
    {
        private List<String> supportedLanguages;

        private String languagePreference;

        @Override
        public void onReceive(Context context, Intent intent)
        {
            Bundle results = getResultExtras(true);
            if (results.containsKey(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE))
            {
                languagePreference =
                        results.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE);
            }
            if (results.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES))
            {
                supportedLanguages =
                        results.getStringArrayList(
                                RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
                for(String s : supportedLanguages) {
                    Log.i(LOG_TAG, s);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //startMasterChooser();


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);

        tts = new TextToSpeech(this, this);


        recResult = (TextView)findViewById(R.id.recResult);

        recButton = (Button)findViewById(R.id.buttonRec);
        recButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
                    recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, MainActivity.this.getPackageName());
                    recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
                    speechRecognizer.startListening(recognizerIntent);
                    recButton.setText("STOP");
                    isRecording = true;

                    /*Intent detailsIntent =  new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
                    sendOrderedBroadcast(detailsIntent, null, new LanguageDetailsChecker(), null, Activity.RESULT_OK, null, null);
                    */
                }
                else {
                    speechRecognizer.stopListening();
                    recButton.setText("REC");
                    isRecording = false;
                }
            }
        });







    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.i(LOG_TAG, "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.i(LOG_TAG, "onRmsChanged");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
    }

    @Override
    public void onError(int error) {
        Log.i(LOG_TAG, "onError: " + error);
    }

    @Override
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = "";
        for (String result : matches)
            text += result + "\n";

        recResult.setText(text);

        if (matches.size() > 0) {
            Log.i(LOG_TAG, "tts");
            String t = matches.get(0);
            tts.speak(t, TextToSpeech.QUEUE_ADD, null, t);
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.i(LOG_TAG, "onPartialResults");
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = "";
        for (String result : matches)
            text += result + "\n";

        recResult.setText(text);



    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onInit(int status) {
        Log.i(LOG_TAG, "onInit");
        tts.setLanguage(new Locale("en_US"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        speechRecognizer.destroy();
        speechRecognizer = null;
        tts.shutdown();
        tts = null;
    }




    //ROS

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {

        Log.d(LOG_TAG,"ROS init");
        try {
            Log.d(LOG_TAG, getMasterUri().getHost().toString());
            Log.d(LOG_TAG, "" + getMasterUri().getPort());
            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress();
            //java.net.InetAddress local_network_address = Inet4Address.getByName("152.66.252.92");
            socket.close();
            NodeConfiguration nodeConfiguration =
                    NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());

            Log.d(LOG_TAG, getMasterUri().toString());
            Log.d(LOG_TAG, local_network_address.getHostAddress().toString());
            //nodeMainExecutor.execute(rosCameraPreviewView, nodeConfiguration);

            nodeMainExecutor.execute(new NodeMain() {
                @Override
                public GraphName getDefaultNodeName() {
                    return GraphName.of("VoiceAgent");
                }

                @Override
                public void onStart(ConnectedNode connectedNode) {
                    Log.d(LOG_TAG, "onStart");



                    Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber("Text2Speech", std_msgs.String._TYPE);
                    subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
                        @Override
                        public void onNewMessage(std_msgs.String message) {
                            Log.d(LOG_TAG, "I heard: \"" + message.getData() + "\"");
                            tts.speak(message.getData(), TextToSpeech.QUEUE_ADD, null, message.getData());
                        }
                    });


                }

                @Override
                public void onShutdown(Node node) {
                    Log.d(LOG_TAG, "onShutdown");
                }

                @Override
                public void onShutdownComplete(Node node) {
                    Log.d(LOG_TAG, "onShutdownComplete");
                }

                @Override
                public void onError(Node node, Throwable throwable) {
                    Log.d(LOG_TAG, "onError");
                }
            }, nodeConfiguration);
        } catch (IOException e) {
            // Socket problem
            Log.e(LOG_TAG, "socket error trying to get networking information from the master uri");
        }
    }
}
