package barqsoft.footballscores.service;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.ScoresWidget;

public class WidgetUpdateService extends IntentService {
    private final String TAG = WidgetUpdateService.class.getSimpleName();

    public WidgetUpdateService() {
        super("WidgetUpdaterService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean matchToday = false;
        Log.v(TAG, "Updating widget...");
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        ComponentName scoresWidget = new ComponentName(getApplicationContext(), ScoresWidget.class);
        RemoteViews remoteViews = new RemoteViews(
                getApplicationContext().getPackageName(),
                R.layout.scores_widget);

        Cursor cursor = null;
        try {
            cursor = getTodaysScores();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                matchToday = true;
                String homeTeam = cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.HOME_COL));
                String homeGoals = cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.HOME_GOALS_COL));
                String awayTeam = cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.AWAY_COL));
                String awayGoals = cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.AWAY_GOALS_COL));
                String date = cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.DATE_COL));
                String time = cursor.getString(cursor.getColumnIndex(DatabaseContract.scores_table.TIME_COL));
                setView(remoteViews, date, time, homeTeam, homeGoals, awayTeam, awayGoals);
                cursor.moveToNext();
            }
        }
        catch (Exception ex) {
            Log.e(TAG, "Exception querying scores", ex);

        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (!matchToday) {
            setView(remoteViews, getString(R.string.no_matches_today), "", "", "", "", "");
        }

        appWidgetManager.updateAppWidget(scoresWidget, remoteViews);
    }

    private Cursor getTodaysScores() {
        Uri todayScoresUri = DatabaseContract.scores_table.buildScoreWithDate();
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dates[] = {dateFormat.format(date)};

        return getContentResolver().query(todayScoresUri, null, null, dates, null);
    }

    private void setView(RemoteViews remoteViews,
                         String date, String time,
                         String homeTeam, String homeGoals,
                         String awayTeam, String awayGoals) {
                Log.v(TAG, "Setting score to " + homeTeam + " " + homeGoals + " - " + awayTeam + " " + awayGoals);
        remoteViews.setTextViewText(R.id.home_name, homeTeam);
        remoteViews.setTextViewText(R.id.away_name, awayTeam);
        remoteViews.setTextViewText(R.id.home_score, getScore(homeGoals));
        remoteViews.setTextViewText(R.id.away_score, getScore(awayGoals));
        remoteViews.setTextViewText(R.id.data_textview, date + " " + time);
    }

    private String getScore(String goals) {
        if ("-1".equals(goals)) { // game hasn't started yet
            return "-";
        }
        else {
            return goals;
        }
    }
}
