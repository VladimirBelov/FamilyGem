package app.familygem;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.folg.gedcom.model.Person;

import app.familygem.constant.Extra;
import app.familygem.util.FileUtil;

/**
 * This BroadcastReceiver has a double function:
 * - Receives intent from Notifier to create notifications
 * - Receives ACTION_BOOT_COMPLETED after reboot to restore notifications saved in settings.json
 */
public class NotifyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Sets again alarms after reboot
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            new Notifier(context, null, 0, Notifier.What.REBOOT);
        } else { // Creates notification
            String personId = intent.getStringExtra(Extra.PERSON_ID);
            int treeId = intent.getIntExtra(Extra.TREE_ID, 0);
            Intent notifyIntent = new Intent(context, TreesActivity.class)
                    .putExtra(Notifier.TREE_ID_KEY, treeId)
                    .putExtra(Notifier.PERSON_ID_KEY, personId);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, intent.getIntExtra(Extra.ID, 1),
                    notifyIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Notifier.CHANNEL_ID)
                    .setContentTitle(intent.getStringExtra(Extra.TITLE))
                    .setContentText(intent.getStringExtra(Extra.TEXT))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setSmallIcon(R.drawable.cherokee_tree);

            Bitmap photo = null;
            if (personId != null) {
                try {
                    Person person = Global.gc.getPerson(personId);
                    if (person != null) {
                        photo = FileUtil.INSTANCE.getMainImage(person, Global.gc, treeId, true);
                        if (photo != null) {
                            photo = FileUtil.INSTANCE.scaleBitmapPreservingAspectRatio(photo, 192);
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e("NotifyReceiver", "Error loading photo for " + personId, e);
                }
            }
            if (photo != null) {
                builder.setLargeIcon(photo);
            }

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            notificationManager.notify(intent.getIntExtra(Extra.ID, 1), builder.build());
        }
    }
}
