package com.azurenight.g2048;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.EventsClient;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayersClient;

public class MainMenuActivity extends AppCompatActivity
    implements PopupMenu.OnMenuItemClickListener {
  // request codes we use when invoking an external activity
  public static final int RC_UNUSED = 5001;
  public static final int RC_SIGN_IN = 9001;
  private static final int RC_ACHIEVEMENT_UI = 9003;
  private static final int RC_LEADERBOARD_UI = 9004;
  public static boolean mIsMainMenu = true;
  public static int mBackgroundColor = 0;
  private static int mRows = 4;
  private final String BACKGROUND_COLOR_KEY = "BackgroundColor";
  // Client used to sign in with Google APIs
  public GamesSignInClient mGoogleSignInClient;
  // Client variables
  public AchievementsClient mAchievementsClient;
  public LeaderboardsClient mLeaderboardsClient;
  public EventsClient mEventsClient;
  public PlayersClient mPlayersClient;

  public static int getRows() {
    return mRows;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main_menu);

    mIsMainMenu = true;

    Typeface ClearSans_Bold =
        Typeface.createFromAsset(getResources().getAssets(), "ClearSans-Bold.ttf");

    Button bt4x4 = findViewById(R.id.btn_start_4x4);
    Button bt5x5 = findViewById(R.id.btn_start_5x5);
    Button bt6x6 = findViewById(R.id.btn_start_6x6);

    bt4x4.setTypeface(ClearSans_Bold);
    bt5x5.setTypeface(ClearSans_Bold);
    bt6x6.setTypeface(ClearSans_Bold);

    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

    // Create the client used to sign in to Google services.
    mGoogleSignInClient = PlayGames.getGamesSignInClient(this);
  }

  private void signInSilently() {
    mGoogleSignInClient
        .isAuthenticated()
        .addOnCompleteListener(
            isAuthenticatedTask -> {
              boolean isAuthenticated =
                  (isAuthenticatedTask.isSuccessful()
                      && isAuthenticatedTask.getResult().isAuthenticated());
              if (isAuthenticated) {
                // Continue with Play Games Services
              } else {
                // If authentication fails, either disable Play Games Services
                // integration or
                // display a login button to prompt players to sign in.
                // Use`gamesSignInClient.signIn()` when the login button is clicked.
              }
            });
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.settings_color_picker:
        mRows = 4; // because of its GameView!
        startActivity(new Intent(MainMenuActivity.this, ColorPickerActivity.class));
        break;
      case R.id.settings_sign_out:
        break;
    }
    return false;
  }

  // Buttons:
  public void onButtonsClick(View view) {
    switch (view.getId()) {
      case R.id.btn_start_4x4:
        StartGame(4);
        break;
      case R.id.btn_start_5x5:
        StartGame(5);
        break;
      case R.id.btn_start_6x6:
        StartGame(6);
        break;
      case R.id.btn_show_achievements:
        try {
          onShowAchievementsRequested();
        } catch (Exception e) {
          Toast.makeText(this, getString(R.string.try_again), Toast.LENGTH_SHORT).show();
        }
        break;
      case R.id.btn_show_leaderboards:
        try {
          onShowLeaderboardsRequested();
        } catch (Exception e) {
          Toast.makeText(this, getString(R.string.try_again), Toast.LENGTH_SHORT).show();
        }
        break;
      case R.id.btn_share:
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(
            Intent.EXTRA_TEXT,
            getString(R.string.get_from_playstore) + "\n\n" + getString(R.string.url_google_play)+this.getPackageName());

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title)));
        break;
      case R.id.btn_rate:
          try {
              startActivity(new Intent(Intent.ACTION_VIEW,
                      Uri.parse("market://details?id=" + this.getPackageName())));
          } catch (android.content.ActivityNotFoundException e) {
              startActivity(new Intent(Intent.ACTION_VIEW,
                      Uri.parse(getString(R.string.url_google_play) + this.getPackageName())));
          }
        break;
      case R.id.btn_settings:
        PopupMenu popup = new PopupMenu(this, view);
        popup.setOnMenuItemClickListener(this); // to implement on click event on items of menu
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menus, popup.getMenu());
        popup.show();
        break;
      case R.id.btn_send_email:
        String[] TO = {getString(R.string.email_support_address)};
        Intent emailIntent = new Intent(Intent.ACTION_SEND);

        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");

        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));

        try {
          emailIntent.setPackage("com.google.android.gm");
          startActivity(emailIntent);
        } catch (ActivityNotFoundException ex) {
          emailIntent.setPackage("");
          startActivity(Intent.createChooser(emailIntent, getString(R.string.email_send_title)));
        } catch (Exception e) {
          Toast.makeText(
                  MainMenuActivity.this, getString(R.string.email_client_error), Toast.LENGTH_SHORT)
              .show();
        }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    mIsMainMenu = true;

    // Since the state of the signed in user can change when the activity is not active
    // it is recommended to try and sign in silently from when the app resumes.
    signInSilently();

    SaveColors();
    LoadColors();
  }

  private void SaveColors() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    SharedPreferences.Editor editor = settings.edit();

    if (mBackgroundColor < 0) editor.putInt(BACKGROUND_COLOR_KEY, mBackgroundColor);

    editor.apply();
  }

  private void LoadColors() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

    if (settings.getInt(BACKGROUND_COLOR_KEY, mBackgroundColor) < 0)
      mBackgroundColor = settings.getInt(BACKGROUND_COLOR_KEY, mBackgroundColor);
    else mBackgroundColor = getResources().getColor(R.color.colorBackground);
  }

  private void StartGame(int rows) {
    mRows = rows;
    mIsMainMenu = false;
    startActivity(new Intent(MainMenuActivity.this, MainActivity.class));
  }

  public void onShowAchievementsRequested() {
    PlayGames.getAchievementsClient(this)
        .getAchievementsIntent()
        .addOnSuccessListener(intent -> startActivityForResult(intent, RC_ACHIEVEMENT_UI));
  }

  public void onShowLeaderboardsRequested() {
    PlayGames.getLeaderboardsClient(this)
        .getAllLeaderboardsIntent()
        .addOnSuccessListener(intent -> startActivityForResult(intent, RC_LEADERBOARD_UI));
  }
}
