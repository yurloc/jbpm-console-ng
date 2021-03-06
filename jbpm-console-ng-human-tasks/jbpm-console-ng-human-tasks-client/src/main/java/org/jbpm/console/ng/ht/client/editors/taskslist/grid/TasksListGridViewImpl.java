/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.console.ng.ht.client.editors.taskslist.grid;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CompositeCell;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.ButtonSize;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.jbpm.console.ng.gc.client.experimental.grid.base.ExtendedPagedTable;
import org.jbpm.console.ng.gc.client.list.base.AbstractMultiGridView;
import org.jbpm.console.ng.gc.client.util.ButtonActionCell;
import org.jbpm.console.ng.gc.client.util.TaskUtils;
import org.jbpm.console.ng.ht.client.editors.quicknewtask.QuickNewTaskPopup;
import org.jbpm.console.ng.ht.client.i18n.Constants;
import org.jbpm.console.ng.ht.model.TaskSummary;
import org.jbpm.console.ng.ht.model.events.NewTaskEvent;
import org.jbpm.console.ng.ht.model.events.TaskRefreshedEvent;
import org.jbpm.console.ng.ht.model.events.TaskSelectionEvent;
import org.jbpm.console.ng.ht.util.TaskRoleDefinition;
import org.uberfire.client.mvp.PlaceStatus;
import org.uberfire.ext.services.shared.preferences.GridGlobalPreferences;
import org.uberfire.ext.widgets.common.client.tables.ColumnMeta;
import org.uberfire.ext.widgets.common.client.tables.popup.NewTabFilterPopup;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.impl.DefaultPlaceRequest;

import static org.jbpm.console.ng.ht.util.TaskRoleDefinition.*;

