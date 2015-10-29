package org.hao.puzzle54;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import org.hh.puzzle.port54.hall.R;

import java.util.Locale;

public class AppPrefUtil {
	public static boolean isPlayMusic(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getBoolean(context.getString(R.string.PREF_BACKGROUND_MUSIC_KEY), true);
	}
	public static void setPlayMusic(Context context, Editor editor, boolean isMusic) {
		editor.putBoolean(context.getString(R.string.PREF_BACKGROUND_MUSIC_KEY), isMusic);
	}
	
	public static boolean isPlaySound(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getBoolean(context.getString(R.string.PREF_SOUND_EFFECT_KEY), true);
	}
	public static void setPlaySound(Context context, Editor editor, boolean isSound) {
		editor.putBoolean(context.getString(R.string.PREF_SOUND_EFFECT_KEY), isSound);
	}
	
	public static boolean isFullScreenMode(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getBoolean(context.getString(R.string.PREF_SCREEN_FULL_KEY), true);
	}
	public static void setFullScreenMode(Context context, Editor editor, boolean isFullScreen) {
		editor.putBoolean(context.getString(R.string.PREF_SCREEN_FULL_KEY), isFullScreen);
	}

	public static boolean isRatedApp(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getBoolean(context.getString(R.string.PREF_APP_ISRATED_KEY), false);
	}
	public static void setRatedApp(Context context, Editor editor, boolean isRated) {
		editor.putBoolean(context.getString(R.string.PREF_APP_ISRATED_KEY), isRated);
	}

	public static boolean isMergedOldPref(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getBoolean(context.getString(R.string.PREF_HAS_MERGED_OLD_PREF_KEY), false);
	}
	public static void setMergedOldPref(Context context, Editor editor, boolean isMerged) {
		editor.putBoolean(context.getString(R.string.PREF_HAS_MERGED_OLD_PREF_KEY), isMerged);
	}
	
	public static int getRunTimesOfApp(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return Integer.parseInt(pref.getString(context.getString(R.string.PREF_APP_RUNTIMES_KEY), "0"));
	}
	public static void setRunTimesOfApp(Context context, Editor editor, int runTimes) {
		editor.putString(context.getString(R.string.PREF_APP_RUNTIMES_KEY), String.valueOf(runTimes));
	}

	public static String getAdBannerId(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(context.getString(R.string.PREF_AD_UNIT_ID_KEY), context.getString(R.string.ad_banner_id));
	}
	public static void setAdBannerId(Context context, Editor editor, String adBannerId) {
		editor.putString(context.getString(R.string.PREF_AD_UNIT_ID_KEY), adBannerId);
	}
	
	public static String getAdInterstitialId(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(context.getString(R.string.PREF_AD_INTERSTITIAL_ID_KEY), context.getString(R.string.ad_interstitial_id));
	}
	public static void setAdInterstitialId(Context context, Editor editor, String adInterstitialId) {
		editor.putString(context.getString(R.string.PREF_AD_INTERSTITIAL_ID_KEY), adInterstitialId);
	}
	
	public static String getOldApp(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(context.getString(R.string.PREF_OLD_APP_KEY), null);
	}
	public static void setOldApp(Context context, Editor editor, String oldAppPackageName) {
		editor.putString(context.getString(R.string.PREF_OLD_APP_KEY), oldAppPackageName);
	}

	public static int getApkVersionCode(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return Integer.parseInt(pref.getString(context.getString(R.string.PREF_APK_VERSION_CODE_KEY), "0"));
	}
	public static void setApkVersionCode(Context context, Editor editor, int versionCode) {
		editor.putString(context.getString(R.string.PREF_APK_VERSION_CODE_KEY), String.valueOf(versionCode));
	}

	public static String getLastPlayPackageCode(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(context.getString(R.string.PREF_LASTPLAY_PACKAGE_CODE_KEY), null);
	}
	public static void setLastPlayPackageCode(Context context, Editor editor, String lastPackageCode) {
		editor.putString(context.getString(R.string.PREF_LASTPLAY_PACKAGE_CODE_KEY), lastPackageCode);
	}
	public static void removeLastPlayPackageCode(Context context, Editor editor) {
		editor.remove(context.getString(R.string.PREF_LASTPLAY_PACKAGE_CODE_KEY));
		editor.remove(context.getString(R.string.PREF_LASTPLAY_SCOREID_KEY));
	}

	public static long getLastPlayScoreId(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return Long.parseLong(pref.getString(context.getString(R.string.PREF_LASTPLAY_SCOREID_KEY), "0"));
	}
	public static void setLastPlayScoreId(Context context, Editor editor, long scoreId) {
		editor.putString(context.getString(R.string.PREF_LASTPLAY_SCOREID_KEY), String.valueOf(scoreId));
	}

	public static String getAdsXmlLastModified(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(context.getString(R.string.PREF_ADS_XML_LASTMODIFIED_KEY), null);
	}
	public static void setAdsXmlLastModified(Context context, Editor editor, String lastModified) {
		editor.putString(context.getString(R.string.PREF_ADS_XML_LASTMODIFIED_KEY), lastModified);
	}

	public static String getPuzzlesXmlLastModified(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(context.getString(R.string.PREF_PUZZLES_XML_LASTMODIFIED_KEY), null);
	}
	public static void setPuzzlesXmlLastModified(Context context, Editor editor, String lastModified) {
		editor.putString(context.getString(R.string.PREF_PUZZLES_XML_LASTMODIFIED_KEY), lastModified);
	}
	
