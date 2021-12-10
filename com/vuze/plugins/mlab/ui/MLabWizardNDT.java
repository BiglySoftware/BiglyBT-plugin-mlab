/*
 * Created on May 24, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */



package com.vuze.plugins.mlab.ui;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import com.biglybt.core.internat.MessageText;
import com.biglybt.core.util.AEThread2;
import com.biglybt.core.util.Constants;
import com.biglybt.core.util.SimpleTimer;
import com.biglybt.core.util.TimerEvent;
import com.biglybt.core.util.TimerEventPerformer;
import com.biglybt.core.util.TimerEventPeriodic;
import com.biglybt.ui.swt.TextViewerWindow;
import com.biglybt.ui.swt.Utils;
import com.biglybt.ui.swt.mainwindow.Colors;
import com.biglybt.ui.swt.wizard.AbstractWizardPanel;
import com.biglybt.ui.swt.wizard.IWizardPanel;

import com.vuze.plugins.mlab.MLabPlugin;
import com.vuze.plugins.mlab.MLabPlugin.*;

public class 
MLabWizardNDT 
	extends AbstractWizardPanel<MLabWizard> 
{
	private StyledText			log;
	
	private volatile MLabPlugin.ToolRun		runner;
	private volatile boolean				cancelled;
	
	private Composite			root_panel;
	
	private StackLayout 		stack_layout;
	private Composite 			progress_panel;
	private Composite 			status_panel;
	private Label				result_label;
	private Button 				retest_button;
	
	private int	prog_value = 0;

	private StringBuffer summary = new StringBuffer();
	private StringBuffer details = new StringBuffer();
		
	
	protected
	MLabWizardNDT(
		MLabWizard							wizard,
		AbstractWizardPanel<MLabWizard>		prev )
	{
		super( wizard, prev );
	}

	@Override
	public void
	show() 
	{
		wizard.setTitle(MessageText.getString( "mlab.wizard.ndt.title" ));
        wizard.setCurrentInfo( MessageText.getString( "mlab.wizard.ndt.info" ));
        wizard.setPreviousEnabled(false);
        wizard.setFinishEnabled(false);

        root_panel = wizard.getPanel();
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		root_panel.setLayout(layout);

	   	log = new StyledText(root_panel,SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
    	GridData gridData = new GridData(GridData.FILL_BOTH);
    	gridData.horizontalSpan = 1;
    	log.setLayoutData(gridData);

    	
        Composite controlPanel = new Composite( root_panel, SWT.NULL );
    	gridData = new GridData(GridData.FILL_HORIZONTAL );
    	gridData.horizontalSpan = 1;
    	controlPanel.setLayoutData(gridData);

        stack_layout = new StackLayout();
        
        controlPanel.setLayout( stack_layout );
        
        Color bg = Utils.isDarkAppearanceNative()?null:Colors.white;
        
		progress_panel 	= new Composite( controlPanel, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 1;
		progress_panel.setLayout(layout);
		progress_panel.setBackground( bg );

		final ProgressBar prog = new ProgressBar( progress_panel, SWT.HORIZONTAL );
	   	gridData = new GridData(GridData.FILL_HORIZONTAL );
    	gridData.horizontalSpan = 1;
    	prog.setLayoutData(gridData);
    	prog.setBackground( bg );
		prog.setMinimum(0);
		prog.setMaximum(100);	
		
		
		status_panel 	= new Composite( controlPanel, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		status_panel.setLayout(layout);
		status_panel.setBackground( bg );
		
		result_label	= new Label( status_panel, SWT.NULL );
	   	gridData = new GridData(GridData.FILL_HORIZONTAL );
	   	result_label.setLayoutData(gridData);
	   	result_label.setBackground( bg );
	   	
	   	Button details_button = new Button( status_panel, SWT.NULL );
	   	details_button.setText( MessageText.getString( "mlab.wizard.details" ));
		
	   	details_button.addListener(
			SWT.Selection,
			new Listener()
			{
				@Override
				public void
				handleEvent(
					Event arg0 )
				{
					new TextViewerWindow( "mlab.wizard.details.title", "mlab.wizard.details.info", details.toString());
				}
			});
	   	
		retest_button = new Button( status_panel, SWT.NULL );
		retest_button.setText( MessageText.getString( "mlab.wizard.retest" ));
		
		retest_button.addListener(
			SWT.Selection,
			new Listener()
			{
				@Override
				public void
				handleEvent(
					Event arg0 )
				{
					retest_button.setEnabled( false );
					
					stack_layout.topControl = progress_panel;
			    	
					prog_value = 0;
					
					log.setText( "" );
					
					root_panel.layout( true, true );
					
					runTest();
				}
			});
		
		long up = wizard.getUploadRate();

		if ( up == 0 ){
			
			stack_layout.topControl = progress_panel;
			
			retest_button.setEnabled( false );

			final TimerEventPeriodic[] 	f_prog_timer = { null };

			f_prog_timer[0] =
				SimpleTimer.addPeriodicEvent(
					"ProgressUpdater",
					250,
					new TimerEventPerformer()
					{					
						@Override
						public void
						perform(
							TimerEvent event ) 
						{
							if ( progress_panel.isDisposed()){
								
								if ( f_prog_timer[0] != null ){
								
									f_prog_timer[0].cancel();
								}
							}else if ( stack_layout.topControl == progress_panel ){
								
								Utils.execSWTThread(
										new Runnable()
										{
											@Override
											public void
											run()
											{
												if ( !prog.isDisposed()){
												
													prog.setSelection( ( prog_value++ )%100 );
												}
											}
										});
							}
						}
					});
			
			runTest();
			
		}else{
			
			wizard.setFinishEnabled( true );
			
			setNextEnabled( true );
			
			stack_layout.topControl = status_panel;
			 	
			log.setText( summary.toString());
			
			result_label.setText( 
					MessageText.getString( "mlab.wizard.results",
					new String[]{ wizard.getRateString( up ) }));
		}
		
		root_panel.layout( true );
		
		cancelled = false;
	}
	
	private void
	setNextEnabled(
		boolean	enabled )
	{
		if ( Constants.isWindows || Constants.isOSX ){
		
			wizard.setNextEnabled( false );	// no shaper probe any more
			//wizard.setNextEnabled( enabled );
			
		}else{
			
			wizard.setNextEnabled( false );
		}
	}

	private void
	runTest()
	{
		wizard.setFinishEnabled( false );
				
		setNextEnabled( false );
		
		if ( wizard.pauseDownloads()){
			
			log.append( "Pausing downloads before performing test." );
		
			new AEThread2( "waiter" )
			{
				@Override
				public void
				run()
				{
					try{
						for (int i=0;i<50&&!cancelled;i++){
							
							final int f_i = i;
							
							Utils.execSWTThread(
								new Runnable()
								{
									@Override
									public void
									run()
									{
										if ( !log.isDisposed()){
											
											log.append( "." );
											
											if ( f_i == 49 ){
												
												log.append( "\n" );
											}
										}
									}
								});
							
							try{
								Thread.sleep(100);
								
							}catch( Throwable e ){
								
							}
						}
					}finally{
						
						runTestSupport();
					}
				}
			}.start();
			
		}else{
		
			runTestSupport();
		}
	}
	
	private void
	runTestSupport()
	{
		if ( cancelled ){
			
			return;
		}
		
	   	runner = wizard.getPlugin().
			runNDT(
				new ToolListener()
				{
					{
						summary.setLength(0);
						details.setLength(0);
					}
					
					@Override
					public void
					reportSummary(
						final String		str )
					{
						summary.append( str );
						summary.append( "\n" );

						Utils.execSWTThread(
							new Runnable()
							{
								@Override
								public void
								run()
								{
									if ( !log.isDisposed()){
									
										log.append( str + "\n" );
																			
										log.setSelection( log.getText().length());
										
										if ( stack_layout.topControl != progress_panel ){
										
											stack_layout.topControl = progress_panel;
	
											root_panel.layout( true, true );
										}
									}
								}
							});
					}
					
					@Override
					public void
					reportDetail(
						String		str )
					{
						details.append( str );
						details.append( "\n" );
					}
					
					@Override
					public void
					complete(
						final Map<String,Object>	results )
					{
						Utils.execSWTThread(
								new Runnable()
								{
									@Override
									public void
									run()
									{
										try{
											if ( !root_panel.isDisposed()){
											
												Long	up 		= (Long)results.get( "up" );
												Long	down 	= (Long)results.get( "down" );
												
												if ( up == null || up == 0 ){
													
													result_label.setText( MessageText.getString( "mlab.wizard.noresults" ) );
													
												}else{
													
													result_label.setText( 
														MessageText.getString( "mlab.wizard.results",
														new String[]{ wizard.getRateString( up ) }));
													
													wizard.setRates( up, down==null?0:down );
													
													wizard.setFinishEnabled( true );
																										
													setNextEnabled( true );
												}
												
												retest_button.setEnabled( true );
												
												stack_layout.topControl = status_panel;
		
												root_panel.layout( true, true );
												
											}
										}finally{ 
											
											runner = null;
										}
									}
								});
	
					}
				});
	   	
	   	if ( cancelled ){
			
			runner.cancel();
		}
	}
	
	@Override
	public boolean
	isNextEnabled()
	{
		return( false );
	}
	
	@Override
	public IWizardPanel
	getNextPanel() 
	{
		return(null);
	}
	
	@Override
	public boolean
	isPreviousEnabled() 
	{
		return( false );
	}

	@Override
	public void
	cancelled()
	{
		cancelled = true;
		
		if ( runner != null ){
		
			runner.cancel();
		}
	}
	
	@Override
	public IWizardPanel
	getFinishPanel() 
	{
		return( this );
	}
	
	@Override
	public void
	finish()
	{
		wizard.finish();
	}
}