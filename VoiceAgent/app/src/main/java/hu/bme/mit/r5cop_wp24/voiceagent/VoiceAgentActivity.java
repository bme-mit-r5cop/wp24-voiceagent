package hu.bme.mit.r5cop_wp24.voiceagent;


import android.speech.SpeechRecognizer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.ros.android.RosActivity;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;



public class VoiceAgentActivity extends RosActivity  {

    private String LOG_TAG = "VoiceAgent";

    Button recButton;
    boolean isRecording = false;
    TextView recResult;


    TextToSpeechNode tts;
    SpeechRecognitionNode sr;


    public VoiceAgentActivity() {
        super("VoiceAgent", "VoiceAgent");
    }

    protected VoiceAgentActivity(String notificationTicker, String notificationTitle) {
        super(notificationTicker, notificationTitle);
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        /*Intent detailsIntent =  new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        sendOrderedBroadcast(detailsIntent, null, new LanguageDetailsChecker(), null, Activity.RESULT_OK, null, null);
        */

        recResult = (TextView)findViewById(R.id.recResult);
        recButton = (Button)findViewById(R.id.buttonRec);
        recButton.setEnabled(false);
        recButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sr != null) {
                    if (!isRecording) {
                        sr.startListening();
                        recButton.setText("STOP");
                        isRecording = true;
                    } else {
                        sr.stopListening();
                        recButton.setText("REC");
                        isRecording = false;
                    }
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sr != null) {
            sr.shutdown();
            sr = null;
        }
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
    }

    //ROS
    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        Log.d(LOG_TAG, "Master IP: " + getMasterUri().getHost().toString() + " port: " + getMasterUri().getPort());
        String ownAddr = findOwnAddress();//"152.66.252.92"
        Log.d(LOG_TAG, "Own IP: " + ownAddr);
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(ownAddr, getMasterUri());

        nodeMainExecutor.execute(new NodeMain() {
            @Override
            public GraphName getDefaultNodeName() {
                return GraphName.of("VoiceAgentNode");
            }

            @Override
            public void onStart(final ConnectedNode connectedNode) {
                Log.d(LOG_TAG, "onStart");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tts = new TextToSpeechNode(VoiceAgentActivity.this, connectedNode);
                        sr = new SpeechRecognitionNode(VoiceAgentActivity.this, connectedNode);
                        recButton.setEnabled(true);
                        sr.setResultListener(new SpeechRecognitionNode.SpeechRecognitionNodeListener() {
                            @Override
                            public void onResults(Bundle results) {
                                Log.i(LOG_TAG, "onResults");
                                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                                String text = "";
                                for (String result : matches)
                                    text += result + "\n";

                                recResult.setText(text);
                            }

                            @Override
                            public void onPartialResults(Bundle results) {
                                Log.i(LOG_TAG, "onPartialResults");
                                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                                String text = "";
                                for (String result : matches)
                                    text += result + "\n";

                                recResult.setText(text);
                            }
                        });
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
    }

    private String findOwnAddress() {
        try {
            Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            InetAddress local_network_address = socket.getLocalAddress();
            socket.close();
            return local_network_address.getHostAddress();
        } catch (IOException e) {
            // Socket problem
            Log.e(LOG_TAG, "socket error trying to get networking information from the master uri");
        }
        return null;
    }
}
