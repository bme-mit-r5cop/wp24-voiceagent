package hu.bme.mit.r5cop_wp24.voiceagent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import org.ros.internal.message.Message;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

import java.util.ArrayList;

/**
 * Created by steve on 7/12/16.
 */
public class SpeechRecognitionNode implements RecognitionListener {

    public interface SpeechRecognitionNodeListener {
        void onResults(Bundle results);
        void onPartialResults(Bundle results);
    }

    private String LOG_TAG = "SpeechRecognitionNode";
    private final String topicRegister = "SpeechRecognitionRegister";

    SpeechRecognizer sr;
    SpeechRecognitionNodeListener srl;

    SpeechRecognitionDispatcher srd;

    public SpeechRecognitionNode(Context context, ConnectedNode node) {
        sr = SpeechRecognizer.createSpeechRecognizer(context);
        sr.setRecognitionListener(this);


        Subscriber<std_msgs.String> subscriber = node.newSubscriber(topicRegister, std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                srd.updateRegistrations(message.getData());
            }
        });

        srd = new SpeechRecognitionDispatcher(node);
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
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches.size() > 0) {
            String t = matches.get(0);
            srd.dispatch(t);
        }

        if (srl != null)
            srl.onResults(results);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        if (srl != null)
            srl.onPartialResults(partialResults);
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.i(LOG_TAG, "onEvent");
    }


    public void setResultListener(SpeechRecognitionNodeListener srl) {
        this.srl = srl;
    }

    public void startListening() {
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getClass().getPackage().getName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        sr.startListening(recognizerIntent);
    }

    public void stopListening() {
    }

    public void shutdown() {
        sr.destroy();
        sr = null;
    }
}
