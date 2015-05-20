/*
 * Copyright 2012 JBoss Inc
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
package org.jbpm.console.ng.ht.client.editors.taskslist.grid.test;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import org.dashbuilder.dataset.DataSet;
import org.dashbuilder.dataset.client.DataSetClientServiceError;
import org.dashbuilder.dataset.client.DataSetReadyCallback;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jbpm.console.ng.ga.model.PortableQueryFilter;
import org.jbpm.console.ng.gc.client.displayer.TableSettings;
import org.jbpm.console.ng.gc.client.displayer.TableSettingsBuilder;
import org.jbpm.console.ng.gc.client.list.base.AbstractListView.ListView;
import org.jbpm.console.ng.gc.client.list.base.AbstractScreenListPresenter;
import org.jbpm.console.ng.gc.client.util.TaskUtils;
import org.jbpm.console.ng.gc.client.util.TaskUtils.TaskType;
import org.jbpm.console.ng.ht.client.editors.taskslist.grid.TaskListTableDisplayer;
import org.jbpm.console.ng.ht.client.i18n.Constants;
import org.jbpm.console.ng.ht.model.TaskSummary;
import org.jbpm.console.ng.ht.service.TaskLifeCycleService;
import org.jbpm.console.ng.ht.service.TaskQueryService;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.client.mvp.UberView;
import org.uberfire.client.workbench.widgets.common.ErrorPopupPresenter;
import org.uberfire.paging.PageResponse;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.dashbuilder.dataset.sort.SortOrder.DESCENDING;
import static org.jbpm.console.ng.ht.util.TaskRoleDefinition.TASK_ROLE_ADMINISTRATOR;
import static org.jbpm.console.ng.ht.util.TaskRoleDefinition.TASK_ROLE_POTENTIALOWNER;

@Dependent
@WorkbenchScreen(identifier = "Tasks List DataSet test")
public class TasksDataSetListGridPresenter extends AbstractScreenListPresenter<TaskSummary> {
  public static String FILTER_STATUSES_PARAM_NAME = "statuses";
  public static String FILTER_CURRENT_ROLE_PARAM_NAME = "filter";

  public interface TaskDataSetListView extends ListView<TaskSummary, TasksDataSetListGridPresenter> {

  }

  @Inject
  private TaskDataSetListView view;

  private Constants constants = GWT.create(Constants.class);

  @Inject
  private ErrorPopupPresenter errorPopup;

  @Inject
  private Caller<TaskQueryService> taskQueryService;

  @Inject
  private Caller<TaskLifeCycleService> taskOperationsService;

  private String currentRole;

  private List<String> currentStatuses;

  private TableSettings currentTableSetting;

  private TaskListTableDisplayer taskListTableDisplayer= new TaskListTableDisplayer();

  private TaskType currentStatusFilter = TaskType.ACTIVE;

  public TasksDataSetListGridPresenter() {
    dataProvider = new AsyncDataProvider<TaskSummary>() {

      @Override
      protected void onRangeChanged(HasData<TaskSummary> display) {
        view.showBusyIndicator(constants.Loading());
        final Range visibleRange = view.getListGrid().getVisibleRange();
        ColumnSortList columnSortList = view.getListGrid().getColumnSortList();
        try {
          if(currentTableSetting==null) {
            currentTableSetting= taskListTableDisplayer.createTableSettingsPrototype();
          }
          currentTableSetting.setTablePageSize( view.getListGrid().getPageSize() );

          taskListTableDisplayer.setDisplayerSettings( currentTableSetting );
          taskListTableDisplayer.init( currentTableSetting );

          taskListTableDisplayer.lookupDataSet(visibleRange.getStart(), new DataSetReadyCallback() {
            @Override
            public void callback(DataSet dataSet) {
              if (dataSet != null && dataSet.getRowCount() > 0) {
                List<TaskSummary> myTasksFromDataSet = new ArrayList<TaskSummary>();

                for (int i = 0;i <  dataSet.getRowCount(); i ++) {
                  myTasksFromDataSet.add( new TaskSummary(
                                  (Long)dataSet.getColumnByIndex(0).getValues().get(i),
                                  (String) dataSet.getColumnByIndex(1).getValues().get(i),
                                  (String) dataSet.getColumnByIndex(5).getValues().get(i),
                                  (String) dataSet.getColumnByIndex(4).getValues().get(i),
                                  0,(String) dataSet.getColumnByIndex(2).getValues().get(i),
                                  "",(Date ) dataSet.getColumnByIndex(3).getValues().get(i),null,null,"",-1,-1,"",-1)
                  );
                }

                dataProvider.updateRowCount(dataSet.getRowCount(),
                        true); // true ??
                dataProvider.updateRowData(0,///dataSet.getStartRowIndex() ???
                        myTasksFromDataSet);
              }
              view.hideBusyIndicator();
            }
            @Override
            public void notFound() {
              GWT.log("DataSet with UUID [  jbpmHumanTasks ] not found.");
            }

            @Override
            public boolean onError(DataSetClientServiceError error) {
              view.hideBusyIndicator();
              GWT.log("DataSet with UUID [  jbpmHumanTasks ] error: ", error.getThrowable());
              return false;
            }
          }  );



        } catch (Exception e) {
          GWT.log("Error looking up dataset with UUID [ jbpmHumanTasks ]");
        }

      }
    };
  }

  public void filterGrid(TableSettings tableSettings) {
    this.currentTableSetting = tableSettings;
    refreshGrid();

  }
  public void filterGrid(String currentRole, List<String> currentStatuses) {
    this.currentRole = currentRole;
    this.currentStatuses= currentStatuses;
    refreshGrid();

  }

  public void refreshActiveTasks() {
    currentRole = TASK_ROLE_POTENTIALOWNER;
    currentStatusFilter = TaskType.ACTIVE;
    refreshGrid();
  }

  public void refreshPersonalTasks() {
    currentRole = TASK_ROLE_POTENTIALOWNER;
    currentStatusFilter = TaskType.PERSONAL;
    refreshGrid();
  }

  public void refreshGroupTasks() {
    currentRole = TASK_ROLE_POTENTIALOWNER;
    currentStatusFilter = TaskType.GROUP;
    refreshGrid();
  }

  public void refreshAllTasks() {
    currentRole = TASK_ROLE_POTENTIALOWNER;
    currentStatusFilter = TaskType.ALL;
    refreshGrid();
  }

  public void refreshAdminTasks() {
    currentRole = TASK_ROLE_ADMINISTRATOR;
    currentStatusFilter = TaskType.ADMIN;
    refreshGrid();
  }

  @WorkbenchPartTitle
  public String getTitle() {
    return constants.Tasks_List();
  }

  @WorkbenchPartView
  public UberView<TasksDataSetListGridPresenter> getView() {
    return view;
  }

  public void releaseTask(final Long taskId, final String userId) {
    taskOperationsService.call(new RemoteCallback<Void>() {
      @Override
      public void callback(Void nothing) {
        view.displayNotification("Task Released");
        refreshGrid();
      }
    }, new ErrorCallback<Message>() {
      @Override
      public boolean error(Message message, Throwable throwable) {
        errorPopup.showMessage("Unexpected error encountered : " + throwable.getMessage());
        return true;
      }
    }).release(taskId, userId);
  }

  public void claimTask(final Long taskId, final String userId, final String deploymentId) {
    taskOperationsService.call(new RemoteCallback<Void>() {
      @Override
      public void callback(Void nothing) {
        view.displayNotification("Task Claimed");
        refreshGrid();
      }
    }, new ErrorCallback<Message>() {
      @Override
      public boolean error(Message message, Throwable throwable) {
        errorPopup.showMessage("Unexpected error encountered : " + throwable.getMessage());
        return true;
      }
    }).claim(taskId, userId, deploymentId);
  }
}