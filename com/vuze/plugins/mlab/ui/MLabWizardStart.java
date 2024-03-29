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


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import com.biglybt.core.internat.MessageText;
import com.biglybt.ui.swt.Utils;
import com.biglybt.ui.swt.components.LinkLabel;
import com.biglybt.ui.swt.mainwindow.Colors;
import com.biglybt.ui.swt.wizard.AbstractWizardPanel;
import com.biglybt.ui.swt.wizard.IWizardPanel;

public class 
MLabWizardStart
	extends AbstractWizardPanel<MLabWizard> 
{
	protected
	MLabWizardStart(
		MLabWizard		wizard )
	{
		super( wizard, null );
	}

	@Override
	public void
	show() 
	{
		wizard.setTitle(MessageText.getString( "mlab.wizard.intro.title" ));
        wizard.setCurrentInfo( "" );
        wizard.setPreviousEnabled(false);
        wizard.setFinishEnabled(false);

        Color bg = Utils.isDarkAppearanceNative()?null:Colors.white;

        Composite rootPanel = wizard.getPanel();
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		rootPanel.setLayout(layout);
		rootPanel.setBackground( bg );
		
		Image img = wizard.getImage( "com/vuze/plugins/mlab/ui/resources/mlab.png" );
		
		Label img_label = new Label( rootPanel, SWT.NULL );
        GridData gridData = new GridData();
        img_label.setLayoutData(gridData);
        img_label.setImage( img );
        img_label.setBackground( bg );
        
		Label info_label = new Label( rootPanel, SWT.WRAP );
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		info_label.setLayoutData(gridData);
		info_label.setBackground( bg );
		info_label.setText( MessageText.getString( "mlab.info" ));
 
		LinkLabel link = new LinkLabel( rootPanel, "mlab.link", MessageText.getString( "mlab.link.url" ));
		Label link_label = link.getlabel();
		
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan=2;
		gridData.verticalIndent=30;
		link_label.setLayoutData(gridData);
        link_label.setBackground( bg );
        
		Label next_label = new Label( rootPanel, SWT.NULL );
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan=2;
		gridData.verticalIndent=30;
		next_label.setLayoutData(gridData);
		next_label.setBackground( bg );
		next_label.setText( MessageText.getString("mlab.wizard.intro.next"));
	}
	
	@Override
	public boolean
	isNextEnabled()
	{
		return( true );
	}
	
	@Override
	public IWizardPanel
	getNextPanel() 
	{
		return( new MLabWizardNDT( wizard, this ));
	}
}