@Dependent
public class TasksListGridViewImpl extends AbstractMultiGridView<TaskSummary, TasksListGridPresenter>
        implements TasksListGridPresenter.TaskListView {

    private final Constants constants = GWT.create( Constants.class );
    public static final String COL_ID_ID = "t.id";
    public static final String COL_ID_NAME = "t.name";
    public static final String COL_ID_DESCRIPTION = "t.description";
    public static final String COL_ID_PRIORITY = "t.priority";
    public static final String COL_ID_STATUS = "t.taskData.status";
    public static final String COL_ID_CREATEON = "t.taskData.createdOn";
    public static final String COL_ID_EXPIRATIONTIME = "t.taskData.expirationTime";
    public static final String COL_ID_ACTIONS = "Actions";

    @Inject
    private Event<TaskSelectionEvent> taskSelected;

    @Inject
    private QuickNewTaskPopup quickNewTaskPopup;

    @Inject
    private NewTabFilterPopup newTabFilterPopup;

    @Override
    public void init( final TasksListGridPresenter presenter ) {
        final List<String> bannedColumns = new ArrayList<String>();
        bannedColumns.add( COL_ID_NAME );
        bannedColumns.add( COL_ID_ACTIONS );
        final List<String> initColumns = new ArrayList<String>();
        initColumns.add( COL_ID_NAME );
        initColumns.add( COL_ID_DESCRIPTION );
        initColumns.add( COL_ID_ACTIONS );

        final Button button = new Button();
        button.setIcon( IconType.PLUS );
        button.setSize( ButtonSize.SMALL );
        button.addClickHandler( new ClickHandler() {
            public void onClick( ClickEvent event ) {
                Command addNewGrid = new Command() {
                    @Override
                    public void execute() {
                        HashMap<String, Object> newTabFormValues = newTabFilterPopup.getFormValues();

                        //Convert value of list of roles selected  to String
                        List<String> selectedCurrentRole = (List) newTabFormValues.get( TasksListGridPresenter.FILTER_CURRENT_ROLE_PARAM_NAME );
                        String currentRole = null;
                        if ( selectedCurrentRole != null && selectedCurrentRole.size() > 0 ) {
                            currentRole = selectedCurrentRole.get( 0 );
                        }
                        newTabFormValues.put( TasksListGridPresenter.FILTER_CURRENT_ROLE_PARAM_NAME, currentRole );

                        final String key = getValidKeyForAdditionalListGrid( "TaskListGrid_" );

                        filterPagedTable.saveNewTabSettings( key, newTabFormValues );
                        final ExtendedPagedTable<TaskSummary> extendedPagedTable = createGridInstance( new GridGlobalPreferences( key, initColumns, bannedColumns ), key );

                        extendedPagedTable.setDataProvider( presenter.getDataProvider() );

                        filterPagedTable.createNewTab( extendedPagedTable, key, button, new Command() {
                            @Override
                            public void execute() {
                                currentListGrid = extendedPagedTable;
                                applyFilterOnPresenter( key );
                            }
                        } );
                        applyFilterOnPresenter( newTabFormValues );

                    }
                };
                createFilterForm();
                newTabFilterPopup.show( addNewGrid, getMultiGridPreferencesStore() );

            }
        } );
        super.init( presenter, new GridGlobalPreferences( "TaskListGrid", initColumns, bannedColumns ), button );

    }

    public void initSelectionModel() {
        final ExtendedPagedTable<TaskSummary> extendedPagedTable = getListGrid();
        selectedStyles = new RowStyles<TaskSummary>() {

            @Override
            public String getStyleNames( TaskSummary row,
                                         int rowIndex ) {
                if ( rowIndex == selectedRow ) {
                    return "selected";
                } else {
                    if ( row.getStatus().equals( "InProgress" ) || row.getStatus().equals( "Ready" ) ) {
                        if ( row.getPriority() == 5 ) {
                            return "five";
                        } else if ( row.getPriority() == 4 ) {
                            return "four";
                        } else if ( row.getPriority() == 3 ) {
                            return "three";
                        } else if ( row.getPriority() == 2 ) {
                            return "two";
                        } else if ( row.getPriority() == 1 ) {
                            return "one";
                        }
                    } else if ( row.getStatus().equals( "Completed" ) ) {
                        return "completed";
                    }

                }
                return null;
            }
        };

        extendedPagedTable.setEmptyTableCaption( constants.No_Tasks_Found() );

        selectionModel = new NoSelectionModel<TaskSummary>();
        selectionModel.addSelectionChangeHandler( new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange( SelectionChangeEvent event ) {
                boolean close = false;
                if ( selectedRow == -1 ) {
                    selectedRow = extendedPagedTable.getKeyboardSelectedRow();
                    extendedPagedTable.setRowStyles( selectedStyles );
                    extendedPagedTable.redraw();

                } else if ( extendedPagedTable.getKeyboardSelectedRow() != selectedRow ) {
                    extendedPagedTable.setRowStyles( selectedStyles );
                    selectedRow = extendedPagedTable.getKeyboardSelectedRow();
                    extendedPagedTable.redraw();
                } else {
                    close = true;
                }

                selectedItem = selectionModel.getLastSelectedObject();

                DefaultPlaceRequest defaultPlaceRequest = new DefaultPlaceRequest( "Task Details Multi" );
                PlaceStatus status = placeManager.getStatus( defaultPlaceRequest );
                boolean logOnly = false;
                if ( selectedItem.getStatus().equals( "Completed" ) && selectedItem.isLogOnly() ) {
                    logOnly = true;
                }
                if ( status == PlaceStatus.CLOSE ) {
                    placeManager.goTo( defaultPlaceRequest );
                    taskSelected.fire( new TaskSelectionEvent( selectedItem.getTaskId(), selectedItem.getTaskName(), selectedItem.isForAdmin(), logOnly ) );
                } else if ( status == PlaceStatus.OPEN && !close ) {
                    taskSelected.fire( new TaskSelectionEvent( selectedItem.getTaskId(), selectedItem.getTaskName(), selectedItem.isForAdmin(), logOnly ) );
                } else if ( status == PlaceStatus.OPEN && close ) {
                    placeManager.closePlace( "Task Details Multi" );
                }

            }
        } );

        noActionColumnManager = DefaultSelectionEventManager
                .createCustomManager( new DefaultSelectionEventManager.EventTranslator<TaskSummary>() {

                    @Override
                    public boolean clearCurrentSelection( CellPreviewEvent<TaskSummary> event ) {
                        return false;
                    }

                    @Override
                    public DefaultSelectionEventManager.SelectAction translateSelectionEvent( CellPreviewEvent<TaskSummary> event ) {
                        NativeEvent nativeEvent = event.getNativeEvent();
                        if ( BrowserEvents.CLICK.equals( nativeEvent.getType() ) ) {
                            // Ignore if the event didn't occur in the correct column.
                            if ( extendedPagedTable.getColumnIndex( actionsColumn ) == event.getColumn() ) {
                                return DefaultSelectionEventManager.SelectAction.IGNORE;
                            }
                        }
                        return DefaultSelectionEventManager.SelectAction.DEFAULT;
                    }
                } );
        extendedPagedTable.setSelectionModel( selectionModel, noActionColumnManager );
        extendedPagedTable.setRowStyles( selectedStyles );

    }

    @Override
    public void initExtraButtons( ExtendedPagedTable extendedPagedTable ) {
        Button newTaskButton = new Button();
        newTaskButton.setTitle( constants.New_Task() );
        newTaskButton.setIcon( IconType.PLUS );
        newTaskButton.setTitle( Constants.INSTANCE.New_Task() );
        newTaskButton.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                quickNewTaskPopup.show();
            }
        } );

        extendedPagedTable.getRightActionsToolbar().clear();
        extendedPagedTable.getRightActionsToolbar().add( newTaskButton );

    }

    @Override
    public void initColumns( ExtendedPagedTable extendedPagedTable ) {
        initCellPreview( extendedPagedTable );
        Column taskIdColumn = initTaskIdColumn();
        Column taskNameColumn = initTaskNameColumn();
        Column descriptionColumn = initTaskDescriptionColumn();
        Column taskPriorityColumn = initTaskPriorityColumn();
        Column statusColumn = initTaskStatusColumn();
        Column createdOnDateColumn = initTaskCreatedOnColumn();
        Column dueDateColumn = initTaskDueColumn();
        actionsColumn = initActionsColumn( extendedPagedTable );

        List<ColumnMeta<TaskSummary>> columnMetas = new ArrayList<ColumnMeta<TaskSummary>>();
        columnMetas.add( new ColumnMeta<TaskSummary>( taskIdColumn, constants.Id() ) );
        columnMetas.add( new ColumnMeta<TaskSummary>( taskNameColumn, constants.Task() ) );
        columnMetas.add( new ColumnMeta<TaskSummary>( descriptionColumn, constants.Description() ) );
        columnMetas.add( new ColumnMeta<TaskSummary>( taskPriorityColumn, constants.Priority() ) );
        columnMetas.add( new ColumnMeta<TaskSummary>( statusColumn, constants.Status() ) );
        columnMetas.add( new ColumnMeta<TaskSummary>( createdOnDateColumn, "CreatedOn" ) );
        columnMetas.add( new ColumnMeta<TaskSummary>( dueDateColumn, "DueOn" ) );
        columnMetas.add( new ColumnMeta<TaskSummary>( actionsColumn, constants.Actions() ) );
        extendedPagedTable.addColumns( columnMetas );
    }

    private void createFilterForm() {
        HashMap<String, String> stateListBoxInfo = new HashMap<String, String>();

        stateListBoxInfo.put( TaskUtils.TASK_STATUS_CREATED, Constants.INSTANCE.Created() );
        stateListBoxInfo.put( TaskUtils.TASK_STATUS_READY, Constants.INSTANCE.Ready() );
        stateListBoxInfo.put( TaskUtils.TASK_STATUS_RESERVED, Constants.INSTANCE.Reserved() );
        stateListBoxInfo.put( TaskUtils.TASK_STATUS_INPROGRESS, Constants.INSTANCE.InProgress() );
        stateListBoxInfo.put( TaskUtils.TASK_STATUS_SUSPENDED, Constants.INSTANCE.Suspended() );
        stateListBoxInfo.put( TaskUtils.TASK_STATUS_FAILED, Constants.INSTANCE.Failed() );
        stateListBoxInfo.put( TaskUtils.TASK_STATUS_ERROR, Constants.INSTANCE.Error() );
        stateListBoxInfo.put( TaskUtils.TASK_STATUS_EXITED, Constants.INSTANCE.Exited() );
        stateListBoxInfo.put( TaskUtils.TASK_STATUS_OBSOLETE, Constants.INSTANCE.Obsolete() );
        stateListBoxInfo.put( TaskUtils.TASK_STATUS_COMPLETED, Constants.INSTANCE.Completed() );

        HashMap<String, String> currentRoleListBoxInfo = new HashMap<String, String>();

        currentRoleListBoxInfo.put( TaskRoleDefinition.TASK_ROLE_INITIATOR, Constants.INSTANCE.Initiator() );
        currentRoleListBoxInfo.put( TaskRoleDefinition.TASK_ROLE_STAKEHOLDER, Constants.INSTANCE.Stakeholder() );
        currentRoleListBoxInfo.put( TaskRoleDefinition.TASK_ROLE_POTENTIALOWNER, Constants.INSTANCE.Potential_Owner() );
        currentRoleListBoxInfo.put( TaskRoleDefinition.TASK_ROLE_ACTUALOWNER, Constants.INSTANCE.Actual_Owner() );
        currentRoleListBoxInfo.put( TaskRoleDefinition.TASK_ROLE_ADMINISTRATOR, Constants.INSTANCE.Administrator() );

        newTabFilterPopup.init();
        newTabFilterPopup.addListBoxToFilter( Constants.INSTANCE.Status(), TasksListGridPresenter.FILTER_STATUSES_PARAM_NAME, true, stateListBoxInfo );
        newTabFilterPopup.addListBoxToFilter( Constants.INSTANCE.TaskRole(), TasksListGridPresenter.FILTER_CURRENT_ROLE_PARAM_NAME, false, currentRoleListBoxInfo );

    }

    private void initCellPreview( final ExtendedPagedTable extendedPagedTable ) {
        extendedPagedTable.addCellPreviewHandler( new CellPreviewEvent.Handler<TaskSummary>() {

            @Override
            public void onCellPreview( final CellPreviewEvent<TaskSummary> event ) {

                if ( BrowserEvents.MOUSEOVER.equalsIgnoreCase( event.getNativeEvent().getType() ) ) {
                    onMouseOverGrid( extendedPagedTable, event );
                }

            }
        } );

    }

    private void onMouseOverGrid( ExtendedPagedTable extendedPagedTable,
                                  final CellPreviewEvent<TaskSummary> event ) {
        TaskSummary task = event.getValue();

        if ( task.getDescription() != null ) {
            extendedPagedTable.setTooltip( extendedPagedTable.getKeyboardSelectedRow(), event.getColumn(), task.getDescription() );
        }
    }

    private Column initTaskIdColumn() {
        Column<TaskSummary, Number> taskIdColumn = new Column<TaskSummary, Number>( new NumberCell() ) {
            @Override
            public Number getValue( TaskSummary object ) {
                return object.getTaskId();
            }
        };
        taskIdColumn.setSortable( true );
        taskIdColumn.setDataStoreName( COL_ID_ID );
        return taskIdColumn;
    }

    private Column initTaskNameColumn() {
        Column<TaskSummary, String> taskNameColumn = new Column<TaskSummary, String>( new TextCell() ) {
            @Override
            public String getValue( TaskSummary object ) {
                return object.getTaskName();
            }
        };
        taskNameColumn.setSortable( true );
        taskNameColumn.setDataStoreName( COL_ID_NAME );
        return taskNameColumn;
    }

    private Column initTaskDescriptionColumn() {
        Column<TaskSummary, String> descriptionColumn = new Column<TaskSummary, String>( new TextCell() ) {
            @Override
            public String getValue( TaskSummary object ) {
                return object.getDescription();
            }
        };
        descriptionColumn.setSortable( true );
        descriptionColumn.setDataStoreName( COL_ID_DESCRIPTION );
        return descriptionColumn;
    }

    private Column initTaskPriorityColumn() {
        Column<TaskSummary, Number> taskPriorityColumn = new Column<TaskSummary, Number>( new NumberCell() ) {
            @Override
            public Number getValue( TaskSummary object ) {
                return object.getPriority();
            }
        };
        taskPriorityColumn.setSortable( true );
        taskPriorityColumn.setDataStoreName( COL_ID_PRIORITY );
        return taskPriorityColumn;
    }

    private Column initTaskStatusColumn() {
        Column<TaskSummary, String> statusColumn = new Column<TaskSummary, String>( new TextCell() ) {
            @Override
            public String getValue( TaskSummary object ) {
                return object.getStatus();
            }
        };
        statusColumn.setSortable( true );
        statusColumn.setDataStoreName( COL_ID_STATUS );
        return statusColumn;
    }

    private Column initTaskCreatedOnColumn() {
        Column<TaskSummary, String> createdOnDateColumn = new Column<TaskSummary, String>( new TextCell() ) {
            @Override
            public String getValue( TaskSummary object ) {
                if ( object.getCreatedOn() != null ) {
                    Date createdOn = object.getCreatedOn();
                    DateTimeFormat format = DateTimeFormat.getFormat( "dd/MM/yyyy HH:mm" );
                    return format.format( createdOn );
                }
                return "";
            }
        };
        createdOnDateColumn.setSortable( true );
        createdOnDateColumn.setDataStoreName( COL_ID_CREATEON );
        return createdOnDateColumn;
    }

    private Column initTaskDueColumn() {
        Column<TaskSummary, String> dueDateColumn = new Column<TaskSummary, String>( new TextCell() ) {
            @Override
            public String getValue( TaskSummary object ) {
                if ( object.getExpirationTime() != null ) {
                    Date expirationTime = object.getExpirationTime();
                    DateTimeFormat format = DateTimeFormat.getFormat( "dd/MM/yyyy HH:mm" );
                    return format.format( expirationTime );
                }
                return "";
            }
        };
        dueDateColumn.setSortable( true );
        dueDateColumn.setDataStoreName( COL_ID_EXPIRATIONTIME );
        return dueDateColumn;
    }

    public void onTaskRefreshedEvent( @Observes TaskRefreshedEvent event ) {
        presenter.refreshGrid();
    }

    private Column initActionsColumn( final ExtendedPagedTable extendedPagedTable ) {
        List<HasCell<TaskSummary, ?>> cells = new LinkedList<HasCell<TaskSummary, ?>>();
        cells.add( new ClaimActionHasCell( constants.Claim(), new ActionCell.Delegate<TaskSummary>() {
            @Override
            public void execute( TaskSummary task ) {

                presenter.claimTask( task.getTaskId(), identity.getIdentifier(), task.getDeploymentId() );
                taskSelected.fire( new TaskSelectionEvent( task.getTaskId(), task.getTaskName() ) );
                extendedPagedTable.refresh();
            }
        } ) );

        cells.add( new ReleaseActionHasCell( constants.Release(), new ActionCell.Delegate<TaskSummary>() {
            @Override
            public void execute( TaskSummary task ) {

                presenter.releaseTask( task.getTaskId(), identity.getIdentifier() );
                taskSelected.fire( new TaskSelectionEvent( task.getTaskId(), task.getTaskName() ) );
                extendedPagedTable.refresh();
            }
        } ) );

        cells.add( new CompleteActionHasCell( constants.Complete(), new ActionCell.Delegate<TaskSummary>() {
            @Override
            public void execute( TaskSummary task ) {
                placeManager.goTo( "Task Details Multi" );
                boolean logOnly = false;
                if ( task.getStatus().equals( "Completed" ) && task.isLogOnly() ) {
                    logOnly = true;
                }
                taskSelected.fire( new TaskSelectionEvent( task.getTaskId(), task.getName(), task.isForAdmin(), logOnly ) );
            }
        } ) );

        CompositeCell<TaskSummary> cell = new CompositeCell<TaskSummary>( cells );
        Column<TaskSummary, TaskSummary> actionsColumn = new Column<TaskSummary, TaskSummary>( cell ) {
            @Override
            public TaskSummary getValue( TaskSummary object ) {
                return object;
            }
        };
        actionsColumn.setDataStoreName( COL_ID_ACTIONS );
        return actionsColumn;

    }

    public void refreshNewTask( @Observes NewTaskEvent newTask ) {
        presenter.refreshGrid();
        PlaceStatus status = placeManager.getStatus( new DefaultPlaceRequest( "Task Details Multi" ) );
        if ( status == PlaceStatus.OPEN ) {
            taskSelected.fire( new TaskSelectionEvent( newTask.getNewTaskId(), newTask.getNewTaskName() ) );
        } else {
            placeManager.goTo( "Task Details Multi" );
            taskSelected.fire( new TaskSelectionEvent( newTask.getNewTaskId(), newTask.getNewTaskName() ) );
        }

        selectionModel.setSelected( new TaskSummary( newTask.getNewTaskId(), newTask.getNewTaskName() ), true );
    }

    protected class CompleteActionHasCell extends ButtonActionCell<TaskSummary> {

        public CompleteActionHasCell( final String text, final ActionCell.Delegate<TaskSummary> delegate ) {
            super( text, delegate );
        }

        @Override
        public void render( final Cell.Context context, final TaskSummary value, final SafeHtmlBuilder sb ) {
            if ( value.getActualOwner() != null && value.getStatus().equals( "InProgress" ) ) {
                super.render( context, value, sb );
            }
        }
    }

    protected class ClaimActionHasCell extends ButtonActionCell<TaskSummary> {

        public ClaimActionHasCell( final String text, final ActionCell.Delegate<TaskSummary> delegate ) {
            super( text, delegate );
        }

        @Override
        public void render( final Cell.Context context, final TaskSummary value, final SafeHtmlBuilder sb ) {
            if ( value.getStatus().equals( "Ready" ) ) {
                super.render( context, value, sb );
            }
        }
    }

    protected class ReleaseActionHasCell extends ButtonActionCell<TaskSummary> {

        public ReleaseActionHasCell( final String text, final ActionCell.Delegate<TaskSummary> delegate ) {
            super( text, delegate );
        }

        @Override
        public void render( final Cell.Context context, final TaskSummary value, final SafeHtmlBuilder sb ) {
            if ( value.getActualOwner() != null && value.getActualOwner().equals( identity.getIdentifier() )
                    && ( value.getStatus().equals( "Reserved" ) || value.getStatus().equals( "InProgress" ) ) ) {
                super.render( context, value, sb );
            }
        }
    }

    private PlaceStatus getPlaceStatus( String place ) {
        DefaultPlaceRequest defaultPlaceRequest = new DefaultPlaceRequest( place );
        PlaceStatus status = placeManager.getStatus( defaultPlaceRequest );
        return status;
    }

    private void closePlace( String place ) {
        if ( getPlaceStatus( place ) == PlaceStatus.OPEN ) {
            placeManager.closePlace( place );
        }
    }

    public void initDefaultFilters( GridGlobalPreferences preferences,
                                    Button createTabButton ) {

        List<String> states;
        presenter.setAddingDefaultFilters( true );
        //Filter status Active
        states = TaskUtils.getStatusByType( TaskUtils.TaskType.ACTIVE );
        initTabFilter( preferences, "TaskListGrid_0", Constants.INSTANCE.Active(), "Filter " + Constants.INSTANCE.Active(), states, TASK_ROLE_POTENTIALOWNER );

        //Filter status Personal
        states = TaskUtils.getStatusByType( TaskUtils.TaskType.PERSONAL );
        initTabFilter( preferences, "TaskListGrid_1", Constants.INSTANCE.Personal(), "Filter " + Constants.INSTANCE.Personal(), states, TASK_ROLE_POTENTIALOWNER );

        //Filter status Group
        states = TaskUtils.getStatusByType( TaskUtils.TaskType.GROUP );
        initTabFilter( preferences, "TaskListGrid_2", Constants.INSTANCE.Group(), "Filter " + Constants.INSTANCE.Group(), states, TASK_ROLE_POTENTIALOWNER );

        //Filter status All
        states = TaskUtils.getStatusByType( TaskUtils.TaskType.ALL );
        initTabFilter( preferences, "TaskListGrid_3", Constants.INSTANCE.All(), "Filter " + Constants.INSTANCE.All(), states, TASK_ROLE_POTENTIALOWNER );

        //Filter status Admin
        states = TaskUtils.getStatusByType( TaskUtils.TaskType.ADMIN );
        initTabFilter( preferences, "TaskListGrid_4", Constants.INSTANCE.Task_Admin(), "Filter " + Constants.INSTANCE.Task_Admin(), states, TASK_ROLE_ADMINISTRATOR );

        filterPagedTable.addAddTableButton( createTabButton );

        presenter.setAddingDefaultFilters( false );
        getMultiGridPreferencesStore().setSelectedGrid( "TaskListGrid_0" );
        filterPagedTable.setSelectedTab();
        applyFilterOnPresenter( "TaskListGrid_0" );

    }

    private void initTabFilter( GridGlobalPreferences preferences,
                                final String key,
                                String tabName,
                                String tabDesc,
                                List<String> states,
                                String role ) {

        HashMap<String, Object> tabSettingsValues = new HashMap<String, Object>();

        tabSettingsValues.put( NewTabFilterPopup.FILTER_TAB_NAME_PARAM, tabName );
        tabSettingsValues.put( NewTabFilterPopup.FILTER_TAB_DESC_PARAM, tabDesc );
        tabSettingsValues.put( TasksListGridPresenter.FILTER_STATUSES_PARAM_NAME, states );
        tabSettingsValues.put( TasksListGridPresenter.FILTER_CURRENT_ROLE_PARAM_NAME, role );

        filterPagedTable.saveNewTabSettings( key, tabSettingsValues );

        final ExtendedPagedTable<TaskSummary> extendedPagedTable = createGridInstance( new GridGlobalPreferences( key, preferences.getInitialColumns(), preferences.getBannedColumns() ), key );
        currentListGrid = extendedPagedTable;
        extendedPagedTable.setDataProvider( presenter.getDataProvider() );
        filterPagedTable.addTab( extendedPagedTable, key, new Command() {
            @Override
            public void execute() {
                currentListGrid = extendedPagedTable;
                applyFilterOnPresenter( key );
            }
        } );

    }

    public void applyFilterOnPresenter( HashMap<String, Object> params ) {
        List<String> states = (List) params.get( TasksListGridPresenter.FILTER_STATUSES_PARAM_NAME );
        String currentRole = (String) params.get( TasksListGridPresenter.FILTER_CURRENT_ROLE_PARAM_NAME );

        presenter.filterGrid( currentRole, states );

    }

    public void applyFilterOnPresenter( String key ) {
        initSelectionModel();
        applyFilterOnPresenter( filterPagedTable.getMultiGridPreferencesStore().getGridSettings( key ) );
    }

}
