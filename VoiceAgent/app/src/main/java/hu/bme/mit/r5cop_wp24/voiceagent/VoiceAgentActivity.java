package hu.bme.mit.r5cop_wp24.voiceagent;


import android.app.Dialog;
import android.content.Intent;
import android.speech.SpeechRecognizer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.common.base.Preconditions;

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
import java.util.List;
import java.util.Map;


public class VoiceAgentActivity extends RosActivity  {

    private String LOG_TAG = "VoiceAgent";

    ImageButton recButton;
    boolean isRecording = false;
    long recStartTime;
    TextView recResult;
    TextView regexpText;


    TextToSpeechNode tts;
    SpeechRecognitionNode sr;
    ShoppingListNode sln;

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
        recButton = (ImageButton)findViewById(R.id.buttonRec);
        regexpText = (TextView)findViewById(R.id.regexpText);
        recButton.setEnabled(false);
        recButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sr != null) {
                    if (!isRecording) {
                        isRecording = true;
                        sr.startListening();
                        recButton.setImageResource(R.drawable.ic_mic_off_black_48dp);

                        Log.d(LOG_TAG, "rec start");
                    } else {
                        isRecording = false;
                        sr.stopListening();
                        recButton.setImageResource(R.drawable.ic_mic_black_48dp);
                        Log.d(LOG_TAG, "rec stop");
                    }
                }
            }
        });


        ImageButton slButton = (ImageButton) findViewById(R.id.butonShoppingList);
        slButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(VoiceAgentActivity.this, ShoppingList.class);
               // startActivity(intent);

                sln.showDialog(VoiceAgentActivity.this);
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // If the Barcode Scanner returned a string then display that string.
        if (requestCode == ShoppingList.SHOPPING_LIST_QR_SCAN_NEW_ID) {
            if (resultCode == RESULT_OK) {
                String scanResultFormat = intent.getStringExtra("SCAN_RESULT_FORMAT");
                Preconditions.checkState(scanResultFormat.equals("TEXT_TYPE")
                        || scanResultFormat.equals("QR_CODE"));
                String contents = intent.getStringExtra("SCAN_RESULT");

                sln.addNewItem(new ShoppingList.ShoppingListItem(contents));
                sln.showDialog(VoiceAgentActivity.this);
            }
        }
        else {
            super.onActivityResult(requestCode,resultCode, intent);
        }
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
        String ownAddr = findOwnAddress();
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
                        sln = new ShoppingListNode(VoiceAgentActivity.this, connectedNode);
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

                            @Override
                            public void onReadyForSpeech(Bundle params) {
                            }

                            @Override
                            public void onBeginningOfSpeech() {
                            }

                            @Override
                            public void onEndOfSpeech() {
                            }

                            @Override
                            public void onRmsChanged(float rmsdB) {
                            }

                            @Override
                            public void onRegexpsChanged(final Map<String, List<SpeechRecognitionDispatcher.RegExpWithPriority>> registeredRegexps) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        StringBuilder sb = new StringBuilder();
                                        sb.append("Available commands:\n");
                                        for(String topic : registeredRegexps.keySet()) {
                                            for (SpeechRecognitionDispatcher.RegExpWithPriority rwp : registeredRegexps.get(topic)) {
                                                //sb.append(topic);
                                                //sb.append(": ");
                                                sb.append(rwp.regexp);
                                                //sb.append(" (");
                                                //sb.append(rwp.priority);
                                                //sb.append(")");
                                                sb.append("\n");
                                            }
                                        }
                                        regexpText.setText(sb.toString());
                                    }
                                });

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
