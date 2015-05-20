/*
 * Copyright 2015 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.console.ng.gc.client.displayer;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import org.dashbuilder.dataset.DataColumn;
import org.dashbuilder.displayer.client.DataSetHandlerImpl;
import org.dashbuilder.renderer.client.table.TableDisplayer;
import org.uberfire.ext.widgets.common.client.tables.FilterPagedTable;
import org.uberfire.ext.widgets.common.client.tables.PagedTable;
import org.uberfire.ext.widgets.common.client.tables.popup.NewTabFilterPopup;
import org.uberfire.mvp.Command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base implementation for all the table displayers within the jBPM console
 */
public abstract class BaseTableDisplayer<T> extends TableDisplayer {

    List<TableSettings> tableSettingsList = new ArrayList<TableSettings>();
    HashMap<String, TableSettings> tableSettingsHashMap = new HashMap<String, TableSettings>(  );

    protected boolean tableRefreshEnabled = false;
    protected boolean tableSelectorEnabled = false;
    protected boolean tableCreationEnabled = false;
    protected transient String _currentTable = null;

    public boolean isTableRefreshEnabled() {
        return tableRefreshEnabled;
    }

    public void setTableRefreshEnabled(boolean tableRefreshEnabled) {
        this.tableRefreshEnabled = tableRefreshEnabled;
    }

    public boolean isTableSelectorEnabled() {
        return tableSelectorEnabled;
    }

    public void setTableSelectorEnabled(boolean tableSelectorEnabled) {
        this.tableSelectorEnabled = tableSelectorEnabled;
    }

    public boolean isTableCreationEnabled() {
        return tableCreationEnabled;
    }

    public void setTableCreationEnabled(boolean tableCreationEnabled) {
        this.tableCreationEnabled = tableCreationEnabled;
    }

    public TableSettings createTableSettingsPrototype() {
        return (TableSettings ) TableSettingsBuilder.init()
                .tableWidth(1000)
                .buildSettings();
    }

    public List<TableSettings> getTableSettingsList() {
        return tableSettingsList;
    }

    public TableSettings getTableSettingsByKey(String key) {
        return tableSettingsHashMap.get( key );
    }

    public void addTableSettings(String name, boolean editable, TableSettings settings) {
        settings.setTableName(name);
        settings.setEditable(editable);
        addTableSettings( settings );
    }

    public void addTableSettings(TableSettings settings) {
        tableSettingsList.add(settings);
        tableSettingsHashMap.put(settings.getKey(),settings);

        // Take the first registered settings as the default one
        if (super.getDisplayerSettings() == null) {
            init( settings );
        }
    }

    public void removeTableSettings(TableSettings settings) {
        tableSettingsList.remove( settings );
    }

    public void updateTableSettings(TableSettings tableSettings) {
        int idx = tableSettingsList.indexOf(tableSettings);
        if (idx == -1) {
            tableSettingsList.add(tableSettings);
        } else {
            tableSettingsList.remove(idx);
            tableSettingsList.add(idx, tableSettings);
        }
        tableSettingsHashMap.put( tableSettings.getKey(),tableSettings);
    }

    public void clearTableSettingsList() {
        tableSettingsList.clear();
        tableSettingsHashMap.clear();
    }

    public void init(TableSettings settings) {
        super.setDisplayerSettings(settings);
        super.setDataSetHandler(new DataSetHandlerImpl(settings.getDataSetLookup()));
        _currentTable = settings.getTableName();
    }

    public void draw(TableSettings tableSettings) {
        init(tableSettings);

        super.drawn = false;
        super.draw();
    }

    public void draw(int settingsIdx) {
        if (settingsIdx >= 0 && settingsIdx < tableSettingsList.size()) {
            TableSettings tableSettings = tableSettingsList.get(settingsIdx);
            draw(tableSettings);
        }
    }

    public String missingColumn(String... columnIds) {
        for (String columnId : columnIds) {
            if (dataSet.getColumnById(columnId) == null) {
                return columnId;
            }
        }
        return null;
    }

    // Items manipulation

    /**
     * Get the list of items being displayed at the current table page.
     * @return A list of objects representing the items
     */
    public List<T> getCurrentPageItems() {
        List<T> itemList = new ArrayList<T>();
        Map<String,Object> itemValues = new HashMap<String,Object>(dataSet.getColumns().size());
        for (int i = 0; i < dataSet.getRowCount(); i++) {
            for (DataColumn column : dataSet.getColumns()) {
                itemValues.put(column.getId(), dataSet.getValueAt(i, column.getId()));
            }
            T item = createItem(itemValues);
            itemList.add(item);
        }
        return itemList;
    }

