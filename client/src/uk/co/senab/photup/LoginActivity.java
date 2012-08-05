package uk.co.senab.photup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.donations.DonationsActivity;
import org.json.JSONException;
import org.json.JSONObject;

import uk.co.senab.photup.facebook.Session;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class LoginActivity extends Activity implements View.OnClickListener, DialogListener {

	static final int REQUEST_FACEBOOK_SSO = 100;

	private Facebook mFacebook;

	private View mAboutLogo;
	private Button mLoginBtn, mLogoutBtn, mLibrariesBtn, mSettingsBtn, mDonateBtn;
	private View mFacebookBtn, mTwitterBtn;
	private TextView mMessageTv;

	public void onClick(View v) {
		if (v == mLoginBtn) {
			loginToFacebook();
		} else if (v == mLogoutBtn) {
			showLogoutPrompt();
		} else if (v == mFacebookBtn) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.facebook_address))));
		} else if (v == mTwitterBtn) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.twitter_address))));
		} else if (v == mLibrariesBtn) {
			startActivity(new Intent(this, LicencesActivity.class));
		} else if (v == mSettingsBtn) {
			startActivity(new Intent(this, SettingsActivity.class));
		} else if (v == mAboutLogo) {
			onBackPressed();
		} else if (v == mDonateBtn) {
			startActivity(new Intent(this, DonationsActivity.class));
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (null == mFacebook) {
			mFacebook = new Facebook(Constants.FACEBOOK_APP_ID);
			mFacebook.setAuthorizeParams(this, REQUEST_FACEBOOK_SSO);
		}
		mFacebook.authorizeCallback(requestCode, resultCode, data);
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		mAboutLogo = findViewById(R.id.ll_about_logo);

		mLoginBtn = (Button) findViewById(R.id.btn_login);
		mLoginBtn.setOnClickListener(this);

		mLogoutBtn = (Button) findViewById(R.id.btn_logout);
		mLogoutBtn.setOnClickListener(this);

		mFacebookBtn = findViewById(R.id.tv_social_fb);
		mFacebookBtn.setOnClickListener(this);

		mTwitterBtn = findViewById(R.id.tv_social_twitter);
		mTwitterBtn.setOnClickListener(this);

		mLibrariesBtn = (Button) findViewById(R.id.btn_libraries);
		mLibrariesBtn.setOnClickListener(this);

		mSettingsBtn = (Button) findViewById(R.id.btn_settings);
		mSettingsBtn.setOnClickListener(this);

		mDonateBtn = (Button) findViewById(R.id.btn_donate);
		mDonateBtn.setOnClickListener(this);

		mMessageTv = (TextView) findViewById(R.id.tv_login_message);

		if (Constants.INTENT_NEW_PERMISSIONS.equals(getIntent().getAction())) {
			loginToFacebook();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		refreshUi();
	}

	private void loginToFacebook() {
		mFacebook = new Facebook(Constants.FACEBOOK_APP_ID);
		mFacebook.authorize(this, Constants.FACEBOOK_PERMISSIONS, BuildConfig.DEBUG ? Facebook.FORCE_DIALOG_AUTH
				: REQUEST_FACEBOOK_SSO, this);
	}

	private void logoutOfFacebook() {
		// Actual log out request
		Session session = Session.restore(this);
		if (null != session) {
			new AsyncFacebookRunner(session.getFb()).logout(getApplicationContext(),
					new AsyncFacebookRunner.SimpleRequestListener());
		}

		Session.clearSavedSession(this);
		refreshUi();
	}

	private void refreshUi() {
		Session session = Session.restore(this);
		if (null != session) {
			mMessageTv.setVisibility(View.GONE);
			mLoginBtn.setVisibility(View.GONE);
			mLogoutBtn.setText(getString(R.string.logout, session.getName()));
			mLogoutBtn.setVisibility(View.VISIBLE);
			mLibrariesBtn.setVisibility(View.VISIBLE);
			mSettingsBtn.setVisibility(View.VISIBLE);
			mAboutLogo.setOnClickListener(this);
		} else {
			mMessageTv.setText(R.string.welcome_message);
			mMessageTv.setVisibility(View.VISIBLE);
			mLoginBtn.setVisibility(View.VISIBLE);
			mLogoutBtn.setVisibility(View.GONE);
			mLibrariesBtn.setVisibility(View.GONE);
			mSettingsBtn.setVisibility(View.GONE);
			mAboutLogo.setOnClickListener(null);
		}
	}

	private void saveFacebookSession() {
		AsyncFacebookRunner fbRunner = new AsyncFacebookRunner(mFacebook);
		fbRunner.request("me", new RequestListener() {

			public void onComplete(String response, Object state) {
				try {
					JSONObject object = new JSONObject(response);
					String id = object.getString("id");
					String name = object.getString("name");

					Session session = new Session(mFacebook, id, name);
					session.save(getApplicationContext());

					setResult(RESULT_OK);
					finish();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			public void onFacebookError(FacebookError e, Object state) {
				e.printStackTrace();
			}

			public void onFileNotFoundException(FileNotFoundException e, Object state) {
				e.printStackTrace();
			}

			public void onIOException(IOException e, Object state) {
				e.printStackTrace();
			}

			public void onMalformedURLException(MalformedURLException e, Object state) {
				e.printStackTrace();
			}
		});
	}

	private void showLogoutPrompt() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.logout_prompt_title);
		builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				logoutOfFacebook();
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(android.R.string.cancel, null);

		builder.show();
	}

	public void onCancel() {
	}

	public void onComplete(Bundle values) {
		saveFacebookSession();
	}

	public void onError(DialogError e) {
		e.printStackTrace();
	}

	public void onFacebookError(FacebookError e) {
		e.printStackTrace();
	}

}
