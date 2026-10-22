package com.mflow.music;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
import android.content.res.*;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import androidx.activity.*;
import androidx.annotation.*;
import androidx.annotation.experimental.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.arch.core.*;
import androidx.exifinterface.*;
import androidx.fragment.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.runtime.*;
import com.bumptech.glide.*;
import com.bumptech.glide.gifdecoder.*;
import com.mflow.music.databinding.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class SettingActivity extends AppCompatActivity {
	
	private SettingBinding binding;
	
	private Intent intent = new Intent();
	private RequestNetwork net;
	private RequestNetwork.RequestListener _net_request_listener;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = SettingBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		net = new RequestNetwork(this);
		
		binding.btnBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				finish();
			}
		});
		
		binding.btnNotifications.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				final android.content.SharedPreferences prefs = getSharedPreferences("app_notifs", MODE_PRIVATE);
				String rawJson = prefs.getString("notif_list_json", "");
				
				float density = getResources().getDisplayMetrics().density;
				android.util.TypedValue typedValue = new android.util.TypedValue();
				
				// حاوية MaterialCardView الرئيسية للديالوج بمعايير Material 3
				com.google.android.material.card.MaterialCardView cardView = new com.google.android.material.card.MaterialCardView(SettingActivity.this);
				cardView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
				android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
				android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
				));
				cardView.setCardElevation(0f);
				cardView.setRadius(28f * density); // حواف دائرية عصرية 28dp
				cardView.setStrokeWidth(0);
				
				// جلب لون السطح الحالي ديناميكياً من الثيم
				getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
				cardView.setCardBackgroundColor(typedValue.data);
				
				LinearLayout layout = new LinearLayout(SettingActivity.this);
				layout.setLayoutParams(new com.google.android.material.card.MaterialCardView.LayoutParams(
				com.google.android.material.card.MaterialCardView.LayoutParams.MATCH_PARENT,
				com.google.android.material.card.MaterialCardView.LayoutParams.WRAP_CONTENT
				));
				layout.setOrientation(LinearLayout.VERTICAL);
				int padding = (int)(24 * density);
				layout.setPadding(padding, padding, padding, padding);
				
				TextView txtTitle = new TextView(SettingActivity.this);
				txtTitle.setText("Recent Notifications");
				txtTitle.setTextSize(18);
				txtTitle.setTypeface(null, android.graphics.Typeface.BOLD);
				getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
				txtTitle.setTextColor(typedValue.data);
				layout.addView(txtTitle);
				
				android.widget.ScrollView scrollView = new android.widget.ScrollView(SettingActivity.this);
				scrollView.setVerticalScrollBarEnabled(false);
				LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 
				LinearLayout.LayoutParams.WRAP_CONTENT
				);
				scrollParams.setMargins(0, (int)(16 * density), 0, (int)(16 * density));
				scrollView.setLayoutParams(scrollParams);
				
				LinearLayout listContainer = new LinearLayout(SettingActivity.this);
				listContainer.setLayoutParams(new android.widget.ScrollView.LayoutParams(
				android.widget.ScrollView.LayoutParams.MATCH_PARENT,
				android.widget.ScrollView.LayoutParams.WRAP_CONTENT
				));
				listContainer.setOrientation(LinearLayout.VERTICAL);
				
				boolean hasItems = false;
				
				try {
					if (!rawJson.isEmpty()) {
						org.json.JSONArray notifArray = new org.json.JSONArray(rawJson);
						int count = Math.min(notifArray.length(), 3);
						
						for (int i = 0; i < count; i++) {
							org.json.JSONObject item = notifArray.getJSONObject(i);
							String itemTitle = item.optString("title", "Notification").trim();
							String itemBody = item.optString("body", "").trim();
							
							if (!itemBody.isEmpty() || !itemTitle.isEmpty()) {
								hasItems = true;
								
								// بطاقة الإشعار الداخلي بتنسيق Material 3 (SurfaceContainerHigh)
								com.google.android.material.card.MaterialCardView itemCard = new com.google.android.material.card.MaterialCardView(SettingActivity.this);
								
								LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.MATCH_PARENT, 
								LinearLayout.LayoutParams.WRAP_CONTENT
								);
								if (i > 0) {
									cardParams.setMargins(0, (int)(10 * density), 0, 0);
								}
								itemCard.setLayoutParams(cardParams);
								
								itemCard.setCardElevation(0f);
								itemCard.setRadius(16f * density);
								itemCard.setStrokeWidth(0);
								getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHigh, typedValue, true);
								itemCard.setCardBackgroundColor(typedValue.data);
								
								LinearLayout itemLayout = new LinearLayout(SettingActivity.this);
								itemLayout.setLayoutParams(new com.google.android.material.card.MaterialCardView.LayoutParams(
								com.google.android.material.card.MaterialCardView.LayoutParams.MATCH_PARENT,
								com.google.android.material.card.MaterialCardView.LayoutParams.WRAP_CONTENT
								));
								itemLayout.setOrientation(LinearLayout.VERTICAL);
								int itemPad = (int)(16 * density);
								itemLayout.setPadding(itemPad, itemPad, itemPad, itemPad);
								
								TextView tView = new TextView(SettingActivity.this);
								tView.setText(itemTitle.isEmpty() ? "Notification" : itemTitle);
								tView.setTextSize(14);
								tView.setTypeface(null, android.graphics.Typeface.BOLD);
								getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
								tView.setTextColor(typedValue.data);
								
								TextView bView = new TextView(SettingActivity.this);
								bView.setText(itemBody);
								bView.setTextSize(13);
								getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
								bView.setTextColor(typedValue.data);
								bView.setPadding(0, (int)(4 * density), 0, 0);
								
								// --- تفعيل الروابط القابلة للضغط ---
								android.text.util.Linkify.addLinks(bView, android.text.util.Linkify.WEB_URLS);
								bView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
								getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
								bView.setLinkTextColor(typedValue.data);
								
								itemLayout.addView(tView);
								itemLayout.addView(bView);
								itemCard.addView(itemLayout);
								
								listContainer.addView(itemCard);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (!hasItems) {
					TextView emptyTxt = new TextView(SettingActivity.this);
					emptyTxt.setText("No notifications found");
					emptyTxt.setTextSize(14);
					getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
					emptyTxt.setTextColor(typedValue.data);
					emptyTxt.setPadding(0, (int)(12 * density), 0, (int)(12 * density));
					listContainer.addView(emptyTxt);
				}
				
				scrollView.addView(listContainer);
				layout.addView(scrollView);
				
				TextView btnOk = new TextView(SettingActivity.this);
				btnOk.setText("Got it");
				getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
				btnOk.setTextColor(typedValue.data);
				btnOk.setTextSize(14);
				btnOk.setTypeface(null, android.graphics.Typeface.BOLD);
				btnOk.setGravity(android.view.Gravity.END);
				int btnPad = (int)(8 * density);
				btnOk.setPadding(btnPad, btnPad, btnPad, btnPad);
				
				LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 
				LinearLayout.LayoutParams.WRAP_CONTENT
				);
				btnOk.setLayoutParams(btnParams);
				
				layout.addView(btnOk);
				cardView.addView(layout);
				
				com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
				new com.google.android.material.dialog.MaterialAlertDialogBuilder(SettingActivity.this);
				builder.setView(cardView);
				
				final androidx.appcompat.app.AlertDialog dialog = builder.create();
				if (dialog.getWindow() != null) {
					dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
				}
				
				btnOk.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v) {
						dialog.dismiss();
					}
				});
				
				dialog.show();
				
				// ضبط عرض الديالوج
				if (dialog.getWindow() != null) {
					int dialogWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
					dialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
				}
				
				// إرسال طلب جلب الإشعارات لتحديث البيانات للمرة القادمة
				net.startRequestNetwork(RequestNetworkController.GET, 
				"https://script.google.com/macros/s/AKfycbyGwpgJ0oalMNfhjmE8CQrlfjzOZa2bCzBX-NRqiToAEewmViHSQrEbwhUYh7xE9aRgJg/exec", 
				"get_notif", 
				_net_request_listener);
				
			}
		});
		
		binding.btnFeedback.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				float density = getResources().getDisplayMetrics().density;
				android.util.TypedValue typedValue = new android.util.TypedValue();
				
				// حاوية MaterialCardView الرئيسية للديالوج بمعايير Material 3 (نفس هيكل ديالوج الإشعارات)
				com.google.android.material.card.MaterialCardView cardView = new com.google.android.material.card.MaterialCardView(SettingActivity.this);
				cardView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
				android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
				android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
				));
				cardView.setCardElevation(0f);
				cardView.setRadius(28f * density); // حواف دائرية عصرية 28dp
				cardView.setStrokeWidth(0);
				
				// جلب لون السطح الحالي ديناميكياً من الثيم
				getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
				cardView.setCardBackgroundColor(typedValue.data);
				
				LinearLayout layout = new LinearLayout(SettingActivity.this);
				layout.setLayoutParams(new com.google.android.material.card.MaterialCardView.LayoutParams(
				com.google.android.material.card.MaterialCardView.LayoutParams.MATCH_PARENT,
				com.google.android.material.card.MaterialCardView.LayoutParams.WRAP_CONTENT
				));
				layout.setOrientation(LinearLayout.VERTICAL);
				int padding = (int)(24 * density);
				layout.setPadding(padding, padding, padding, padding);
				
				TextView txtTitle = new TextView(SettingActivity.this);
				txtTitle.setText("Send Feedback");
				txtTitle.setTextSize(18);
				txtTitle.setTypeface(null, android.graphics.Typeface.BOLD);
				getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
				txtTitle.setTextColor(typedValue.data);
				layout.addView(txtTitle);
				
				final EditText input = new EditText(SettingActivity.this);
				input.setHint("Write your feedback here...");
				getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
				input.setTextColor(typedValue.data);
				getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
				input.setHintTextColor(typedValue.data);
				input.setTextSize(14f);
				
				// خلفية حقل الإدخال بتنسيق Material 3 الموحد (SurfaceContainerHigh)
				android.graphics.drawable.GradientDrawable inputBg = new android.graphics.drawable.GradientDrawable();
				getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainerHigh, typedValue, true);
				inputBg.setColor(typedValue.data);
				inputBg.setCornerRadius(16f * density);
				input.setBackground(inputBg);
				
				int inPad = (int)(16 * density);
				input.setPadding(inPad, inPad, inPad, inPad);
				
				LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 
				LinearLayout.LayoutParams.WRAP_CONTENT
				);
				inputParams.setMargins(0, (int)(16 * density), 0, (int)(20 * density));
				input.setLayoutParams(inputParams);
				layout.addView(input);
				
				LinearLayout btnLayout = new LinearLayout(SettingActivity.this);
				btnLayout.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
				));
				btnLayout.setOrientation(LinearLayout.HORIZONTAL);
				btnLayout.setGravity(android.view.Gravity.END);
				
				TextView btnCancel = new TextView(SettingActivity.this);
				btnCancel.setText("Cancel");
				getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
				btnCancel.setTextColor(typedValue.data);
				btnCancel.setTextSize(14);
				btnCancel.setTypeface(null, android.graphics.Typeface.BOLD);
				int btnPad = (int)(8 * density);
				btnCancel.setPadding(btnPad, btnPad, btnPad, btnPad);
				
				TextView btnSend = new TextView(SettingActivity.this);
				btnSend.setText("Send");
				getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
				btnSend.setTextColor(typedValue.data);
				btnSend.setTextSize(14);
				btnSend.setTypeface(null, android.graphics.Typeface.BOLD);
				btnSend.setPadding(btnPad, btnPad, btnPad, btnPad);
				
				btnLayout.addView(btnCancel);
				btnLayout.addView(btnSend);
				layout.addView(btnLayout);
				
				cardView.addView(layout);
				
				com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
				new com.google.android.material.dialog.MaterialAlertDialogBuilder(SettingActivity.this);
				builder.setView(cardView);
				
				final androidx.appcompat.app.AlertDialog dialog = builder.create();
				if (dialog.getWindow() != null) {
					dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
				}
				
				btnCancel.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v) {
						dialog.dismiss();
					}
				});
				
				btnSend.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v) {
						final String noteText = input.getText().toString().trim();
						final String deviceModel = android.os.Build.MODEL; // جلب رمز موديل الجهاز مثل SM-M356B
						
						if (!noteText.isEmpty()) {
							SketchwareUtil.showMessage(getApplicationContext(), "Sending...");
							dialog.dismiss();
							
							new Thread(new Runnable() {
								@Override
								public void run() {
									try {
										final HashMap<String, Object> params = new HashMap<>();
										params.put("device", deviceModel);
										params.put("user_note", noteText);
										
										runOnUiThread(new Runnable() {
											@Override
											public void run() {
												net.setParams(params, RequestNetworkController.REQUEST_PARAM);
												net.startRequestNetwork(RequestNetworkController.POST, 
												"https://script.google.com/macros/s/AKfycbyGwpgJ0oalMNfhjmE8CQrlfjzOZa2bCzBX-NRqiToAEewmViHSQrEbwhUYh7xE9aRgJg/exec", 
												"send_note", 
												_net_request_listener);
											}
										});
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}).start();
						} else {
							SketchwareUtil.showMessage(getApplicationContext(), "Please write something first!");
						}
					}
				});
				
				dialog.show();
				
				// ضبط عرض الديالوج بنفس المعيار الموحد (85% من عرض الشاشة)
				if (dialog.getWindow() != null) {
					int dialogWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
					dialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
				}
				
			}
		});
		
		binding.btnPrivacyPolicy.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				float density = getResources().getDisplayMetrics().density;
				android.util.TypedValue typedValue = new android.util.TypedValue();
				
				// حاوية MaterialCardView الرئيسية للديالوج بمعايير Material 3
				com.google.android.material.card.MaterialCardView cardContainer = new com.google.android.material.card.MaterialCardView(SettingActivity.this);
				cardContainer.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
				android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
				android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
				));
				cardContainer.setCardElevation(0f);
				cardContainer.setRadius(28f * density); // حواف دائرية عصرية 28dp
				cardContainer.setStrokeWidth(0);
				
				// جلب لون السطح الحالي ديناميكياً من الثيم
				getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
				cardContainer.setCardBackgroundColor(typedValue.data);
				
				android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(SettingActivity.this);
				linearLayout.setLayoutParams(new com.google.android.material.card.MaterialCardView.LayoutParams(
				com.google.android.material.card.MaterialCardView.LayoutParams.MATCH_PARENT,
				com.google.android.material.card.MaterialCardView.LayoutParams.WRAP_CONTENT
				));
				linearLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
				int p24 = (int) (24 * density);
				linearLayout.setPadding(p24, p24, p24, p24);
				
				android.widget.TextView tvTitle = new android.widget.TextView(SettingActivity.this);
				tvTitle.setText("Privacy Policy");
				getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
				tvTitle.setTextColor(typedValue.data);
				tvTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18.0f);
				tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
				linearLayout.addView(tvTitle);
				
				android.widget.ScrollView scrollView = new android.widget.ScrollView(SettingActivity.this);
				scrollView.setVerticalScrollBarEnabled(false);
				android.widget.LinearLayout.LayoutParams scrollParams = new android.widget.LinearLayout.LayoutParams(
				android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
				(int) (300 * density)
				);
				int p16 = (int) (16 * density);
				scrollParams.setMargins(0, p16, 0, 0);
				scrollView.setLayoutParams(scrollParams);
				
				android.widget.TextView tvContent = new android.widget.TextView(SettingActivity.this);
				tvContent.setText("Privacy Policy — Music Flow\n\n" +
				"Effective Date: September 19, 2026\n\n" +
				"Music Flow is a local audio player for Android developed by Hance. This Privacy Policy outlines how your data is handled when using the application.\n\n" +
				"1. Information Collection & Permissions\n\n" +
				"Music Flow does not collect, store, transmit, or sell any personal identifying information. You are not required to create an account, register, or provide personal details (such as name, phone number, or location).\n\n" +
				"To function properly as a media player, the app requires local storage permissions (e.g., READ_MEDIA_AUDIO or READ_EXTERNAL_STORAGE) strictly to scan, display, and play your local audio files.\n\n" +
				"2. Internet Connection & User Feedback\n\n" +
				"The core music playback functionality of Music Flow operates completely offline.\n\n" +
				"However, the app utilizes an internet connection solely to allow users to voluntarily submit feedback, error reports, or feature requests. Any feedback submitted is transmitted securely and is used exclusively by the developer to diagnose issues and improve the app.\n\n" +
				"3. Local Data Processing & Security\n\n" +
				"All processing of your media files, play histories, favorites, and playlists occurs entirely on your device. Music Flow does not upload your music files, metadata, or media library to any external servers, nor does it maintain a remote user profile database.\n\n" +
				"4. Language Support\n\n" +
				"Music Flow is currently available and supported exclusively in the English language.\n\n" +
				"5. Data Sharing & Third-Party Services\n\n" +
				"Music Flow does not share, rent, or sell user data to any third parties. The app contains no third-party tracking SDKs, data brokers, or advertising networks.\n\n" +
				"6. Data Control & Retention\n\n" +
				"Since all app settings, playlists, and user preferences are stored locally on your device, you maintain full control over them. Uninstalling the app or clearing its data via Android system settings will permanently delete all app-stored preferences.\n\n" +
				"7. Children's Privacy\n\n" +
				"Music Flow does not collect personal information from anyone, including children.\n\n" +
				"8. Changes to This Policy\n\n" +
				"This Privacy Policy may be updated to reflect future features or adjustments. Any revisions will be published with an updated \"Effective Date.\"\n\n" +
				"9. Developer & Contact Information\n\n" +
				"If you have questions, feedback, or concerns regarding this Privacy Policy or Music Flow, please contact:\n\n" +
				"App Name: Music Flow\n" +
				"Developer: Hance (Personal Developer)\n" +
				"Contact Email: hance16x@gmail.com");
				getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true);
				tvContent.setTextColor(typedValue.data);
				tvContent.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13.5f);
				scrollView.addView(tvContent);
				linearLayout.addView(scrollView);
				
				// زر "GOT IT!" في المنتصف وداخل إطار أنيق (Outlined Button Style)
				android.widget.TextView btnGotIt = new android.widget.TextView(SettingActivity.this);
				btnGotIt.setText("GOT IT!");
				getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
				int primaryColor = typedValue.data;
				btnGotIt.setTextColor(primaryColor);
				btnGotIt.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14.0f);
				btnGotIt.setTypeface(null, android.graphics.Typeface.BOLD);
				btnGotIt.setGravity(android.view.Gravity.CENTER);
				
				// تصميم الإطار للزر باستخدام setCornerRadius الصحيحة
				android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
				btnBg.setColor(android.graphics.Color.TRANSPARENT);
				btnBg.setStroke((int)(1.5f * density), primaryColor);
				btnBg.setCornerRadius(14f * density);
				btnGotIt.setBackground(btnBg);
				
				int btnPad = (int) (12 * density);
				btnGotIt.setPadding(btnPad, (int)(10 * density), btnPad, (int)(10 * density));
				
				android.widget.LinearLayout.LayoutParams btnParams = new android.widget.LinearLayout.LayoutParams(
				android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
				android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
				);
				btnParams.setMargins(0, p16, 0, 0);
				btnGotIt.setLayoutParams(btnParams);
				
				linearLayout.addView(btnGotIt);
				cardContainer.addView(linearLayout);
				
				// استخدام MaterialAlertDialogBuilder ليتطابق مع أسلوب ظهور بقية الديالوجات تماماً
				com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
				new com.google.android.material.dialog.MaterialAlertDialogBuilder(SettingActivity.this);
				builder.setView(cardContainer);
				
				final androidx.appcompat.app.AlertDialog dialog = builder.create();
				if (dialog.getWindow() != null) {
					dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
				}
				
				// تعريف حدث النقر على الزر بعد إنشاء الـ dialog لكي يتم التعرف عليه بنجاح
				btnGotIt.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v) {
						dialog.dismiss();
					}
				});
				
				dialog.show();
				
				if (dialog.getWindow() != null) {
					int dialogWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
					dialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
				}
				
			}
		});
		
		binding.btnContact.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setAction(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("https://t.me/X_hance_x"));
				startActivity(intent);
			}
		});
		
		binding.linear2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setAction(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("https://mflow.hance16x.workers.dev/"));
				startActivity(intent);
			}
		});
		
		_net_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
				final String _tag = _param1;
				final String _response = _param2;
				final HashMap<String, Object> _responseHeaders = _param3;
				if (_tag.equals("send_note")) {
					SketchwareUtil.showMessage(getApplicationContext(), "Feedback sent successfully!");
				} 
				else if (_tag.equals("get_notif")) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								if (_response == null || _response.trim().isEmpty()) return;
								
								String str = _response.trim();
								android.content.SharedPreferences prefs = getSharedPreferences("app_notifs", MODE_PRIVATE);
								
								int startArr = str.indexOf("[");
								int endArr = str.lastIndexOf("]");
								
								if (startArr != -1 && endArr > startArr) {
									String jsonArrStr = str.substring(startArr, endArr + 1);
									prefs.edit().putString("notif_list_json", jsonArrStr).apply();
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}).start();
				}
				
			}
			
			@Override
			public void onErrorResponse(String _param1, String _param2) {
				final String _tag = _param1;
				final String _message = _param2;
				
			}
		};
	}
	
	private void initializeLogic() {
		// 1. جلب لون الخلفية أو السطح (Surface) مباشرة من ثيم Material 3 الحالي
		android.util.TypedValue typedValue = new android.util.TypedValue();
		getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
		int backgroundColor = typedValue.data;
		
		// 2. تطبيق اللون على شريط الحالة وأزرار التنقل لتتطابق تماماً مع الثيم
		getWindow().setStatusBarColor(backgroundColor);
		getWindow().setNavigationBarColor(backgroundColor);
		
		// 3. ضبط لون الأيقونات (فواتح أو غوامق) تلقائياً حسب الوضع الحالي (داكن أو فاتح)
		boolean isDarkTheme = (getResources().getConfiguration().uiMode & 
		android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
		android.content.res.Configuration.UI_MODE_NIGHT_YES;
		
		androidx.core.view.WindowInsetsControllerCompat controller = 
		androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
		
		if (controller != null) {
			// إذا كان الثيم فاتحاً، تصبح الأيقونات داكنة والعكس صحيح لضمان وضوحها
			controller.setAppearanceLightStatusBars(!isDarkTheme);
			controller.setAppearanceLightNavigationBars(!isDarkTheme);
		}
		
		binding.txtTitle.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/bold.ttf"), 1);
		binding.textview17.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/bold.ttf"), 1);
		binding.textview18.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/bold.ttf"), 1);
		binding.txtNotificationsTitle.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/bold.ttf"), 1);
		binding.txtFeedbackTitle.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/bold.ttf"), 1);
		binding.txtCheckUpdates.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/bold.ttf"), 1);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		net.startRequestNetwork(RequestNetworkController.GET, 
		"https://script.google.com/macros/s/AKfycbyGwpgJ0oalMNfhjmE8CQrlfjzOZa2bCzBX-NRqiToAEewmViHSQrEbwhUYh7xE9aRgJg/exec", 
		"get_notif", 
		_net_request_listener);
		
	}
}