    /**
     * Transform a data set row into a object
     * @param rowIndex The item's row index
     * @return An object representing the data set row.
     */
    protected T createItem(int rowIndex) {
        Map<String,Object> itemValues = new HashMap<String,Object>(dataSet.getColumns().size());
        for (DataColumn column : dataSet.getColumns()) {
            itemValues.put(column.getId(), dataSet.getValueAt(rowIndex, column.getId()));
        }
        return createItem(itemValues);
    }

    /**
     * Transform a data set row into a object
     * @param itemValues The values for the item
     * @return An object representing the data set row.
     */
    protected abstract T createItem(Map<String,Object> itemValues);

    /**
     * Invoked when an item is selected
     * @param item The selected item.
     */
    protected abstract void onItemSelected(T item);

    /**
     * Get the popup title to show when creating a brand new table settings
     */
    protected abstract String getNewTableSettingsTitle();


    // TableDisplayer overrides

    ListBox tableSettingsListBox = createTableSelector();

    @Override
    protected PagedTable<Integer> createTable() {
        PagedTable<Integer> pagedTable = super.createTable();
        if (isTableSelectorEnabled()) {
            populateTableSelector();
            pagedTable.getLeftToolbar().add(tableSettingsListBox);
        }
        if (isTableRefreshEnabled()) {
            Widget refreshWidget = createRefreshWidget();
            pagedTable.getRightToolbar().add( refreshWidget );
        }
        return pagedTable;
    }

    protected ListBox createTableSelector() {
        final ListBox listBox = new ListBox();
        listBox.addChangeHandler( new ChangeHandler() {
            public void onChange( ChangeEvent event ) {
                int idx = listBox.getSelectedIndex();
                if ( isTableCreationEnabled() && idx == tableSettingsList.size() ) {
                    TableSettings prototype = createTableSettingsPrototype();
                    //prototype.setTableName("- New Task list - ");
                    //prototype.setTableDescription("- New Task list -");
                    //showTableSettingsEditor( prototype );
                } else {
                    draw( listBox.getSelectedIndex() );
                }
            }
        } );
        return listBox;
    }

    protected void populateTableSelector() {
        tableSettingsListBox.clear();
        for (int i = 0; i < tableSettingsList.size(); i++) {
            TableSettings settings = tableSettingsList.get(i);
            tableSettingsListBox.addItem(settings.getTableName());
            if (_currentTable != null && _currentTable.equals(settings.getTableName())) {
                tableSettingsListBox.setSelectedIndex(i);
            }
        }
        if (isTableCreationEnabled()) {
            tableSettingsListBox.addItem("- New filter -");
        }
    }

    protected Widget createRefreshWidget() {
        Button refresh = new Button("Refresh");
        refresh.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                redraw();
            }
        });
        return refresh;
    }

    TableDisplayerEditorPopup tableDisplayerEditorPopup = new TableDisplayerEditorPopup();

    protected void showTableSettingsEditor(TableSettings tableSettings) {
        TableSettings clone = tableSettings.cloneInstance();
        tableDisplayerEditorPopup.setTitle( getNewTableSettingsTitle() );
        tableDisplayerEditorPopup.show( clone, new TableDisplayerEditor.Listener() {

            public void onClose( TableDisplayerEditor editor ) {
            }

            public void onSave( TableDisplayerEditor editor ) {
                TableSettings modifiedSettings = editor.getTableSettings();
                updateTableSettings( modifiedSettings );
                //populateTableSelector();
                draw( modifiedSettings );
            }
        } );
    }

    public void showTableSettingsEditor(TableSettings tableSettings,final Command drawCommand, final FilterPagedTable<T> filterPagedTable) {
        TableSettings clone = tableSettings.cloneInstance();
        tableDisplayerEditorPopup.setTitle( getNewTableSettingsTitle() );
        tableDisplayerEditorPopup.show( clone, new TableDisplayerEditor.Listener() {

            public void onClose( TableDisplayerEditor editor ) {
            }

            public void onSave( TableDisplayerEditor editor ) {
                TableSettings modifiedSettings = editor.getTableSettings();
                updateTableSettings( modifiedSettings );
                HashMap<String, Object> tabSettingsValues = new HashMap<String, Object>();

                tabSettingsValues.put( NewTabFilterPopup.FILTER_TAB_NAME_PARAM, modifiedSettings.getTableName() );
                tabSettingsValues.put( NewTabFilterPopup.FILTER_TAB_DESC_PARAM, modifiedSettings.getTableName() );

                filterPagedTable.saveNewTabSettings( modifiedSettings.getKey(), new HashMap<String, Object>() );
                populateTableSelector();
                drawCommand.execute();
            }
        } );
    }

    @Override
    protected void onCellSelected(String columnId, boolean selectable, int rowIndex) {
        super.onCellSelected(columnId, selectable, rowIndex);
        this.onItemSelected(createItem(rowIndex));
    }
}