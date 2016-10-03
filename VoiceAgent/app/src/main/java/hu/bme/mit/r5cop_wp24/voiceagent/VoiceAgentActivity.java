package hu.bme.mit.r5cop_wp24.voiceagent;


import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.SpeechRecognizer;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
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

import demo.acl.Product;


public class VoiceAgentActivity extends RosActivity  {

    private static final boolean DEBUG = false;
    private String LOG_TAG = "VoiceAgent";

    ImageButton recButton;
    boolean isRecording = false;
    TextView chatText;
    TextView regexpText;


    TextToSpeechNode tts;
    SpeechRecognitionNode sr;
    ShoppingListNode sln;
    R5COPManagementNode management;

    Dialog shoppingList;

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

        chatText = (TextView) findViewById(R.id.chatText);
        chatText.setMovementMethod(new ScrollingMovementMethod());
        recButton = (ImageButton)findViewById(R.id.buttonRec);
        regexpText = (TextView)findViewById(R.id.regexpText);
        regexpText.setMovementMethod(new ScrollingMovementMethod());

        if (DEBUG) {
            new ScreenLogger(regexpText, this);
        }



        recButton.setEnabled(false);
        recButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sr != null) {
                    if (!isRecording) {
                        isRecording = true;
                        sr.startListening();
                        recButton.setImageResource(R.drawable.ic_mic_off_black_48dp);

                        ScreenLogger.d(LOG_TAG, "rec start");
                    } else {
                        isRecording = false;
                        sr.stopListening();
                        recButton.setImageResource(R.drawable.ic_mic_black_48dp);
                        ScreenLogger.d(LOG_TAG, "rec stop");
                    }
                }
            }
        });


        ImageButton slButton = (ImageButton) findViewById(R.id.butonShoppingList);
        slButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shoppingList.show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {

            } else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
            }
        }
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

                sln.addNewItem(contents);
                shoppingList.show();
            }
        }
        else if (requestCode == ShoppingList.SHOPPING_LIST_QR_SCAN_EXISTING_ID) {
            if (resultCode == RESULT_OK) {
                String scanResultFormat = intent.getStringExtra("SCAN_RESULT_FORMAT");
                Preconditions.checkState(scanResultFormat.equals("TEXT_TYPE")
                        || scanResultFormat.equals("QR_CODE"));
                String contents = intent.getStringExtra("SCAN_RESULT");

                sln.scannedProductForPickup(contents);
                shoppingList.show();
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
        ScreenLogger.destroy();
    }


    private void scrollToBottom(TextView tv) {
        final int scrollAmount = tv.getLayout().getLineTop(tv.getLineCount()) - tv.getHeight();
        if (scrollAmount > 0)
            tv.scrollTo(0, scrollAmount);
        else
            tv.scrollTo(0, 0);
    }

    NodeMainExecutor nodeMainExecutor;

    //ROS
    @Override
    protected void init(final NodeMainExecutor nodeMainExecutor) {
        this.nodeMainExecutor = nodeMainExecutor;
        ScreenLogger.d(LOG_TAG, "Master IP: " + getMasterUri().getHost().toString() + " port: " + getMasterUri().getPort());
        String ownAddr = findOwnAddress();
        ScreenLogger.d(LOG_TAG, "Own IP: " + ownAddr);

        if (ownAddr == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder b = new AlertDialog.Builder(VoiceAgentActivity.this);
                    b.setTitle("Error!").setMessage("Cannot connect to ROS core, make sure you are connected to VPN, and ROS core is running! Press OK to try again!").setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            (new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    init(nodeMainExecutor);
                                }
                            })).start();
                        }
                    }).create().show();
                }
            });
            return;
        }


        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(ownAddr, getMasterUri());

        nodeMainExecutor.execute(new NodeMain() {
            @Override
            public GraphName getDefaultNodeName() {
                return GraphName.of("VoiceAgentNode");
            }

            @Override
            public void onStart(final ConnectedNode connectedNode) {
                ScreenLogger.d(LOG_TAG, "onStart");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sr = new SpeechRecognitionNode(VoiceAgentActivity.this, connectedNode);

                        sr.setResultListener(new SpeechRecognitionNode.SpeechRecognitionNodeListener() {
                            @Override
                            public void onResults(Bundle results) {
                                ScreenLogger.i(LOG_TAG, "onResults");
                                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                                CharSequence text = chatText.getText();
                                text = TextUtils.concat(text, Html.fromHtml("<br><font color='red'><b>User:</b></font> " + matches.get(0)));
                                chatText.setText(text, TextView.BufferType.SPANNABLE);
                                scrollToBottom(chatText);
                            }

                            @Override
                            public void onPartialResults(Bundle results) {
                                ScreenLogger.i(LOG_TAG, "onPartialResults");
                                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

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
                            public void onRegexpsChanged(final List<SpeechRecognitionDispatcher.RegExpWithPriority> registeredRegexps) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!DEBUG) {
                                            StringBuilder sb = new StringBuilder();
                                            sb.append("Available commands:\n");
                                            for (SpeechRecognitionDispatcher.RegExpWithPriority rwp : registeredRegexps) {
                                                //sb.append(topic);
                                                //sb.append(": ");
                                                sb.append(rwp.regexp);
                                                //sb.append(" (");
                                                //sb.append(rwp.priority);
                                                //sb.append(")");
                                                sb.append("\n");
                                            }

                                            regexpText.setText(sb.toString());
                                        }
                                    }
                                });

                            }
                        });

                        management = new R5COPManagementNode(VoiceAgentActivity.this, connectedNode);
                        management.setOnManagementMessageListener(new R5COPManagementNode.OnManagementMessageListener() {
                            @Override
                            public void onManagementMessage(String s) {
                                sr.reset();
                                sln.reset();
                                tts.reset();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        chatText.setText("");
                                        regexpText.setText("");
                                    }
                                });
                            }
                        });


                        sln = new ShoppingListNode(VoiceAgentActivity.this, connectedNode);
                        shoppingList = ShoppingList.createDialog(VoiceAgentActivity.this, sln.getShoppingList());

                        sln.setOnShoppingListChangedListener(new ShoppingListNode.OnShoppingListChangedListener() {
                            @Override
                            public void onShoppingListChanged(final Runnable r) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ShoppingList.notifyDataSetChanged(shoppingList);
                                        r.run();
                                    }
                                });
                            }
                        });

                        tts = new TextToSpeechNode(VoiceAgentActivity.this, connectedNode);
                        tts.setOnTextToSpeechMessageListener(new TextToSpeechNode.OnTextToSpeechMessageListener() {
                            @Override
                            public void onTextToSpeechMessage(final String msg) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CharSequence text = chatText.getText();
                                        text = TextUtils.concat(text, Html.fromHtml("<br><font color='blue'><b>Robot:</b></font> " + msg));
                                        chatText.setText(text, TextView.BufferType.SPANNABLE);
                                        scrollToBottom(chatText);
                                    }
                                });
                            }
                        });
                        recButton.setEnabled(true);

                    }
                });

            }

            @Override
            public void onShutdown(Node node) {
                ScreenLogger.d(LOG_TAG, "onShutdown");
            }

            @Override
            public void onShutdownComplete(Node node) {
                ScreenLogger.d(LOG_TAG, "onShutdownComplete");
            }

            @Override
            public void onError(Node node, Throwable throwable) {
                ScreenLogger.d(LOG_TAG, "onError");
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
            ScreenLogger.e(LOG_TAG, "socket error trying to get networking information from the master uri");
        }
        return null;
    }
}
