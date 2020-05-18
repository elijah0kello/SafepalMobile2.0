package com.unfpa.safepal.chatmodule;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.pixplicity.easyprefs.library.Prefs;
import com.unfpa.safepal.R;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static com.unfpa.safepal.Utils.Constants.ANONYMOUS_USER_NAME;
import static com.unfpa.safepal.Utils.Constants.MAC_ADDRESS;
import static com.unfpa.safepal.Utils.Utilities.getMacAddress;
import static com.unfpa.safepal.Utils.Utilities.getRandomString;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = ChatActivity.class.getSimpleName();
    private FirebaseFirestore db;
    private static final String USER = "user";
    private static final String CHAT = "chat";
    private EditText inputMessageEditView;
    private ImageButton sendButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private DocumentReference userDocument;
    private ChatUser chatUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        FirebaseApp.initializeApp(this);
        chatUser = new ChatUser(
                Prefs.getString(ANONYMOUS_USER_NAME, "User" + getRandomString()),
                Prefs.getString(MAC_ADDRESS, getMacAddress(this)));

        inputMessageEditView = findViewById(R.id.input_messsage);
        sendButton = findViewById(R.id.send);
        listView = findViewById(R.id.chats_list);

        sendButton.setOnClickListener(v -> {
            String message = inputMessageEditView.getText().toString();
            Timber.d("onClick: %s", message);
            saveChatDocumentData(message);
        });

        db = FirebaseFirestore.getInstance();
        saveUserDocumentData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        triggerNewMessageListener();
    }

    private void triggerNewMessageListener() {
        try {
            userDocument.collection(CHAT).addSnapshotListener((queryDocumentSnapshots, e) -> {
                if (e != null) {
                    Timber.e(e, "listen:error");
                    return;
                }
                displayMessagesFromServer(queryDocumentSnapshots);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayMessagesFromServer(@Nullable QuerySnapshot queryDocumentSnapshots) {
        Timber.d("displayMessagesFromServer: started");
        List<Chat> chats = queryDocumentSnapshots.toObjects(Chat.class);
        ArrayList<String> messages = new ArrayList<>();
        for (Chat chat : chats) {
            Timber.d("displayMessagesFromServer: %s", chat.getMessage());
            messages.add(chat.getName() + " : " + chat.getMessage());
        }
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, messages);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void saveChatDocumentData(String message) {
        Timber.d("saveChatDocumentData: started");
        Chat chat = new Chat(chatUser.getUsername(), message);

        userDocument.collection(CHAT)
                .add(chat)
                .addOnSuccessListener(documentReference -> Timber.d("DocumentSnapshot added with ID: %s", documentReference.getId()))
                .addOnFailureListener(e -> Timber.e(e, "Error adding document"));
    }

    private void saveUserDocumentData() {
        Timber.d("saveUserDocumentData: started");
        db.collection(USER)
                .add(chatUser)
                .addOnSuccessListener(documentReference -> {
                    Timber.d("DocumentSnapshot added user with ID: %s", documentReference.getId());
                    userDocument = documentReference;
                    triggerNewMessageListener();
                })
                .addOnFailureListener(e -> Timber.e(e, "Error adding document"));
    }
}
