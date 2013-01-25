/*
 * Copyright 2011 JBoss Inc 
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
package org.jbpm.console.ng.ht.client.editors.taskform;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.jboss.errai.bus.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.api.Caller;
import org.jbpm.console.ng.bd.service.KnowledgeDomainServiceEntryPoint;
import org.jbpm.console.ng.bd.service.StatefulKnowledgeSessionEntryPoint;

import org.jbpm.console.ng.ht.model.TaskSummary;
import org.jbpm.console.ng.ht.model.fb.events.FormRenderedEvent;
import org.jbpm.console.ng.ht.service.FormServiceEntryPoint;
import org.jbpm.console.ng.ht.service.TaskServiceEntryPoint;
import org.jbpm.console.ng.pr.model.ProcessSummary;
import org.jbpm.console.ng.pr.model.events.ProcessInstanceCreated;
import org.uberfire.client.annotations.OnReveal;
import org.uberfire.client.annotations.OnStart;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchPopup;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.mvp.UberView;
import org.uberfire.client.workbench.widgets.events.BeforeClosePlaceEvent;
import org.uberfire.security.Identity;
import org.uberfire.shared.mvp.PlaceRequest;
import org.uberfire.shared.mvp.impl.DefaultPlaceRequest;

@Dependent
@WorkbenchPopup(identifier = "Form Display")
public class FormDisplayAlternativePopupPresenter {

    @Inject
    private FormDisplayView                          view;
    @Inject
    private Caller<FormServiceEntryPoint>            formServices;
    @Inject
    private Caller<KnowledgeDomainServiceEntryPoint> domainServices;
    @Inject
    Caller<StatefulKnowledgeSessionEntryPoint> ksessionServices;
    @Inject
    private Caller<TaskServiceEntryPoint> taskServices;
    @Inject
    private Event<FormRenderedEvent>      formRendered;
    @Inject
    Event<ProcessInstanceCreated> processInstanceCreatedEvents;
    @Inject
    private Identity                     identity;
    @Inject
    private PlaceManager                 placeManager;
    @Inject
    private Event<BeforeClosePlaceEvent> closePlaceEvent;
    private PlaceRequest                 place;

    public interface FormDisplayView
            extends
            UberView<FormDisplayAlternativePopupPresenter> {

        void displayNotification( String text );

        long getTaskId();

        void setTaskId( long taskId );

        String getProcessId();

        void setProcessId( String processId );

        VerticalPanel getFormView();

        Label getNameText();

        Label getTaskIdText();

        FlowPanel getOptionsDiv();

    }

    @PostConstruct
    public void init() {
        publish( this );
        publishGetFormValues();

    }

    @OnStart
    public void onStart( final PlaceRequest place ) {
        this.place = place;
    }

    public void renderTaskForm( final long taskId ) {

        formServices.call( new RemoteCallback<String>() {
            @Override
            public void callback( String form ) {
                view.getFormView().clear();
                view.getFormView().add( new HTMLPanel( form ) );
                taskServices.call( new RemoteCallback<TaskSummary>() {
                    @Override
                    public void callback( final TaskSummary task ) {
                        view.getOptionsDiv().clear();
                        view.getNameText().setText( task.getName() );
                        view.getTaskIdText().setText( String.valueOf( task.getId() ) );
                        if ( task.getStatus().equals( "Reserved" ) ) {
                            FocusPanel startFlowPanel = new FocusPanel();
                            startFlowPanel.setStyleName( "start" );
                            startFlowPanel.addClickHandler( new ClickHandler() {

                                public native void onClick( ClickEvent event )/*-{
                                    $wnd.startTask($wnd.getFormValues($doc.getElementById("form-data")));
                                }-*/;
                            } );
                            view.getOptionsDiv().add( startFlowPanel );
                        } else if ( task.getStatus().equals( "InProgress" ) ) {
                            FocusPanel saveTaskFlowPanel = new FocusPanel();
                            saveTaskFlowPanel.setStyleName( "save" );
                            saveTaskFlowPanel.addClickHandler( new ClickHandler() {

                                public native void onClick( ClickEvent event )/*-{
                                    $wnd.saveTaskState($wnd.getFormValues($doc.getElementById("form-data")));
                                }-*/;
                            } );
                            view.getOptionsDiv().add( saveTaskFlowPanel );
                            FocusPanel completeTaskFlowPanel = new FocusPanel();
                            completeTaskFlowPanel.setStyleName( "complete" );
                            completeTaskFlowPanel.addClickHandler( new ClickHandler() {

                                public native void onClick( ClickEvent event )/*-{
                                    $wnd.completeTask($wnd.getFormValues($doc.getElementById("form-data")));
                                }-*/;
                            } );
                            view.getOptionsDiv().add( completeTaskFlowPanel );

                        }
                    }
                } ).getTaskDetails( taskId );
            }
        } ).getFormDisplayTask( taskId );

    }

    public void renderProcessForm( final String processId ) {

        formServices.call( new RemoteCallback<String>() {
            @Override
            public void callback( String form ) {
                view.getFormView().clear();
                view.getFormView().add( new HTMLPanel( form ) );

                domainServices.call( new RemoteCallback<ProcessSummary>() {
                    @Override
                    public void callback( ProcessSummary summary ) {
                        view.getNameText().setText( summary.getName() );
                        view.getTaskIdText().setText( String.valueOf( summary.getId() ) );
                        FocusPanel startFlowPanel = new FocusPanel();
                            startFlowPanel.setStyleName( "start" );
                            startFlowPanel.addClickHandler( new ClickHandler() {

                                public native void onClick( ClickEvent event )/*-{
                                    $wnd.startProcess($wnd.getFormValues($doc.getElementById("form-data")));
                                }-*/;
                            } );
                            view.getOptionsDiv().add( startFlowPanel );

                    }
                } ).getProcessDesc( processId );

            }
        } ).getFormDisplayProcess( processId );

    }

    @WorkbenchPartTitle
    public String getTitle() {
        return "Form Display";
    }

    @WorkbenchPartView
    public UberView<FormDisplayAlternativePopupPresenter> getView() {
        return view;
    }

    public void completeTask( String values ) {
        final Map<String, String> params = getUrlParameters( values );
        final Map<String, Object> objParams = new HashMap<String, Object>( params );
        taskServices.call( new RemoteCallback<Void>() {
            @Override
            public void callback( Void nothing ) {
                view.displayNotification( "Form for Task Id: " + params.get( "taskId" ) + " was completed!" );
                close();
            }
        } ).complete( Long.parseLong( params.get( "taskId" ) ), identity.getName(), objParams );

    }

    public void saveTaskState( final Long taskId,
                               final Map<String, String> values ) {
        taskServices.call( new RemoteCallback<Long>() {
            @Override
            public void callback( Long contentId ) {
                view.displayNotification( "Task Id: " + taskId + " State was Saved! ContentId : " + contentId );
                renderTaskForm( taskId );
            }
        } ).saveContent( taskId, values );
    }

    public void saveTaskState( String values ) {
        final Map<String, String> params = getUrlParameters( values );
        taskServices.call( new RemoteCallback<Long>() {
            @Override
            public void callback( Long contentId ) {
                view.displayNotification( "Task Id: " + params.get( "taskId" ) + " State was Saved! ContentId : " + contentId );
                renderTaskForm( Long.parseLong( params.get( "taskId" ).toString() ) );
            }
        } ).saveContent( Long.parseLong( params.get( "taskId" ).toString() ), params );

    }

    public void startTask( final Long taskId,
                           final String identity ) {
        taskServices.call( new RemoteCallback<Void>() {
            @Override
            public void callback( Void nothing ) {
                view.displayNotification( "Task Id: " + taskId + " was started!" );
                renderTaskForm( taskId );
            }
        } ).start( taskId, identity );
    }

    public void startTask( String values ) {
        final Map<String, String> params = getUrlParameters( values );
        taskServices.call( new RemoteCallback<Void>() {
            @Override
            public void callback( Void nothing ) {
                view.displayNotification( "Task Id: " + params.get( "taskId" ) + " was started!" );
                renderTaskForm( Long.parseLong( params.get( "taskId" ).toString() ) );
            }
        } ).start( Long.parseLong( params.get( "taskId" ).toString() ), identity.getName() );

    }

    public void startProcess( String values ) {
        final Map<String, String> params = getUrlParameters( values );

        ksessionServices.call( new RemoteCallback<Long>() {
            @Override
            public void callback( Long processId ) {
                view.displayNotification( "Process Id: " + processId + " started!" );
                processInstanceCreatedEvents.fire( new ProcessInstanceCreated() );
                close();
                PlaceRequest placeRequestImpl = new DefaultPlaceRequest( "Process Definition Details Perspective" );
                placeRequestImpl.addParameter( "processId", params.get( "processId" ).toString() );
                placeManager.goTo( placeRequestImpl );
            }
        } ).startProcess( params.get( "processId" ).toString(), params );

    }

    // Set up the JS-callable signature as a global JS function.
    private native void publish( FormDisplayAlternativePopupPresenter fdp )/*-{

        $wnd.completeTask = function (from) {
            fdp.@org.jbpm.console.ng.ht.client.editors.taskform.FormDisplayAlternativePopupPresenter::completeTask(Ljava/lang/String;)(from);
        }

        $wnd.startTask = function (from) {
            fdp.@org.jbpm.console.ng.ht.client.editors.taskform.FormDisplayAlternativePopupPresenter::startTask(Ljava/lang/String;)(from);
        }

        $wnd.saveTaskState = function (from) {
            fdp.@org.jbpm.console.ng.ht.client.editors.taskform.FormDisplayAlternativePopupPresenter::saveTaskState(Ljava/lang/String;)(from);
        }

        $wnd.startProcess = function (from) {
            fdp.@org.jbpm.console.ng.ht.client.editors.taskform.FormDisplayAlternativePopupPresenter::startProcess(Ljava/lang/String;)(from);
        }

    }-*/;

    private native void publishGetFormValues() /*-{
        $wnd.getFormValues = function (form) {
            var params = '';

            for (i = 0; i < form.elements.length; i++) {
                var fieldName = form.elements[i].name;
                var fieldValue = form.elements[i].value;
                if (fieldName != '') {
                    params += fieldName + '=' + fieldValue + '&';
                }
            }

            return params;
        };

    }-*/;

    public static Map<String, String> getUrlParameters( String values ) {
        Map<String, String> params = new HashMap<String, String>();
        for ( String param : values.split( "&" ) ) {
            String pair[] = param.split( "=" );
            String key = pair[ 0 ];
            String value = "";
            if ( pair.length > 1 ) {
                value = pair[ 1 ];
            }
            if ( !key.startsWith( "btn_" ) ) {
                params.put( key, value );
            }
        }

        return params;
    }

    @OnReveal
    public void onReveal() {
        long taskId = Long.parseLong( place.getParameter( "taskId", "-1" ).toString() );
        String processId = place.getParameter( "processId", "none" ).toString();
        if ( taskId != -1 ) {
            view.setTaskId( taskId );
            renderTaskForm( taskId );
        } else if ( !processId.equals( "none" ) ) {
            view.setProcessId( processId );
            renderProcessForm( processId );
        }
    }

    public void close() {
        closePlaceEvent.fire( new BeforeClosePlaceEvent( this.place ) );
    }
}