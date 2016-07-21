package hu.bme.mit.r5cop_wp24.voiceagent;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.ros.message.MessageListener;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import acl.GeneralMessage;
import demo.acl.Product;
import demo.acl.ProductMessage;


/**
 * Created by steve on 2016.07.21..
 */
public class ShoppingListNode {

    private String LOG_TAG = "ShoppingListNode";
    private final String newItemTopic = "ItemCollectorAgent_AddItem";
    private final String itemPickupTopic = "PickupAgent_Pickup";
    private final String itemChangedTopic = "ShoppingListAgent_Control";

    Publisher<std_msgs.String> newItemPublisher;
    Publisher<std_msgs.String> pickupItemPublisher;

    List<ShoppingList.ShoppingListItem> shoppingList;

    OnShoppingListChangedListener oslcl;



    public interface OnShoppingListChangedListener {
        void onShoppingListChanged();
    }

    public ShoppingListNode(Context context, ConnectedNode node) {
        shoppingList = new ArrayList<>();
        //shoppingList.add(new ShoppingList.ShoppingListItem("123-456-789"));
        //shoppingList.add(new ShoppingList.ShoppingListItem("456-789-123"));


        newItemPublisher = node.newPublisher(newItemTopic, std_msgs.String._TYPE);
        pickupItemPublisher = node.newPublisher(itemPickupTopic, std_msgs.String._TYPE);

        Subscriber<std_msgs.String> subscriber = node.newSubscriber(itemChangedTopic, std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                Log.d(LOG_TAG, "item changed received: " + message.getData());
                try {
                    ProductMessage pm = new ProductMessage(message.getData());
                    changeItemState(pm.getProduct(), pm.getStatus());

                    if (oslcl != null) {
                        oslcl.onShoppingListChanged();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void setOnShoppingListChangedListener(OnShoppingListChangedListener l) {
        oslcl = l;
    }

    public List<ShoppingList.ShoppingListItem> getShoppingList() {
        return shoppingList;
    }

    private void changeItemState(Product p, String status) {
        switch(status) {
            case "item_added" :
                shoppingList.add(new ShoppingList.ShoppingListItem(p));
                break;
            case "at_product" :
                for(ShoppingList.ShoppingListItem item : shoppingList) {
                    if (item.p.compareTo(p)) {
                        item.status = ShoppingList.ShoppingListItem.Status.AT_PRODUCT;
                    }
                }
                break;
            case "pickup_success" :
                for(ShoppingList.ShoppingListItem item : shoppingList) {
                    if (item.p.compareTo(p)) {
                        item.status = ShoppingList.ShoppingListItem.Status.COLLECTED;
                    }
                }
                break;
            case "pickup_failure" :
                for(ShoppingList.ShoppingListItem item : shoppingList) {
                    if (item.p.compareTo(p)) {
                        item.status = ShoppingList.ShoppingListItem.Status.ADDED;
                    }
                }
                break;
        }
    }

    public void addNewItem(String id) {
        GeneralMessage gm = new GeneralMessage("ShoppingListNode", newItemTopic, id);
        std_msgs.String str = newItemPublisher.newMessage();
        str.setData(gm.toJson());
        newItemPublisher.publish(str);
    }

    public void scannedProductForPickup(String id) {
        GeneralMessage gm = new GeneralMessage("ShoppingListNode", itemPickupTopic, id);
        std_msgs.String str = pickupItemPublisher.newMessage();
        str.setData(gm.toJson());
        pickupItemPublisher.publish(str);
    }

    public void reset() {
        shoppingList.clear();
        if (oslcl != null) {
            oslcl.onShoppingListChanged();
        }
    }
}
