package barqsoft.footballscores;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.RemoteViews;

public class ScoresWidgetConfigure extends ActionBarActivity {

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public ScoresWidgetConfigure() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            RemoteViews remoteViews = new RemoteViews(
                    getApplicationContext().getPackageName(),
                    R.layout.scores_widget);
            AppWidgetManager.getInstance(this).updateAppWidget(mAppWidgetId,
                    remoteViews);

            Intent resultIntent = new Intent();
            resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultIntent);
        }

        finish();
    }
}
