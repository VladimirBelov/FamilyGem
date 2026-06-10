package app.familygem;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Parcelable;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import app.familygem.constant.Extra;

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
            Intent notifyIntent = new Intent(context, TreesActivity.class)
                    .putExtra(Notifier.TREE_ID_KEY, intent.getIntExtra(Extra.TREE_ID, 0))
                    .putExtra(Notifier.PERSON_ID_KEY, intent.getStringExtra(Extra.PERSON_ID));
            PendingIntent pendingIntent = PendingIntent.getActivity(context, intent.getIntExtra(Extra.ID, 1),
                    notifyIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Notifier.CHANNEL_ID)
                    .setContentTitle(intent.getStringExtra(Extra.TITLE))
                    .setContentText(intent.getStringExtra(Extra.TEXT))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setSmallIcon(R.drawable.cherokee_tree);

            if (intent.hasExtra(Extra.PERSON_PHOTO)) {
                Parcelable parcelable = intent.getParcelableExtra(Extra.PERSON_PHOTO);
                Bitmap bitmap = null;

                if (parcelable instanceof Bitmap) {
                    bitmap = (Bitmap) parcelable;
                } else if (parcelable instanceof BitmapDrawable) {
                    bitmap = ((BitmapDrawable) parcelable).getBitmap();
                }

                if (bitmap != null) {
                    builder.setLargeIcon(bitmap);
                }
            }

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(intent.getIntExtra(Extra.ID, 1), builder.build());
        }
    }
}