	public static String getAppsupdateXmlLastModified(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(context.getString(R.string.PREF_APPSUPDATE_XML_LASTMODIFIED_KEY), null);
	}
	public static void setAppsupdateXmlLastModified(Context context, Editor editor, String lastModified) {
		editor.putString(context.getString(R.string.PREF_APPSUPDATE_XML_LASTMODIFIED_KEY), lastModified);
	}

	public static int getApkNewVersionCode(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return Integer.parseInt(pref.getString(context.getString(R.string.PREF_APK_NEWVERSION_CODE_KEY), "0"));
	}
	public static void setApkNewVersionCode(Context context, Editor editor, int versionCode) {
		editor.putString(context.getString(R.string.PREF_APK_NEWVERSION_CODE_KEY), String.valueOf(versionCode));
	}

	public static String getApkNewVersionPackage(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(context.getString(R.string.PREF_APK_NEWVERSION_PACKAGE_KEY), null);
	}
	public static void setApkNewVersionPackage(Context context, Editor editor, String strPackage) {
		editor.putString(context.getString(R.string.PREF_APK_NEWVERSION_PACKAGE_KEY), strPackage);
	}

	public static String getApkNewVersionUri(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(context.getString(R.string.PREF_APK_NEWVERSION_URL_KEY), null);
	}
	public static void setApkNewVersionUri(Context context, Editor editor, String uri) {
		editor.putString(context.getString(R.string.PREF_APK_NEWVERSION_URL_KEY), uri);
	}

	public static String getApkNewVersionGoogleUri(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(context.getString(R.string.PREF_APK_NEWVERSION_GOOGLE_URL_KEY), null);
	}
	public static void setApkNewVersionGoogleUri(Context context, Editor editor, String uri) {
		editor.putString(context.getString(R.string.PREF_APK_NEWVERSION_GOOGLE_URL_KEY), uri);
	}

	public static String getThemeCode(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(context.getString(R.string.PREF_THEME_CODE_KEY), null);
	}
	public static void setThemeCode(Context context, Editor editor, String themeCode) {
		editor.putString(context.getString(R.string.PREF_THEME_CODE_KEY), themeCode);
	}

	public static String getThemeName(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(context.getString(R.string.PREF_THEME_NAME_KEY), null);
	}
	public static void setThemeName(Context context, Editor editor, String themeName) {
		editor.putString(context.getString(R.string.PREF_THEME_NAME_KEY), themeName);
	}

	public static long getApkNewVersionContentLength(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return Long.parseLong(pref.getString(context.getString(R.string.PREF_APK_NEWVERSION_CONTENTLENGTH_KEY), "-2"));
	}
	public static void setApkNewVersionContentLength(Context context, Editor editor, long contentLength) {
		editor.putString(context.getString(R.string.PREF_APK_NEWVERSION_CONTENTLENGTH_KEY), String.valueOf(contentLength));
	}

	public static String getApkNewVersionLastModified(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		return pref.getString(context.getString(R.string.PREF_APK_NEWVERSION_LASTMODIFIED_KEY), null);
	}
	public static void setApkNewVersionLastModified(Context context, Editor editor, String lastModified) {
		editor.putString(context.getString(R.string.PREF_APK_NEWVERSION_LASTMODIFIED_KEY), lastModified);
	}
	public static boolean hasNewApkVersion(Context context, SharedPreferences pref) {
		return getApkNewVersionCode(context, pref) > getApkVersionCode(context, pref);
	}
	
	public static String getDifficulty(Context context, SharedPreferences pref) {
		if(pref == null) pref = PreferenceManager.getDefaultSharedPreferences(context);
		String defaultValue = context.getString(R.string.PREF_DIFFICULTY_DEFAULT);
		MyApp myApp = (MyApp)context.getApplicationContext();
		DisplayMetrics dm = myApp.getDisplay();
		if(dm.widthPixels < AppConstants.SCREEN_WIDTH_MID) defaultValue = context.getString(R.string.PREF_DIFFICULTY_DEFAULT_SMALL);
		return pref.getString(context.getString(R.string.PREF_DIFFICULTY_KEY), defaultValue);
	}
	public static void setDifficulty(Context context, Editor editor, String difficulty) {
		editor.putString(context.getString(R.string.PREF_DIFFICULTY_KEY), difficulty);
	}
	
	public static int getRows(Context context, SharedPreferences pref) {
		String rowsXcols = getDifficulty(context, pref);
		return Integer.parseInt(rowsXcols.toLowerCase(Locale.US).split("x")[0]);
	}
	public static int getCols(Context context, SharedPreferences pref) {
		String rowsXcols = getDifficulty(context, pref);
		return Integer.parseInt(rowsXcols.toLowerCase(Locale.US).split("x")[1]);
	}
	
	@Deprecated
	public static String getMatchedDifficulty(Context context, int rows, int cols) {
		String[] arrDifficulty = context.getResources().getStringArray(R.array.difficulty_value);
        for (String anArrDifficulty : arrDifficulty) {
            if (anArrDifficulty.toLowerCase(Locale.getDefault()).equalsIgnoreCase(rows + "x" + cols)) {
                return anArrDifficulty;
            }
        }
		return null;
	}
}
