package com.bkc.mmlx;


import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.EditText;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MMLEdit extends EditText
{
	public interface OnTextChangedListener
	{
		public void onTextChanged( String text );
	}

	public OnTextChangedListener onTextChangedListener = null;
	public int updateDelay = 1000;
	public int errorLine = 0;
	public boolean dirty = false;

	public static final int PATTERN_COMMANDS = 1;
	public static final int PATTERN_COMMENTS = 2;
	public static final int PATTERN_SPECIALS = 3;
	
	
	private static final int COLOR_ERROR = 0x80ff0000;
//	private static final int COLOR_NUMBER = 0xff7ba212;
	private static final int COLOR_COMMAND = 0xff399ed7;
	private static final int COLOR_SPECIAL = 0xffd79e39;
	private static final int COLOR_COMMENT = 0xffff0000;

	private static final Pattern line = Pattern.compile(
		".*\\n" );
	
	//private static final Pattern numbers = Pattern.compile(
	//	"(\\d*[.]?\\d+)" );

	//private static final Pattern numbers = Pattern.compile(
	//		"\\d+" );
	
	private static Pattern commands = null;
	private static Pattern specials = null;
	private static Pattern comments = null;

	 private static String patCommands = 
		"([abcdefg]|v\\+|v\\-|VOP|I@|IP|IV|FO|" +
		"[oltCqQvVPNsmpGDKRxyzLjI]|" +
		"HL|HM|HP|HA|HS|HR|SP|SA|SRP|SRA|SR" +
		")";

	private static String patSpecials = "";
		//	"(\\?|\\!|\\$|\\||\\+|\\-|\\=|\\^|\\&|\\>|\\<|\\*|\\~|\\*\\*)";
		
	 
	//private static String patSpecials = 
	//	"(\\?|\\!|\\$|\\||\\+|\\-|\\=|\\^|\\&|\\>|\\<|\\*|\\~|\\*\\*)";
	
	private static String patComments = "/\\*(?:.|[\\n\\r])*?\\*/|;.*|//.*";
	

	private final Handler updateHandler = new Handler();
	private final Runnable updateRunnable =
		new Runnable()
		{
			@Override
			public void run()
			{
				Editable e = getText();

				if( onTextChangedListener != null )
					onTextChangedListener.onTextChanged( e.toString() );

				highlightWithoutChange( e );
			}
		};
	private boolean modified = true;

	public MMLEdit( Context context )
	{
		super( context );
		init();
		compilePatterns();
	}

	public MMLEdit( Context context, AttributeSet attrs )
	{
		super( context, attrs );
		init();
		compilePatterns();
	}
	
	public void compilePatterns()
	{
		// specials = Pattern.compile(patSpecials);
		// commands = Pattern.compile(patCommands);
		comments = Pattern.compile(patComments);
	}
	
	public String getPatternString(int pattern)
	{
		switch(pattern)
		{
			case PATTERN_COMMANDS:
				return patCommands;
				
			case PATTERN_SPECIALS:
				return patSpecials;
			
			case PATTERN_COMMENTS:	
				return patComments;
		}
		return "";
	}

	public void setPatternString(int pattern, String pats)
	{
		switch(pattern)
		{
			case PATTERN_COMMANDS:
				patCommands = pats;
			break;
				
			case PATTERN_SPECIALS:
				patSpecials = pats;
			break;			
			case PATTERN_COMMENTS:	
				patComments = pats;
		}
	}


	public void setTextHighlighted( CharSequence text )
	{
		cancelUpdate();

		errorLine = 0;
		dirty = false;

		modified = false;
		setText( highlight( new SpannableStringBuilder( text ) ) );
		modified = true;

		if( onTextChangedListener != null )
			onTextChangedListener.onTextChanged( text.toString() );
	}
	

	public void refresh()
	{
		highlightWithoutChange( getText() );
	}

	private void init()
	{
		setHorizontallyScrolling( true );


		addTextChangedListener(
			new TextWatcher()
			{
				@Override
				public void onTextChanged(
					CharSequence s,
					int start,
					int before,
					int count )
				{
				}

				@Override
				public void beforeTextChanged(
					CharSequence s,
					int start,
					int count,
					int after )
				{
				}

				@Override
				public void afterTextChanged( Editable e )
				{
					cancelUpdate();

					if( !modified )
						return;

					dirty = true;
					updateHandler.postDelayed(
						updateRunnable,
						updateDelay );
				}
			} );
	}

	private void cancelUpdate()
	{
		updateHandler.removeCallbacks( updateRunnable );
	}

	private void highlightWithoutChange( Editable e )
	{
		modified = false;
		highlight( e );
		modified = true;
	}
	
	private void setSyntaxColor( Editable e, Pattern ptn, int color )
	{
		for (Matcher m = ptn.matcher(e); m.find();)
		{
			e.setSpan(new ForegroundColorSpan(color),
					m.start(), m.end(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			
		}
	}

	private Editable highlight( Editable e )
	{
		try
		{
			// don't use e.clearSpans() because it will remove
			// too much
			clearSpans( e );

			if( e.length() == 0 )
				return e;

			if( errorLine > 0 )
			{
				Matcher m = line.matcher( e );

				for( int n = errorLine;
					n-- > 0 && m.find(); );

				e.setSpan(
					new BackgroundColorSpan( COLOR_ERROR ),
					m.start(),
					m.end(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );
			}

			/* for( Matcher m = numbers.matcher( e );
				m.find(); )
				e.setSpan(
					new ForegroundColorSpan( COLOR_NUMBER ),
					m.start(),
					m.end(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );
			*/

			if (commands != null)
			{
				setSyntaxColor(e, commands, COLOR_COMMAND);
			}
			
			if (specials != null)
			{
				setSyntaxColor(e, specials, COLOR_SPECIAL);
			}
			
			if (comments != null)
			{
				setSyntaxColor(e, comments, COLOR_COMMENT);
			}
		}
		catch( Exception ex )
		{
		}

		return e;
	}

	private void clearSpans( Editable e )
	{
		// remove foreground color spans
		{
			ForegroundColorSpan spans[] = e.getSpans(
				0,
				e.length(),
				ForegroundColorSpan.class );

			for( int n = spans.length; n-- > 0; )
				e.removeSpan( spans[n] );
		}

		// remove background color spans
		{
			BackgroundColorSpan spans[] = e.getSpans(
				0,
				e.length(),
				BackgroundColorSpan.class );

			for( int n = spans.length; n-- > 0; )
				e.removeSpan( spans[n] );
		}
	}

	
}