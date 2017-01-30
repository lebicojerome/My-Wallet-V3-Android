package piuk.blockchain.android.data.notifications;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.Map;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.ui.contacts.list.ContactsListActivity;
import piuk.blockchain.android.ui.launcher.LauncherActivity;
import piuk.blockchain.android.util.ApplicationLifeCycle;
import piuk.blockchain.android.util.NotificationsUtil;

public class FcmCallbackService extends FirebaseMessagingService {

    private static final String TAG = FcmCallbackService.class.getSimpleName();

    public static final String EXTRA_CONTACTS_SERVICE = "contacts_service";
    public static final Subject<NotificationPayload> notificationSubject = PublishSubject.create();

    public FcmCallbackService() {
        // No-op
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            // Parse data, emit events
            NotificationPayload payload = new NotificationPayload(remoteMessage.getData());
            notificationSubject.onNext(payload);
            sendNotification(payload);
        }
    }

    public static Subject<NotificationPayload> getNotificationSubject() {
        return notificationSubject;
    }

    private void sendNotification(NotificationPayload payload) {
        if (ApplicationLifeCycle.getInstance().isForeground()
                && AccessState.getInstance().isLoggedIn()) {
            sendForegroundNotification(payload);
        } else {
            sendBackgroundNotification(payload);
        }
    }

    /**
     * Redirects the user to the {@link LauncherActivity} which will then handle the routing
     * appropriately.
     */
    private void sendBackgroundNotification(NotificationPayload payload) {
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notifyIntent = new Intent(getApplicationContext(), LauncherActivity.class);
        notifyIntent.putExtra(EXTRA_CONTACTS_SERVICE, true);
        PendingIntent intent =
                PendingIntent.getActivity(getApplicationContext(), 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_notification_transparent)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.blockchain_blue))
                .setContentTitle(payload.getTitle())
                .setTicker(payload.getBody())
                .setContentText(payload.getBody())
                .setContentIntent(intent)
                .setWhen(System.currentTimeMillis())
                .setSound(Uri.parse("android.resource://"
                        + getApplicationContext().getPackageName()
                        + "/"
                        + R.raw.beep))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setDefaults(Notification.DEFAULT_LIGHTS);

        notificationManager.notify(1337, builder.build());
    }

    /**
     * Redirects the user to the {@link ContactsListActivity}
     */
    private void sendForegroundNotification(NotificationPayload payload) {
        new NotificationsUtil(getApplicationContext()).setNotification(
                payload.getTitle(),
                payload.getBody(),
                payload.getBody(),
                R.drawable.ic_notification_transparent,
                R.drawable.ic_launcher,
                ContactsListActivity.class,
                1337);
    }

    @SuppressWarnings("WeakerAccess")
    public static class NotificationPayload {

        private String title;
        private String body;

        public NotificationPayload(Map<String, String> map) {
            if (map.containsKey("title")) {
                title = map.get("title");
            }

            if (map.containsValue("body")) {
                body = map.get("body");
            }
        }

        @Nullable
        public String getTitle() {
            return title;
        }

        @Nullable
        public String getBody() {
            return body;
        }
    }

}
