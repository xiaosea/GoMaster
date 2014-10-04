package de.agrothe.go;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.fish.gomaster.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.agrothe.util.AssetsManager;
import de.agrothe.util.Generics;

import static de.agrothe.go.Globals.AppInfo;
import static de.agrothe.go.Globals._appInfo;
import static de.agrothe.go.Globals._autoSaveGamePathFileName;
import static de.agrothe.go.Globals._boardView;
import static de.agrothe.go.Globals._externalInputPathFileName;
import static de.agrothe.go.Globals._gtp;
import static de.agrothe.go.Globals._mainActivity;
import static de.agrothe.go.Globals._mainHandler;
import static de.agrothe.go.Globals._resources;
import static de.agrothe.go.Globals._scoreView;
import static de.agrothe.util.Logging.buidLogTag;
import static de.agrothe.util.Logging.isEnabledFor;
import static de.agrothe.util.Logging.log;

public
class MainActivity
	extends Activity
{
public static final
String
	_PACKAGE_NAME = MainActivity.class.getPackage ().getName (),
	_LOG_TAG = buidLogTag (_PACKAGE_NAME);

private static final
String
	_GNUGO_SO_LIBRARY_NAME = "gnuGo-3.8",
	_GNUGO_THREAD_NAME = "gnuGo";

private static final
int
	_GNUGO_MEMORY_SIZE = 8;

static final
int
	_SHOW_ESTIMATED_SCORE = -1,
	_SHOW_MESSAGE = -2;

static
enum MainCommand
{
	SHOW_MOVE,
	SHOW_CAPTURES,
	ENABLE_PASS_MENU,
	SHOW_WAIT_PROGRESS,
	SHOW_SCORE,
	ENABLE_UNDO_MENU,
	SHOW_COLLECTION_LIST,
	SHARE_GAME;

	int _cmd;
}
private static final
Map <Integer, MainCommand> _cmdMessagesMap;
static
{
	final MainCommand[] values = MainCommand.values ();
	final Map <Integer, MainCommand> cmdMessagesMap =
		_cmdMessagesMap = Generics.newHashMap (values.length);
	int numMessage = 0;
	for (final MainCommand message : values)
	{
		cmdMessagesMap.put (message._cmd = numMessage++, message);
	}
}

private
SharedPreferences _sharedPreferences;

private
String
	_saveLoadGamesDirName,
	_preferencesBoardSizeKey,
	_preferencesHandicapKey,
	_preferencesKomiKey,
	_preferencesChineseRulesKey,
	_preferencesLevelKey,
	_preferencesPlayerBlackHumanKey,
	_preferencesPlayerWhiteHumanKey;

private
Looper _gnuGoLooper;

private
Thread _gnuGoThread;

private
GameInfo _gameInfo;

private
List <File> _goProblems;

private
ProgressDialog _progressDialog;

private
TextView
	_capturesRowTextView,
	_blackCapturesTextView, _whiteCapturesTextView,
	_blackMoveTextView, _whiteMoveTextView,
	_blackScoreTextView, _whiteScoreTextView,
	_moveRowTextView, _messageScoreTextView;

private
TextView[] _scoreViewTextViews;

private
TableRow _scoreTableRow, _messageTableRow;

private
ProgressBar _blackMoveProgressBar, _whiteMoveProgressBar;

private
MenuItem
	_saveLoadMenuItem,
	_passMenuItem,
	_undoMenuItem, _redoMenuItem,
	_restartMenuItem;

private
boolean _undoEnabled = false, _redoEnabled = false;

private
float _3rowsTextSize, _4rowsTextSize;

private
Toast _undoRedoHint;

private
CharSequence
	_undoHintText, _redoHintText,
	_noUndoHintText, _noRedoHintText,
	_yourTurnText;

private
Dialog _changeGameDialog;

private
View _changeGameApplyButton;

static
FileFilter _sgfFileFilter = null;

private native static
void initGTP (float pMemory);

native static
String playGTP (String pInput);

native static
void setRules (int chineseRules);

static
{
	System.loadLibrary (MainActivity._GNUGO_SO_LIBRARY_NAME);
	initGTP (_GNUGO_MEMORY_SIZE);
}


private AdView mAdView;// Added by xiaosea、AdMob

public
void onCreate ( // 0
	final Bundle pSavedInstanceState
	)
{
	super.onCreate (pSavedInstanceState);
    setVolumeControlStream(AudioManager.STREAM_MUSIC);// Added by xiaosea
	_mainActivity = this;
	_appInfo = new AppInfo ();
	final Resources resources = _resources = getResources ();

	_saveLoadGamesDirName = resources.getString (R.string.app_name);
	_autoSaveGamePathFileName =
		getFileStreamPath (resources.getString (R.string.autoSaveFileName)).
			toString ();
	_sharedPreferences = PreferenceManager.getDefaultSharedPreferences (this);
	_preferencesBoardSizeKey = resources.getString (
		R.string.preferencesBoardSizeKey);
	_preferencesHandicapKey = resources.getString (
		R.string.preferencesHandicapKey);
	_preferencesKomiKey = resources.getString (R.string.preferencesKomiKey);
	_preferencesChineseRulesKey =
		resources.getString (R.string.preferencesChineseRulesKey);
	_preferencesLevelKey = resources.getString (R.string.preferencesLevelKey);
	_preferencesPlayerBlackHumanKey = resources.getString (
		R.string.preferencesPlayerBlackHunanKey);
	_preferencesPlayerWhiteHumanKey = resources.getString (
		R.string.preferencesPlayerWhiteHunanKey);

	_undoHintText = resources.getText (R.string.undoHintText);
	_redoHintText = resources.getText (R.string.redoHintText);
	_noUndoHintText = resources.getText (R.string.noUndoHintText);
	_noRedoHintText = resources.getText (R.string.noRedoHintText);
	_yourTurnText = resources.getText (R.string.yourTurnText);

	setContentView (R.layout.main);
	// 2

	final String textViewTag = resources.getString (R.string.moveTextViewTag),
		progressBarViewTag = resources.getString (R.string.moveProgressBarTag);
	View moveView = findViewById (R.id.blackMoveCell);
	_blackMoveTextView = (TextView)moveView.findViewWithTag (textViewTag);
	_blackMoveProgressBar = (ProgressBar)moveView.
		findViewWithTag (progressBarViewTag);
	moveView = findViewById (R.id.whiteMoveCell);
	_whiteMoveTextView = (TextView)moveView.findViewWithTag (textViewTag);
	_whiteMoveProgressBar = (ProgressBar)moveView.
		findViewWithTag (progressBarViewTag);
	_capturesRowTextView = (TextView)findViewById (R.id.capturesRowTextView);
	_blackCapturesTextView =
		(TextView)findViewById (R.id.blackCapturesTextView);
	_whiteCapturesTextView =
		(TextView)findViewById (R.id.whiteCapturesTextView);
	_scoreTableRow = (TableRow)findViewById (R.id.scoreTableRow);
	_messageTableRow = (TableRow)findViewById (R.id.messageTableRow);
	_blackScoreTextView = (TextView)findViewById (R.id.blackScoreTextView);
	_whiteScoreTextView = (TextView)findViewById (R.id.whiteScoreTextView);
	_moveRowTextView = (TextView)findViewById (R.id.moveRowTextView);
	_messageScoreTextView = (TextView)findViewById (R.id.messageScoreTextView);
 	_undoRedoHint = makeToast (this, "");

	_scoreViewTextViews = new TextView[] {
		(TextView)findViewById (R.id.blackColorTextView),
		(TextView)findViewById (R.id.whiteColorTextView),
		(TextView)findViewById (R.id.scoreRowTextView),
		_blackMoveTextView,
		_whiteMoveTextView,
		_capturesRowTextView,
		_blackCapturesTextView,
		_whiteCapturesTextView,
		_blackScoreTextView,
		_whiteScoreTextView,
		_moveRowTextView,
		_messageScoreTextView
	};

	findViewById (R.id.MenuButton).setOnClickListener (
		new View.OnClickListener ()
		{
			@Override
			public
			void onClick (final View pView)
			{
				openOptionsMenu ();
			}
		}
	);
    findViewById (R.id.MenuButton).setVisibility(View.INVISIBLE);// Added by xiaosea

	final View scoreView = _scoreView = findViewById (R.id.scoreView);
	((ScoreView)scoreView)._gestureDetector =
		new GestureDetector (this,
			new GestureDetector.SimpleOnGestureListener ()
	{
		public
		boolean onFling (
			final MotionEvent pMotionEvent,
			final MotionEvent pMotionEvent1,
			final float pVelo,
			final float pVelo1
			)
		{
			final CharSequence toastText;
			final boolean undo =
				pMotionEvent.getRawX () - pMotionEvent1.getRawX () > 0;
			if (undo)
			{
				if (_undoEnabled)
				{
					undo ();
					return true;
				}
				else
				{
					toastText = _noUndoHintText;
				}
			}
			else if (_redoEnabled)
			{
				redo ();
				return true;
			}
			else
			{
				toastText = _noRedoHintText;
			}
			showUndoRedoHint (toastText);
			return true;
		}
	});

	_mainHandler = new Handler ()
	{
		public
		void handleMessage (
			final Message pMessage
			)
		{
			final MainCommand cmd =
				_cmdMessagesMap.get (pMessage.what);
			if (cmd == null)
			{
				return;
			}
			switch (cmd)
			{
			case SHOW_MOVE:
				showMove (pMessage.arg1 != 0, (String)pMessage.obj);
				break;
			case SHOW_CAPTURES:
				showCaptures (pMessage.arg1, pMessage.arg2);
				break;
			case ENABLE_PASS_MENU:
				enablePassMenu ((Boolean)pMessage.obj);
				break;
			case ENABLE_UNDO_MENU:
				final Object obj = pMessage.obj;
				enableUndoMenu (obj == null ? null : (GameInfo)obj);
				break;
			case SHOW_WAIT_PROGRESS:
				showWaitProgress ((String)pMessage.obj);
				break;
			case SHOW_SCORE:
				showScore (pMessage.arg1, pMessage.arg2, pMessage.obj);
				break;
			case SHOW_COLLECTION_LIST:
				showCollectionList ();
				break;
			case SHARE_GAME:
				shareGame ((Uri)pMessage.obj);
				break;
			}
		}};

	final HandlerThread gtpThread = new HandlerThread (_GNUGO_THREAD_NAME);
	_gnuGoThread = gtpThread;
	gtpThread.start ();
	final Looper gtpLooper = _gnuGoLooper = gtpThread.getLooper ();
	if (gtpLooper == null)
	{
		return;
	}
	_gtp = new Gtp (
		new Handler (gtpLooper)
		{
			public
			void handleMessage (
				final Message pMessage
				)
			{
				_gtp.handleMessage (pMessage);
			}
		});

    // Added by xiaosea
    // AdMob
    TableLayout tableLayout = (TableLayout)_scoreViewTextViews[0].getParent().getParent();
    mAdView = new AdView(this);
    mAdView.setAdUnitId("ca-app-pub-2334745760937499/2443747566");
    mAdView.setAdSize(AdSize.BANNER);
    tableLayout.addView(mAdView, 0);
    mAdView.loadAd(new AdRequest.Builder().build());
}

// Added by xiaosea
// AdMob
@Override
protected void onPause() {
    mAdView.pause();
    super.onPause();
}

// Added by xiaosea
// AdMob
@Override
protected void onResume() {
    super.onResume();
    mAdView.resume();
}

@Override
public
void onStart ()
{
	super.onStart ();
	if (_gameInfo == null)
	{
		onNewIntent (getIntent ());
	}
}

@Override
public
void onNewIntent (
	final Intent pIntent
	)
{
	super.onNewIntent (pIntent);
	if (pIntent == null || !Intent.ACTION_VIEW.equals (pIntent.getAction ()))
	{
		return;
	}
	final Uri uri = pIntent.getData ();
	if (uri == null)
	{
		return;
	}
	try
	{
		if (isEnabledFor (_LOG_TAG, Log.INFO))
		{
			log (_LOG_TAG, Log.INFO, "got intent data uri: '" + uri + "'");
		}
		final String scheme = uri.getScheme ();
		final File inputFile = AssetsManager.copyFile (
			"content".equals (scheme) ?
				getContentResolver ().openInputStream (uri) :
				new URL (scheme, uri.getHost (), uri.getPort (),
					uri.getEncodedPath ()).
						openConnection ().getInputStream (),
			getFileStreamPath (uri.getLastPathSegment ()).toString ());
		if (inputFile != null)
		{
			inputFile.deleteOnExit ();
			_externalInputPathFileName = inputFile.getAbsolutePath ();
			if (_gameInfo != null)
			{
				newGame ();
			}
		}
	}
	catch (final Exception e)
	{
		if (isEnabledFor (_LOG_TAG, Log.WARN))
		{
			log (_LOG_TAG, Log.WARN,
				"failed to copy intent data file : '" + e + "'");
		}
	}
}

protected
void onDestroy ()
{
    mAdView.destroy();// Added by xiaosea、AdMob
	_gnuGoLooper.quit ();
	final Thread thread = _gnuGoThread;
	while (true)
	{
		try
		{
			thread.join ();
			break;
		}
		catch (final InterruptedException e) {}
	}
	super.onDestroy ();
}

public
boolean onCreateOptionsMenu (
	final Menu pMenu
	)
{
	super.onCreateOptionsMenu (pMenu);
	getMenuInflater ().inflate (R.menu.menu, pMenu);
	_saveLoadMenuItem = pMenu.findItem (R.id.menuSaveLoad);
	_passMenuItem = pMenu.findItem (R.id.menuPass);
	final MenuItem
		undoMenuItem = _undoMenuItem = pMenu.findItem (R.id.menuUndo);
	_redoMenuItem = pMenu.findItem (R.id.menuRedo);
	_restartMenuItem = pMenu.findItem (R.id.menuToStart);
	final Drawable undoDrawable = undoMenuItem.getIcon ();
	final int width = undoDrawable.getIntrinsicWidth (),
		height = undoDrawable.getIntrinsicHeight ();
	final Bitmap redoBitmap = Bitmap.createBitmap (width, height,
		Bitmap.Config.ARGB_8888);
	final Canvas canvas = new Canvas (redoBitmap);
	undoDrawable.setBounds (0, 0, width, height);
	undoDrawable.draw (canvas);
	final Matrix matrix = new Matrix ();
	matrix.preScale (-1, 1);
	try
	{
		_redoMenuItem.setIcon (new BitmapDrawable (_resources,
			Bitmap.createBitmap (redoBitmap, 0, 0,
				width, height, matrix, false)));
	}
	catch (final Exception e) {}
	final GameInfo gameInfo = _gameInfo;
	if (gameInfo != null)
	{
		enablePassMenu (!Gtp.playerIsMachine (gameInfo)
			&& !gameInfo.bothPlayersPassed ());
		enableUndoMenu (gameInfo);
	}
	return true;
}

public
boolean onPrepareOptionsMenu (
	final Menu pMenu
	)
{
	super.onPrepareOptionsMenu (pMenu);
	enableMenuItem (_saveLoadMenuItem, storageCardMounted ());
    // Added by xiaosea
    pMenu.findItem (R.id.menuInfo).setVisible(false);
    pMenu.findItem (R.id.menuExit).setVisible(false);
	return true;
}

static
void enableMenuItem (
	final MenuItem pMenuItem,
	final boolean pEnable
	)
{
	if (pMenuItem == null)
	{
		return;
	}
//	pMenuItem.setVisible (pEnable);
	pMenuItem.setEnabled (pEnable);
}

public
boolean onOptionsItemSelected (
	final MenuItem pItem
	)
{
	switch (pItem.getItemId ())
	{
	case R.id.menuNewGame:
        // modified by xiaosea
//		if (_changeGameDialog == null)
//		{
//			initChangeGameDialog ();
//		}
//		showChangedGameDialog ();
        newGame(restoreGameInfo(new GameInfo()), true);// Added by xiaosea
		return true;
	case R.id.menuPass:
		showMove (_gameInfo._playerBlackMoves, getPassedText (false));
		nextMove (GameInfo.Passed._Passed);
		return true;
	case R.id.menuUndo:
		showUndoRedoHint (_undoHintText);
		undo ();
		return true;
	case R.id.menuRedo:
		showUndoRedoHint (_redoHintText);
		redo ();
		return true;
	case R.id.menuSave:
		saveGame (null);
		return true;
	case R.id.menuLoad:
		loadGame ();
		return true;
	case R.id.menuInfo:
		showInfo ();
		return true;
	case R.id.menuToStart:
		restartGame ();
		return true;
	/* too slow (takes minutes) in most situations
	case R.id.menuShowTerritory:
		showTerritory ();
		return true;
	*/
	case R.id.menuShareGame:
		gtpShareGame ();
		return true;
	case R.id.menuExit:
		System.runFinalizersOnExit (true);
		System.exit (0);
		return true;
	}
	return false;
}

private
enum SpinnerEnum
{
	BoardSize (R.id.newGameDialogBoardSizeSpinner, R.array.boardSizes),
	Handicap (R.id.newGameDialogHandicapSpinner, R.array.handicaps),
	PlayerBlackWhite (R.id.newGameDialogHumanPlaysSpinner,
		R.array.playerBlackWhiteValues, true),
	Strength (R.id.newGameDialogStrengthSpinner, R.array.strengths, true),
	Komi (R.id.newGameDialogKomiSpinner, R.array.komis, true),
	Scoring (R.id.newGameDialogScoringSpinner, R.array.scorings);

	final
	int _spinnerResId;

	private final
	int _valuesResId;

	final
	boolean _mustChange;

	private
	Spinner _spinner;

	private
	TypedArray
		_values,
		_playerBlackHumanValues,
		_playerWhiteHumanValues;

	private
	Map <Object, Integer> _spinnerEntriesMap;

	boolean _changed;

	SpinnerEnum (
		final int pSpinnerResId,
		final int pResId
		)
	{
		this (pSpinnerResId, pResId, false);
	}

	SpinnerEnum (
		final int pSpinnerResId,
		final int pResId,
		final boolean pMustChange
		)
	{
		_spinnerResId = pSpinnerResId;
		_valuesResId = pResId;
		_mustChange = pMustChange;
	}

	void init (
		final Resources pResources,
		final Spinner pSpinner
		)
	{
		_spinner = pSpinner;
		final TypedArray values = _values =
			pResources.obtainTypedArray (_valuesResId);
		int numEntries = values.length ();
		Map <Object, Integer> spinnerEntriesMap = _spinnerEntriesMap;
		if (spinnerEntriesMap == null)
		{
			spinnerEntriesMap = _spinnerEntriesMap =
				Generics.newHashMap (numEntries);
		}
		for (int idx=0; idx < numEntries; idx++)
		{
			Object value = "";
			switch (values.peekValue (idx).type)
			{
			case TypedValue.TYPE_STRING:
				value = values.getString (idx);
				break;
			case TypedValue.TYPE_FLOAT:
				value = values.getFloat (idx, 0);
				break;
			case TypedValue.TYPE_INT_DEC:
			case TypedValue.TYPE_INT_HEX:
				value = values.getInt (idx, 0);
				break;
			case TypedValue.TYPE_INT_BOOLEAN:
				value = values.getBoolean (idx, false);
				break;
			}
			spinnerEntriesMap.put (value, idx);
		}
		if (this == PlayerBlackWhite)
		{
			_playerBlackHumanValues = pResources.obtainTypedArray (
				R.array.playerBlackHumanValues);
			_playerWhiteHumanValues = pResources.obtainTypedArray (
				R.array.playerWhiteHumanValues);
		}
	}

	void setSpinnerSelection (
		final GameInfo pGameInfo
		)
	{
		Object val = 0;
		switch (this)
		{
		case BoardSize:
			val = pGameInfo._boardSize;
			break;
		case Handicap:
			val = pGameInfo._handicap;
			break;
		case PlayerBlackWhite:
			_spinner.setSelection (_values.getInteger (
				(pGameInfo._playerBlackHuman ? 2 : 0)
					+ (pGameInfo._playerWhiteHuman ? 1 : 0), 0));
			return;
		case Strength:
			val = pGameInfo._level;
			break;
		case Komi:
			val = pGameInfo._komi;
			break;
		case Scoring:
			val = pGameInfo._chineseRules;
			break;
		}
		_spinner.setSelection (_spinnerEntriesMap.get (val));
	}

	void setHideStatus (
		final GameInfo pGameInfo,
		int pRow
		)
	{
		Object value = 0;
		switch (this)
		{
		case BoardSize:
			value = pGameInfo._boardSize;
			break;
		case Handicap:
			value = pGameInfo._handicap;
			break;
		case PlayerBlackWhite:
			value = pRow;
			pRow = (pGameInfo._playerBlackHuman ? 2 : 0)
				+ (pGameInfo._playerWhiteHuman ? 1 : 0);
			break;
		case Strength:
			value = pGameInfo._level;
			break;
		case Komi:
			value = pGameInfo._komi;
			break;
		case Scoring:
			value = pGameInfo._chineseRules;
			break;
		}
		_changed = pRow != _spinnerEntriesMap.get (value);
	}

	void setGameInfoValue (
		final GameInfo pGameInfo
		)
	{
		final int pos = _spinner.getSelectedItemPosition ();
		switch (this)
		{
		case BoardSize:
			pGameInfo._boardSize = _values.getInteger (pos, 0);
			break;
		case Handicap:
			pGameInfo._handicap = _values.getInteger (pos, 0);
			break;
		case PlayerBlackWhite:
			pGameInfo._playerBlackHuman =
				_playerBlackHumanValues.getBoolean (pos, true);
			pGameInfo._playerWhiteHuman =
				_playerWhiteHumanValues.getBoolean (pos, true);
			break;
		case Strength:
			pGameInfo._level = _values.getInteger (pos, 0);
			break;
		case Komi:
			pGameInfo._komi = _values.getString (pos);
			break;
		case Scoring:
			pGameInfo._chineseRules = _values.getBoolean (pos, false);
			break;
		}
	}
}

private
void initChangeGameDialog ()
{
	final ViewStub viewStub = new ViewStub (this, R.layout.new_game);
	final Dialog dialog = _changeGameDialog = new Dialog (this);
	dialog.getWindow ().requestFeature (Window.FEATURE_NO_TITLE);
	dialog.setContentView (viewStub);
	final View view = viewStub.inflate ();

	final Resources resources = _resources;
	final SpinnerEnum [] spinnerEnums = SpinnerEnum.values ();
	final Map <Spinner, SpinnerEnum> spinnerSpinnerEnumMap =
		Generics.newHashMap (spinnerEnums.length);
	for (final SpinnerEnum spinnerEnum : spinnerEnums)
	{
		final Spinner spinner = (Spinner)view.findViewById (
			spinnerEnum._spinnerResId);
		spinnerEnum.init (resources, spinner);
		spinnerSpinnerEnumMap.put (spinner, spinnerEnum);
	}
	final View applyButton = _changeGameApplyButton =
		view.findViewById (R.id.newGameDialogApplyButton);

	final AdapterView.OnItemSelectedListener hideChangeSettingsListener =
		new AdapterView.OnItemSelectedListener ()
		{
			public
			void onItemSelected (
				final AdapterView <?> pAdapterView,
				final View pView,
				final int pPosition,
				long pRow
				)
			{
				final GameInfo gameInfo = _gameInfo;
				if (gameInfo._invalid)
				{
					return;
				}
				//noinspection SuspiciousMethodCalls
				spinnerSpinnerEnumMap.get (pAdapterView).setHideStatus (
					gameInfo, (int)pRow);
				boolean showApplyButton = false;
				for (final SpinnerEnum item : spinnerEnums)
				{
					final boolean changed = item._changed;
					if (item._mustChange)
					{
						if (changed)
						{
							showApplyButton = true;
						}
					}
					else if (changed)
					{
						showApplyButton = false;
						break;
					}
				}
				applyButton.setEnabled (showApplyButton);
			}

			public
			void onNothingSelected (
				final AdapterView <?> pAdapterView
				)
			{
			}
		};
	for (final Spinner spinner : spinnerSpinnerEnumMap.keySet ())
	{
		spinner.setOnItemSelectedListener (hideChangeSettingsListener);
	}

	final View.OnClickListener onClickListener =
		new View.OnClickListener ()
		{
			public
			void onClick (
				final View pView
				)
			{
				final GameInfo gameInfo = new GameInfo ();
				for (final SpinnerEnum spinnerEnum : spinnerEnums)
				{
					spinnerEnum.setGameInfoValue (gameInfo);
				}
				newGame (storeGameInfo (gameInfo), pView != applyButton);
				dialog.dismiss ();
			}
		};
	applyButton.setOnClickListener (onClickListener);
	view.findViewById (R.id.newGameDialogStartButton).
		setOnClickListener (onClickListener);
}

static private
void clearChangedGameDialogSpinnerChangedStatus ()
{
	for (final SpinnerEnum item : SpinnerEnum.values ())
	{
		item._changed = false;
	}
}

private
void showChangedGameDialog ()
{
	_changeGameApplyButton.setEnabled (false);
	clearChangedGameDialogSpinnerChangedStatus ();
	final GameInfo gameInfo = _gameInfo;
	for (final SpinnerEnum item : SpinnerEnum.values ())
	{
		item.setSpinnerSelection (gameInfo);
	}
	_changeGameDialog.show ();
}

private
void undo ()
{
	final GameInfo gameInfo = _gameInfo;
	if (!gameInfo._invalid)
	{
		_gtp.undoMove (gameInfo);
	}
}

private
void redo ()
{
	_gtp.redoMove (_gameInfo);
}

private
void showTerritory ()
{
	_gtp.showTerritory (_gameInfo);
}

void shareGame (
	final Uri pUri
	)
{
	if (pUri == null)
	{
		return;
	}
	final Intent shareIntent = new Intent (Intent.ACTION_SEND);
    shareIntent.addFlags (Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    shareIntent.setType ("text/*");
    shareIntent.putExtra (Intent.EXTRA_STREAM, pUri);
	startActivity (shareIntent);
}

void gtpShareGame ()
{
	final GameInfo gameInfo = _gameInfo;
	showWait4Move2FinishMessage (gameInfo);
	_gtp.shareGame (gameInfo);
}

private
void restartGame ()
{
	_gtp.restartGame (_gameInfo);
}

String getDateFileName ()
{
	final Date now = new Date ();
	return formatFileName (DateFormat.getDateFormat (this).format (now)
		+ " " + DateFormat.getTimeFormat (this).format (now));
}

private
void saveGame (
	final String pFilename
	)
{
	final Resources resources = _resources;
	final EditText input = new EditText (this);
	input.setLayoutParams (new ViewGroup.LayoutParams (
		ViewGroup.LayoutParams.FILL_PARENT,
		ViewGroup.LayoutParams.FILL_PARENT));
	input.setSelectAllOnFocus (true);
	final View focusView = input.focusSearch (View.FOCUS_LEFT);
	if (focusView != null)
	{
		focusView.requestFocus ();
	}
	final String sgfExtension = getSgfFileExtension ();
	final GameInfo gameInfo = _gameInfo;
	String fileName = pFilename;
	if (fileName == null && !gameInfo._isCollection)
	{
		final String sgfFileName = gameInfo._sgfFileName;
		if (sgfFileName != null)
		{
			fileName = sgfFileName.substring (
				sgfFileName.lastIndexOf (File.separator) +1);
			fileName = fileName.substring (0, fileName.indexOf (sgfExtension));
		}
	}
	if (fileName == null)
	{
		fileName = getDateFileName ();
	}
	input.setText (fileName);
	new AlertDialog.Builder (this).
		setTitle (R.string.menuSaveLabel).
		setIcon (R.drawable.saveLoadMenuIcon).
		setView (input).
		setPositiveButton (R.string.menuSaveLabel,
			new DialogInterface.OnClickListener ()
			{
				public
				void onClick (
					final DialogInterface pDialogInterface,
					final int pWhichButton
					)
				{
					final File saveLoadGamesDir = getSaveLoadGamesDir ();
					final Editable inputText = input.getText ();
					String sgfFileName = null;
					if (saveLoadGamesDir == null
						|| inputText == null
						|| ((sgfFileName = inputText.toString ()) == null)
						|| ((sgfFileName = formatFileName (sgfFileName.trim ()))
							.length () == 0))
					{
						final String fileName = sgfFileName;
						showMessage (resources.getString (
							R.string.invalidFileNameAlertMessage,
							(saveLoadGamesDir == null ?
								"" : saveLoadGamesDir + "/")
							+ (sgfFileName == null ? "" : sgfFileName))).
								setOnDismissListener (
									new DialogInterface.OnDismissListener ()
									{
										public
										void onDismiss (
											final DialogInterface
												pDialogInterface)
										{
											saveGame (fileName);
										}
									}
								);
						return;
					}
					final File sgfFile = new File (saveLoadGamesDir,
						sgfFileName + sgfExtension);
					final String path = sgfFile.getAbsolutePath ();
					if (sgfFile.exists ())
					{
						final String fileName = sgfFileName;
						final DialogInterface.OnClickListener clickListener =
							new DialogInterface.OnClickListener ()
							{
								public
								void onClick (
									final DialogInterface pDialogInterface,
									final int pWhichButton
									)
								{
									if (pWhichButton ==
										DialogInterface.BUTTON_POSITIVE)
									{
										gtpSaveGame (path);
									}
									else
									{
										saveGame (fileName);
									}
								}
							};
						new AlertDialog.Builder (MainActivity.this).
							setTitle (R.string.menuSaveLabel).
							setIcon (R.drawable.saveLoadMenuIcon).
							setMessage (resources.getString (
								R.string.fileAlreadyExistsMessage, path,
								resources.getString (
									R.string.overwriteButtonLabel))).
							setNegativeButton (android.R.string.cancel,
								clickListener).
							setPositiveButton (R.string.overwriteButtonLabel,
								clickListener).
							show ();
						return;
					}
					gtpSaveGame (path);
				}}).
		show ();
}

private
void gtpSaveGame (
	final String pSgfFilePath
	)
{
	final GameInfo gameInfo = _gameInfo;
	showWait4Move2FinishMessage (gameInfo);
	gameInfo._sgfFileName = pSgfFilePath;
	_gtp.saveGame (gameInfo);
}

private static
String formatFileName (
	final String pFileName
	)
{
	return pFileName.replaceAll ("[ /:*?\"<>|\\\\]", "_");
}

String getSgfFileExtension ()
{
	return _resources.getString (R.string.sgfFileExtension);
}

private
void loadGame ()
{
	final GameInfo gameInfo = _gameInfo;
	if (gameInfo._isCollection)
	{
		showCollectionList ();
		return;
	}
	final File saveLoadGamesDir = getSaveLoadGamesDir ();
	if (saveLoadGamesDir == null)
	{
		return;
	}
	final Resources resources = _resources;
	if (_goProblems == null)
	{
		_goProblems = AssetsManager.extractAssetFiles (
			getAssets (), null, saveLoadGamesDir,
			resources.getString (R.string.goProblemsAssetFolderName));
	}
	final List <File> goProblems = _goProblems;

	final FileFilter sgfFileFilter = _sgfFileFilter;
	final String sgfSuffix = getSgfFileExtension ();
	final File [] files =
		saveLoadGamesDir.listFiles (sgfFileFilter != null ? sgfFileFilter
		: (_sgfFileFilter = new FileFilter ()
			{
				public
				boolean accept (
					final File pFile
					)
				{
					return !pFile.isDirectory ()
						&& pFile.getName ().endsWith (sgfSuffix);
				}
			}));
	if (files.length == 0)
	{
		showMessage (resources.getString (R.string.noFiles2load,
			saveLoadGamesDir.getAbsolutePath ()));
		return;
	}
	Arrays.sort (files,
		new Comparator <File> ()
		{
			public
			int compare (
				final File pFile1,
				final File pFile2
				)
			{
				return pFile1.lastModified () < pFile2.lastModified () ? 1 : -1;
			}
		});

	final String filePath = gameInfo._sgfFileName;
	final File [] checkedFile = new File [1];
	int selectedPos = -1, fileIdx = 0;
	for (final File file : files)
	{
		if (file.getAbsolutePath ().equals (filePath))
		{
			selectedPos = fileIdx;
			checkedFile [0] = file;
			break;
		}
		fileIdx++;
	}

	final ViewStub viewStub = new ViewStub (this, R.layout.load_game);
	final Dialog dialog = new Dialog (this);
	dialog.getWindow ().requestFeature (Window.FEATURE_NO_TITLE);
	dialog.setContentView (viewStub);
	final View view = viewStub.inflate ();

	final Map <View, File> fileNameViewFileMap =
			Generics.newHashMap (),
		deleteButtonViewFileMap = Generics.newHashMap ();
	final Set <View> fileNameViews = Generics.newHashSet ();
	final View loadButton = view.findViewById (R.id.loadGameDialogButton);
	loadButton.setOnClickListener (
		new View.OnClickListener ()
		{
			public
			void onClick (
				final View pView
				)
			{
				dialog.dismiss ();
				newGame (restoreGameInfo (new GameInfo ()), null);
				final GameInfo gameInfo = _gameInfo;
				gameInfo._sgfFileName = checkedFile [0].getAbsolutePath ();
				_gtp.loadGame (
					gameInfo, GameInfo._DEFAULT_GAME_COLLECTION_IDX);
			}
		});

	final View.OnClickListener selectOnClickListener =
		new View.OnClickListener ()
		{
			public
			void onClick (
				final View pView
				)
			{
				loadButton.setEnabled (true);
				for (final View view : fileNameViews)
				{
					final boolean checked = view == pView;
					final CompoundButton fileNameView = (CompoundButton)view.
						findViewById (R.id.fileNameTextView);
					fileNameView.setChecked (checked);
					if (checked)
					{
						checkedFile [0] =
							fileNameViewFileMap.get (fileNameView);
					}
				}
			}
		};
	@SuppressWarnings ("unchecked")
	final ArrayAdapter <File> [] arrayAdapter = new ArrayAdapter [1];
	final View.OnClickListener deleteOnClickListener =
		new View.OnClickListener ()
		{
			public
			void onClick (
				final View pView
				)
			{
				final File deleteFile = deleteButtonViewFileMap.get (pView);
				final DialogInterface.OnClickListener clickListener =
					new DialogInterface.OnClickListener ()
					{
						public
						void onClick (
							final DialogInterface pDialogInterface,
							final int pWhichButton
							)
						{
							if (!deleteFile.delete ())
							{
								return;
							}
							if (saveLoadGamesDir.listFiles (_sgfFileFilter).
								length == 0)
							{
								dialog.dismiss ();
							}
							arrayAdapter [0].remove (deleteFile);
							final View parentView = (View)pView.getParent ();
							loadButton.setEnabled (loadButton.isEnabled ()
								&& !((CompoundButton)parentView.
									findViewById (R.id.fileNameTextView)).
										isChecked ());
						}
					};
				new AlertDialog.Builder (MainActivity.this).
					setTitle (R.string.deleteButtonLabel).
					setIcon (android.R.drawable.ic_dialog_alert).
					setMessage (resources.getString (
						R.string.deleteFileMessage,
						deleteFile.getAbsolutePath ())).
					setNegativeButton (android.R.string.cancel, null).
					setPositiveButton (R.string.deleteButtonLabel,
						clickListener).
					show ();
			}
		};
	final ListView filesView =
		(ListView)view.findViewById (R.id.fileListView);
	final LayoutInflater layoutInflater = getLayoutInflater ();
	final CharSequence fileNameInfoTemplate =
		resources.getText (R.string.fileNameFileInfoTemplate);
	final String [] fileNameInfoSources = new String []
		{
			resources.getString (R.string.fileNameTemplate),
			resources.getString (R.string.fileInfoTemplate),
		};
	final CharSequence [] fileNameInfoDestinations = new CharSequence [2];
	filesView.setAdapter (
		arrayAdapter [0] = new ArrayAdapter <File> (this, 0,
			Generics.newArrayList (Arrays.asList (files)))
	{
		public
		View getView (
			final int pPosition,
			View pCachedView,
			final ViewGroup pParent
			)
		{
			final CompoundButton fileNameView;
			final View deleteButtonView;
			if (pCachedView == null)
			{
				pCachedView = layoutInflater.
					inflate (R.layout.load_game_list_entry, null);
				fileNameViews.add (pCachedView);
				pCachedView.setOnClickListener (selectOnClickListener);
				fileNameView = (CompoundButton)pCachedView.
					findViewById (R.id.fileNameTextView);
				fileNameView.setClickable (false);
				deleteButtonView = pCachedView.
					findViewById (R.id.fileDeleteButton);
				deleteButtonView.setOnClickListener (deleteOnClickListener);
			}
			else
			{
				fileNameView = (CompoundButton)pCachedView.
					findViewById (R.id.fileNameTextView);
				deleteButtonView = pCachedView.
					findViewById (R.id.fileDeleteButton);
			}
			final File file = getItem (pPosition);
			fileNameView.setChecked (checkedFile [0] == file);
			fileNameViewFileMap.put (fileNameView, file);
			final boolean isGoProblem = goProblems.contains (file);
			deleteButtonView.setVisibility (
				isGoProblem ? View.GONE : View.VISIBLE);
			if (isGoProblem)
			{
				fileNameInfoDestinations[1] =
					resources.getString (R.string.goProblemFileInfo);
			}
			else
			{
				deleteButtonViewFileMap.put (deleteButtonView, file);
				final Date date = new Date (file.lastModified ());
				fileNameInfoDestinations[1] =
					DateFormat.getDateFormat (
						MainActivity.this).format (date) + " "
					+ DateFormat.getTimeFormat (
						MainActivity.this).format (date);
			}
			final String name = file.getName ();
			fileNameInfoDestinations[0] =
				name.substring (0, name.lastIndexOf (sgfSuffix));
			fileNameView.setText (TextUtils.replace (fileNameInfoTemplate,
				fileNameInfoSources, fileNameInfoDestinations));
			return pCachedView;
		}
	});
	filesView.setSelection (selectedPos);
	loadButton.setEnabled (selectedPos != -1);
	dialog.show ();
}

void showCollectionList ()
{
	final Resources resources = _resources;
	final GameInfo gameInfo = _gameInfo;
	final String[] names = gameInfo._collectionNames;
	if (names == null || names.length == 0)
	{
		showMessage (resources.getString (R.string.noFiles2load,
			gameInfo._sgfFileName));
		return;
	}
	final List <String> namesList = Arrays.asList (names);
	final int selectedPos = gameInfo._collectionIdx;
	final String [] checkedIdx = new String [1];
	if (selectedPos >= 0 && selectedPos < names.length)
	{
		checkedIdx[0] = names[selectedPos];
	}

	final ViewStub viewStub = new ViewStub (this, R.layout.load_game);
	final Dialog dialog = new Dialog (this);
	dialog.getWindow ().requestFeature (Window.FEATURE_NO_TITLE);
	dialog.setContentView (viewStub);
	dialog.setOnCancelListener (new DialogInterface.OnCancelListener ()
	{
		public
		void onCancel (
			final DialogInterface pDialogInterface
			)
		{
			dialog.dismiss ();
			gameInfo._isCollection = false;
			loadGame ();
		}
	});
	final View view = viewStub.inflate ();

	final Map <View, String> gameNameViewNameMap = Generics.newHashMap ();
	final Set <View> gameNameViews = Generics.newHashSet ();
	final View loadButton = view.findViewById (R.id.loadGameDialogButton);
	loadButton.setOnClickListener (
		new View.OnClickListener ()
		{
			public
			void onClick (
				final View pView
				)
			{
				dialog.dismiss ();
				final GameInfo gameInfo = _gameInfo;
				newGame (restoreGameInfo (new GameInfo ()).
					copyCollection (gameInfo), null);
				_gtp.loadGame (
					_gameInfo, namesList.indexOf (checkedIdx [0]));
			}
		});

	final View.OnClickListener selectOnClickListener =
		new View.OnClickListener ()
		{
			public
			void onClick (
				final View pView
				)
			{
				loadButton.setEnabled (true);
				for (final View view : gameNameViews)
				{
					final boolean checked = view == pView;
					final CompoundButton gameNameView = (CompoundButton)view.
						findViewById (R.id.fileNameTextView);
					gameNameView.setChecked (checked);
					if (checked)
					{
						checkedIdx [0] = gameNameViewNameMap.get (gameNameView);
					}
				}
			}
		};

	final ListView gameNameView =
		(ListView)view.findViewById (R.id.fileListView);
	final LayoutInflater layoutInflater = getLayoutInflater ();
	gameNameView.setAdapter (new ArrayAdapter <String> (this, 0, names)
	{
		public
		View getView (
			final int pPosition,
			View pCachedView,
			final ViewGroup pParent
			)
		{
			final CompoundButton gameNameView;
			if (pCachedView == null)
			{
				pCachedView = layoutInflater.
					inflate (R.layout.load_game_list_entry, null);
				gameNameViews.add (pCachedView);
				pCachedView.setOnClickListener (selectOnClickListener);
				gameNameView = (CompoundButton)pCachedView.
					findViewById (R.id.fileNameTextView);
				gameNameView.setClickable (false);
				final View deleteButtonView = pCachedView.
					findViewById (R.id.fileDeleteButton);
				deleteButtonView.setVisibility (View.GONE);
			}
			else
			{
				gameNameView = (CompoundButton)pCachedView.
					findViewById (R.id.fileNameTextView);
			}
			final String name = getItem (pPosition);
			gameNameView.setChecked (name.equals (checkedIdx [0]));
			gameNameViewNameMap.put (gameNameView, name);
			gameNameView.setText (name);
			return pCachedView;
		}
	});
	gameNameView.setSelection (selectedPos);
	loadButton.setEnabled (
		selectedPos != GameInfo._DEFAULT_GAME_COLLECTION_IDX);
	dialog.show ();
}

static private
boolean storageCardMounted ()
{
	return Environment.MEDIA_MOUNTED.equals (
		Environment.getExternalStorageState ());
}

/*
private
boolean _migrateFiles = true; // todo remove when no old app versions in use any more
*/

private
File getSaveLoadGamesDir ()
{
	File saveLoadGamesDir = null;
	try
	{
//		final int buildVersion = Build.VERSION.SDK_INT;
		final File externalStorageDir =
			getExternalStorageDirectory (/* buildVersion */);
		boolean dirExists;
		if (externalStorageDir == null
			|| !storageCardMounted ()
			|| ((dirExists = (saveLoadGamesDir = new File (
					externalStorageDir, _saveLoadGamesDirName)).exists ())
				&& !saveLoadGamesDir.isDirectory ())
			|| (!dirExists && !saveLoadGamesDir.mkdir ()))
		{
			throw new Exception ("not mounted?");
		}
		// migrate files from earlier app versions
		/*
		if (_migrateFiles && buildVersion >= 8) // todo remove when no old app versions in use any more
		{
			final File preV8externalStorageDir =
				getExternalStorageDirectory (7);
			if (preV8externalStorageDir != null
				&& !preV8externalStorageDir.equals (externalStorageDir)
				&& preV8externalStorageDir.exists ()
				&& preV8externalStorageDir.isDirectory ())
			{
				final File preV8saveLoadGamesDir = new File (
					preV8externalStorageDir, _saveLoadGamesDirName);
				if (preV8saveLoadGamesDir.exists ()
					&& preV8saveLoadGamesDir.isDirectory ())
				{
					final File[] files = preV8saveLoadGamesDir.listFiles ();
					if (files != null)
					{
						for (final File file : files)
						{
							file.renameTo (
								new File (saveLoadGamesDir, file.getName ()));
						}
					}
					preV8saveLoadGamesDir.delete ();
				}
			}
		}
		_migrateFiles = false;
		*/
	}
	catch (final Exception e)
	{
		if (isEnabledFor (_LOG_TAG, Log.INFO))
		{
			log (_LOG_TAG, Log.INFO, "failed accessing SD card: '" + e);
		}
		final Resources resources = _resources;
		new AlertDialog.Builder (this).
			setTitle (R.string.menuSaveLoadLabel).
			setIcon (android.R.drawable.ic_dialog_alert).
			setNeutralButton (android.R.string.ok, null).
			setMessage (resources.getString (R.string.accessFailedAlertMessage,
				saveLoadGamesDir != null ? saveLoadGamesDir.getAbsolutePath () :
					resources.getString (R.string.sdCardName))).show ();
		return null;
	}
	return saveLoadGamesDir;
}

static
File getExternalStorageDirectory (
//	final int pBuildVersion
	)
	throws Exception
{
	return //pBuildVersion < 8 ?
		Environment.getExternalStorageDirectory  (); /*:
		(File)Environment.class.getDeclaredMethod (
			"getExternalStoragePublicDirectory",
			String.class).invoke (null,
				Environment.class.getDeclaredField ("DIRECTORY_DOWNLOADS").
					get (null));
	*/
}

public
boolean onKeyDown (
	final int pKeyCode,
	final KeyEvent pKeyEvent
	)
{
	int diffX = 0, diffY = 0;
	switch (pKeyCode)
	{
	case KeyEvent.KEYCODE_DPAD_CENTER:
	case KeyEvent.KEYCODE_SPACE:
		break;
	case KeyEvent.KEYCODE_DPAD_UP:
	case KeyEvent.KEYCODE_U:
		diffY = -1;
		break;
	case KeyEvent.KEYCODE_DPAD_DOWN:
	case KeyEvent.KEYCODE_N:
		diffY = 1;
		break;
	case KeyEvent.KEYCODE_DPAD_LEFT:
	case KeyEvent.KEYCODE_G:
		diffX = -1;
		break;
	case KeyEvent.KEYCODE_DPAD_RIGHT:
	case KeyEvent.KEYCODE_J:
		diffX = 1;
		break;
	/*
	case KeyEvent.KEYCODE_BACK:
		if (_undoEnabled)
		{
			undo ();
		}
		else
		{
			showUndoRedoHint (_noUndoHintText);
		}
		break;
	*/
	default:
		return super.onKeyDown (pKeyCode, pKeyEvent);
	}
	_boardView.moveStone (diffX, diffY);
	return true;
}

public
boolean onKeyUp (
	final int pKeyCode,
	final KeyEvent pKeyEvent
	)
{
	switch (pKeyCode)
	{
	case KeyEvent.KEYCODE_DPAD_UP:
	case KeyEvent.KEYCODE_U:
	case KeyEvent.KEYCODE_DPAD_DOWN:
	case KeyEvent.KEYCODE_N:
	case KeyEvent.KEYCODE_DPAD_LEFT:
	case KeyEvent.KEYCODE_G:
	case KeyEvent.KEYCODE_DPAD_RIGHT:
	case KeyEvent.KEYCODE_J:
		_boardView.showKeyUpHint ();
		break;
	default:
		return super.onKeyUp (pKeyCode, pKeyEvent);
	}
	return true;
}

void showUndoRedoHint (
	final CharSequence pText
	)
{
	final Toast undoRedoHint = _undoRedoHint;
	undoRedoHint.setText (pText);
	undoRedoHint.show ();
}

void
showWaitProgress (
	final String pMessage
	)
{
	if (_progressDialog == null)
	{
		_progressDialog = ProgressDialog.show (this, null, pMessage, true);
		_gtp.hideWaitProgress ();
	}
}

void hideWaitProgress ()
{
	final ProgressDialog progressDialog = _progressDialog;
	if (progressDialog != null)
	{
		progressDialog.dismiss ();
		_progressDialog = null;
	}
}

GameInfo storeGameInfo (
	final GameInfo pGameInfo
	)
{
	final SharedPreferences.Editor editor = _sharedPreferences.edit ();
	editor.putInt (_preferencesBoardSizeKey, pGameInfo._boardSize);
	editor.putInt (_preferencesHandicapKey, pGameInfo._handicap);
	editor.putString (_preferencesKomiKey, pGameInfo._komi);
	editor.putBoolean (_preferencesChineseRulesKey, pGameInfo._chineseRules);
	editor.putInt (_preferencesLevelKey, pGameInfo._level);
	editor.putBoolean (
		_preferencesPlayerBlackHumanKey, pGameInfo._playerBlackHuman);
	editor.putBoolean (
		_preferencesPlayerWhiteHumanKey, pGameInfo._playerWhiteHuman);
	editor.commit ();
	return pGameInfo;
}

private
GameInfo restoreGameInfo (
	final GameInfo pGameInfo
	)
{
	final SharedPreferences preferences = _sharedPreferences;
	pGameInfo._boardSize = preferences.getInt (_preferencesBoardSizeKey,
		pGameInfo._boardSize);
	pGameInfo._handicap = preferences.getInt (_preferencesHandicapKey,
		pGameInfo._handicap);
	try
	{
		pGameInfo._komi = preferences.getString (_preferencesKomiKey,
			pGameInfo._komi);
		pGameInfo._chineseRules = preferences.getBoolean (
			_preferencesChineseRulesKey, pGameInfo._chineseRules);
	}
	catch (final Exception e) {}
	pGameInfo._level = preferences.getInt (_preferencesLevelKey,
		pGameInfo._level);
	pGameInfo._playerBlackHuman = preferences.getBoolean (
		_preferencesPlayerBlackHumanKey, pGameInfo._playerBlackHuman);
	pGameInfo._playerWhiteHuman = preferences.getBoolean (
		_preferencesPlayerWhiteHumanKey, pGameInfo._playerWhiteHuman);
	return pGameInfo;
}

void newGame ()
{
    // modified by xiaosea
	// newGame (restoreGameInfo (new GameInfo ()), true);
    String sgfFileName = getIntent().getStringExtra(Intent.EXTRA_TEXT);
    if (sgfFileName != null) {
        newGame(restoreGameInfo(new GameInfo()), null);
        final GameInfo gameInfo = _gameInfo;
        gameInfo._sgfFileName = sgfFileName;
        _gtp.loadGame(gameInfo, GameInfo._DEFAULT_GAME_COLLECTION_IDX);
    } else {
        newGame (restoreGameInfo (new GameInfo ()), true);
    }
}

private
void newGame (
	final GameInfo pGameInfo,
	final Boolean pNewGame
	)
{
	GameInfo gameInfo = _gameInfo;
	final Gtp gtp = _gtp;
	if (gameInfo != null && (pNewGame == null || pNewGame))
	{
		gameInfo._invalid = true;
		gtp.deleteAutoSaveFile ();
	}
	showWait4Move2FinishMessage (gameInfo);
	_gameInfo = pGameInfo;
	if (pNewGame == null)
	{
		return;
	}
	if (pNewGame)
	{
		gtp.newGame (pGameInfo);
	}
	else
	{
		gtp.changeGame (gameInfo, pGameInfo);
	}
}

private
void showWait4Move2FinishMessage (
	final GameInfo pGameInfo
	)
{
	if (Gtp.playerIsMachine (pGameInfo))
	{
		showWaitProgress (_resources.getString (
			R.string.waitProgressLastMoveMessage));
	}
}

void nextMove (
	final Point pPoint
	)
{
	final GameInfo gameInfo = _gameInfo;
	gameInfo.addMove (pPoint);
	_gtp.nextMove (_gameInfo);
}

void drawBoard (
	final boolean pInit
	)
{
	_gtp.drawBoard (_gameInfo, pInit);
}

GameInfo getGameInfo ()
{
	return _gameInfo;
}

AlertDialog showMessage (
	final String pMessage
	)
{
	return new AlertDialog.Builder (this).
		setMessage (pMessage).
		setNeutralButton (android.R.string.ok, null).
		show ();
}

void showPassMessage (
	final boolean pColorBlack,
	final boolean pResigned
	)
{
	final Resources resources = _resources;
	showMessage (
		resources.getString (R.string.passedDialogMessageText,
			resources.getString (pColorBlack ?
				R.string.blackColorText : R.string.whiteColorText),
			getPassedText (pResigned)));
}

String getPassedText (
	final boolean pResigned
	)
{
	return _resources.getString (pResigned ? R.string.resignedText
		: R.string.passedText);
}

void showMove (
	final boolean pBlack,
	final String pMoveText
	)
{
	final boolean showProgressBar = pMoveText == null;
	(pBlack ? _blackMoveProgressBar : _whiteMoveProgressBar).
		setVisibility (showProgressBar ? View.VISIBLE : View.GONE);
	final TextView textView = pBlack ? _blackMoveTextView : _whiteMoveTextView;
	textView.setVisibility (showProgressBar ? View.GONE : View.VISIBLE);
	final GameInfo gameInfo = _gameInfo;
	_boardView.showScoreBackground (!showProgressBar
		&& !(Gtp.otherPlayerIsMachine (gameInfo)
			&& Gtp.playerIsMachine (gameInfo)));
	if (!showProgressBar)
	{
		textView.setText (pMoveText.length () == 0
			&& ((pBlack && gameInfo._playerBlackHuman
					&& gameInfo._playerBlackMoves)
				|| (!pBlack && gameInfo._playerWhiteHuman
					&& !gameInfo._playerBlackMoves)) ?
						_yourTurnText : pMoveText);
	}
}

private
void showCaptures (
	final int pBlack,
	final int pWhite
	)
{
	_blackCapturesTextView.setText (String.valueOf (pBlack));
	_whiteCapturesTextView.setText (String.valueOf (pWhite));
}

private
void enablePassMenu (
	final boolean pEnable
	)
{
	enableMenuItem (_passMenuItem, pEnable);
}

private
void enableUndoMenu (
	final GameInfo pGameInfo
	)
{
	boolean undoEnabled = false, redoEnabled = false;
	if (pGameInfo != null)
	{
		boolean playerIsMaschine = Gtp.playerIsMachine (pGameInfo),
			otherPlayerIsMachine = Gtp.otherPlayerIsMachine (pGameInfo);
		final int moveNumber = pGameInfo._moveNumber;
		_undoEnabled = undoEnabled =
			!(playerIsMaschine && otherPlayerIsMachine)
			&& (!playerIsMaschine || pGameInfo.bothPlayersPassed ())
			&& !(otherPlayerIsMachine && moveNumber == 1)
			&& moveNumber > 0;
		_redoEnabled = redoEnabled = !playerIsMaschine
			&& pGameInfo._redoPoints == null
			&& pGameInfo.canRedo (otherPlayerIsMachine ? 2 : 1);
	}
	enableMenuItem (_undoMenuItem, undoEnabled);
	enableMenuItem (_restartMenuItem, undoEnabled);
	enableMenuItem (_redoMenuItem, redoEnabled);
}

private
void showScore (
	final int pBlackTerritory,
	final int pWhiteTerritory,
	final Object pStatus
	)
{
	_boardView.showScoreBackground (true);
	if (pStatus == null)
	{
		_capturesRowTextView.setText (R.string.capturesLabelText);
		_moveRowTextView.setText (R.string.moveLabelText);
		_scoreTableRow.setVisibility (View.GONE);
		_messageTableRow.setVisibility (View.GONE);
		setScoreViewTextSize (_3rowsTextSize);
		_blackScoreTextView.setText (null);
		_whiteScoreTextView.setText (null);
		return;
	}
	if (pBlackTerritory == _SHOW_MESSAGE)
	{
		_scoreTableRow.setVisibility (View.GONE);
		_messageTableRow.setVisibility (View.VISIBLE);
		setScoreViewTextSize (_4rowsTextSize);
		_messageScoreTextView.setText ((String)pStatus);
		return;
	}
	_scoreTableRow.setVisibility (View.VISIBLE);
	_messageTableRow.setVisibility (View.GONE);
	setScoreViewTextSize (_4rowsTextSize);
	if (pBlackTerritory == _SHOW_ESTIMATED_SCORE
		|| pWhiteTerritory == _SHOW_ESTIMATED_SCORE)
	{
		if (pBlackTerritory == _SHOW_ESTIMATED_SCORE)
		{
			_blackScoreTextView.setText ((String)pStatus);
		}
		else
		{
			_whiteScoreTextView.setText ((String)pStatus);
		}
		return;
	}
	final GameInfo gameInfo = (GameInfo)pStatus;
	if (gameInfo._chineseRules)
	{
		_capturesRowTextView.setText (R.string.stonesLabelText);
	}
	_moveRowTextView.setText (R.string.territoryLabelText);
	_blackMoveTextView.setText (String.valueOf (pBlackTerritory));
	_whiteMoveTextView.setText (String.valueOf (pWhiteTerritory));
	_blackScoreTextView.setText (String.valueOf (
		Integer.parseInt (_blackCapturesTextView.getText ().toString ()) +
		pBlackTerritory + 0f));
	_whiteScoreTextView.setText (String.valueOf (
		Integer.parseInt (_whiteCapturesTextView.getText ().toString ()) +
		pWhiteTerritory + Float.parseFloat (gameInfo._komi)));
}

private
void showInfo ()
{
	try
	{
		final AppInfo appInfo = _appInfo;
		final Resources resources = _resources;
		final WebView webView = new WebView (this);
		webView.loadDataWithBaseURL (null,
			resources.getString (R.string.infoDialogText,
				appInfo._appName,
				appInfo._appVersion,
				resources.getString (R.string.BYauthorText),
				resources.getString (R.string.authorMailUrl),
				resources.getString (R.string.authorName),
				resources.getString (R.string.goRulesUrl),
				resources.getString (R.string.goRulesText),
				resources.getString (R.string.godroidProjectUrl),
				resources.getString (R.string.projectHomepageText),
				resources.getString (R.string.gnugoProjectUrl),
				resources.getString (R.string.gnuGoHomepageText)),
			"text/html", "utf-8", null);
		webView.setNetworkAvailable (false);
		webView.setBackgroundColor (Color.TRANSPARENT);
		new AlertDialog.Builder (this).
			setIcon (R.drawable.infoMenuIcon).
			setTitle (R.string.menuInfoLabel).
			setView (webView).
			show ();
	}
	catch (final Exception e) {}
}

static
String getLongestString (
	final int pColumn0StringsId,
	final int pColumsRefsId,
	final Resources pResources,
	final Paint pPaint
	)
{
	final String COLUMN_SEPARATOR = "  ";
	String longestString = "";
	float maxWidth = 0;
	final String[] column0Strings =
		pResources.getStringArray (pColumn0StringsId);
	final TypedArray columnsRefs = pResources.obtainTypedArray (pColumsRefsId);
	final int numCols0 = columnsRefs.length ();
	for (int col0Idx = 0; col0Idx < numCols0; col0Idx++)
	{
		final String column0String = column0Strings[col0Idx];
		final String[] columns =
			pResources.getStringArray (columnsRefs.getResourceId (col0Idx, 0));
		final int numCols = columns.length;
		for (int colIdx = 0; colIdx < numCols; colIdx += 2)
		{
			final String column1 = columns[colIdx];
			final boolean emptyCol = column1.length () == 0;
			final String measureString =
				column0String + (emptyCol ? ""
					: (COLUMN_SEPARATOR + column1 + COLUMN_SEPARATOR))
					+ columns[colIdx+1];
			final float width = pPaint.measureText (measureString);
			if (isEnabledFor (_LOG_TAG, Log.DEBUG))
			{
				log (_LOG_TAG, Log.DEBUG, "measure string: "
					+ width + " '" + measureString + "'");
			}
			if (width > maxWidth)
			{
				maxWidth = width;
				longestString = measureString;
			}
		}
	}
	columnsRefs.recycle ();
	if (isEnabledFor (_LOG_TAG, Log.DEBUG))
	{
		log (_LOG_TAG, Log.DEBUG, "longest string: " + maxWidth
			+ " '" + longestString + "'");
	}
	return longestString;
}

void sizeScoreViewTexts (
	final int pWidth,
	final int pHeight
	)
{
	final Resources resources = _resources;
	final float padding =
		resources.getDimension (R.dimen.scoreViewTextPadding) * 2f,
		maxLines = resources.getDimension (R.dimen.scoreViewMaxLines),
		maxLinesMinus1 = maxLines -1f,
		netHeight = (float)pHeight - padding;
	final TextView measureView = _capturesRowTextView;
	final Paint paint = measureView.getPaint ();
	_3rowsTextSize = calcTextSize (measureView, netHeight / maxLinesMinus1,
		getLongestString (
			R.array.scoreView3LinesColumn0Strings,
			R.array.scoreView3LinesColumns, resources, paint),
		maxLinesMinus1, padding, pWidth, pHeight);
	_4rowsTextSize = calcTextSize (measureView, netHeight / maxLines,
		getLongestString (
			R.array.scoreView4LinesColumn0Strings,
			R.array.scoreView4LinesColumns, resources, paint),
		maxLines, padding, pWidth, pHeight);
	setScoreViewTextSize (_messageTableRow.getVisibility() == View.VISIBLE || _scoreTableRow.getVisibility() == View.VISIBLE ? _4rowsTextSize : _3rowsTextSize);// Modified by xiaosea
	findViewById (R.id.MenuButton);
}

static
float calcTextSize (
	final TextView pMeasureView,
	float pTextSize,
	final String pMeasureString,
	final float pNumLines,
	final float pPadding,
	final float pMaxWidth,
	final float pMaxHeight
	)
{
	final float numLinesMinus1 = pNumLines -1f;
	while (true)
	{
		pMeasureView.setTextSize (TypedValue.COMPLEX_UNIT_PX, pTextSize);
		final TextPaint paint = pMeasureView.getPaint ();
		final Paint.FontMetrics metrics = paint.getFontMetrics ();
		final float width = paint.measureText (pMeasureString) + pPadding,
			height = (float)(new StaticLayout (pMeasureString, paint,
				(int)width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true).
					getHeight ()) * pNumLines
				+ metrics.leading * numLinesMinus1 + pPadding;
		if (isEnabledFor (_LOG_TAG, Log.DEBUG))
		{
			log (_LOG_TAG, Log.DEBUG, "scale down: w " + width
				+ " max w " + pMaxWidth
				+ " | h " + height + " max h " + pMaxHeight
				+ "\ns " + pTextSize);
		}
		if (width < pMaxWidth && height < pMaxHeight)
		{
			break;
		}
		pTextSize -= width - pMaxWidth > 100 || height - pMaxHeight > 100 ?
			4f : 2f;
	}
	return pTextSize;
}

void setScoreViewTextSize (
	final float pTextSize
	)
{
	for (final TextView view : _scoreViewTextViews)
	{
		view.setTextSize (TypedValue.COMPLEX_UNIT_PX, pTextSize);
	}
	final int size = (int)pTextSize;
	final LinearLayout.LayoutParams layoutParams =
		new LinearLayout.LayoutParams (size, size);
	_blackMoveProgressBar.setLayoutParams (layoutParams);
	_whiteMoveProgressBar.setLayoutParams (layoutParams);
}

static
Toast makeToast (
	final Context pContext,
	final String pText
	)
{
	final Toast toast = Toast.makeText (pContext, pText, Toast.LENGTH_SHORT);
	toast.setGravity (Gravity.TOP, 0, 0);
	final View hintView = toast.getView ();
	if (hintView instanceof LinearLayout)
	{
		final View textView = ((LinearLayout)hintView).getChildAt (0);
		if (textView != null && textView instanceof TextView)
		{
			((TextView)textView).setGravity (Gravity.CENTER_HORIZONTAL);
		}
	}
	return toast;
}
}