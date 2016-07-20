package hu.bme.mit.r5cop_wp24.voiceagent;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ListView;

import org.json.JSONException;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by steve on 2016.07.21..
 */
public class ShoppingListNode {

    private String LOG_TAG = "ShoppingListNode";
    private final String newItemTopic = "ShoppingListNewItem";
    private final String itemChanged = "ShoppingListItemChanged";

    Publisher<std_msgs.String> newItemPublisher;

    List<ShoppingList.ShoppingListItem> shoppingList;

    WeakReference<Dialog> shoppingListDialog;

    public ShoppingListNode(Context context, ConnectedNode node) {
        shoppingList = new ArrayList<>();
        shoppingList.add(new ShoppingList.ShoppingListItem("123-456-789"));
        shoppingList.add(new ShoppingList.ShoppingListItem("456-789-123"));


        newItemPublisher = node.newPublisher(newItemTopic, std_msgs.String._TYPE);

        Subscriber<std_msgs.String> subscriber = node.newSubscriber(itemChanged, std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                Log.d(LOG_TAG, "item changed received: " + message.getData());
                //TODO do something with the data

                //if the dialog is still visible, update the view
                Dialog sld = shoppingListDialog.get();
                if (sld != null) {
                    ShoppingList.notifyDataSetChanged(sld);
                }
            }
        });
    }

    public void showDialog(Activity activity) {
        Dialog sld= ShoppingList.createDialog(activity, shoppingList);
        sld.show();
        shoppingListDialog = new WeakReference<>(sld);
    }


    public void addNewItem(ShoppingList.ShoppingListItem shoppingListItem) {
        shoppingList.add(shoppingListItem);

        std_msgs.String str = newItemPublisher.newMessage();

        //TODO replace message whit ACL JSON stuff
        str.setData(shoppingListItem.id);
        newItemPublisher.publish(str);
    }
}
