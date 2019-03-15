/*
 *  Copyright (C) 2017 Dirk Lemmermann Software & Consulting (dlsc.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.atplatform.calendarfx;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.controlsfx.dialog.ProgressDialog;

import com.atplatform.calendarfx.model.ATPCalendar;
import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.model.LoadEvent;
import com.calendarfx.util.LoggingDomain;
import com.calendarfx.view.CalendarView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ATPCalendarApp extends Application {

    private static final ExecutorService executor = Executors
            .newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "ICalCalendar Load Thread");
                thread.setDaemon(true);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            });

    @Override
    public void start(Stage primaryStage) throws Exception {
        LoggingDomain.CONFIG.info("Java version: " + System.getProperty("java.version"));

        System.setProperty("ical4j.unfolding.relaxed", "true");
        System.setProperty("ical4j.parsing.relaxed", "true");

        CalendarView calendarView = new CalendarView();
        calendarView.setToday(LocalDate.now());
        calendarView.setTime(LocalTime.now());
        calendarView.addEventFilter(LoadEvent.LOAD, evt -> {

            /*
             * Run in background thread. We do not want to block the UI.
             */
            executor.submit(() -> {
                for (CalendarSource source : evt.getCalendarSources()) {
                    for (Calendar calendar : source.getCalendars()) {
                        if (calendar instanceof ATPCalendar) {
                            ATPCalendar account = (ATPCalendar) calendar;
                            account.load(evt);
                        }
                    }
                }
            });
        });

        Thread updateTimeThread = new Thread("Calendar: Update Time Thread") {
            @Override
            public void run() {
                while (true) {
                    Platform.runLater(() -> {
                        calendarView.setToday(LocalDate.now());
                        calendarView.setTime(LocalTime.now());
                    });

                    try {
                        // update every 10 seconds
                        sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        updateTimeThread.setPriority(Thread.MIN_PRIORITY);
        updateTimeThread.setDaemon(true);
        updateTimeThread.start();

        calendarView.setRequestedTime(LocalTime.now());
        calendarView.setTraysAnimated(false);
        calendarView.getCalendarSources().setAll(ATPCalRepository.defaultCalendars);

        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                ATPCalRepository.workDoneProperty.addListener(it -> updateProgress(ATPCalRepository.workDoneProperty.get(), ATPCalRepository.totalWorkProperty.get()));
                ATPCalRepository.totalWorkProperty.addListener(it -> updateProgress(ATPCalRepository.workDoneProperty.get(), ATPCalRepository.totalWorkProperty.get()));
                ATPCalRepository.messageProperty.addListener(it -> updateMessage(ATPCalRepository.messageProperty.get()));
                ATPCalRepository.loadSources();
                return null;
            }
        };

        ImageView logo = new ImageView(ATPCalendarApp.class.getResource("ical.png").toExternalForm());
        logo.setFitWidth(64);
        logo.setPreserveRatio(true);

        ProgressDialog progressDialog = new ProgressDialog(task);
        progressDialog.setGraphic(logo);
        progressDialog.initModality(Modality.NONE);
        progressDialog.initStyle(StageStyle.UTILITY);
        progressDialog.initOwner(primaryStage.getOwner());
        progressDialog.setTitle("Progress");
        progressDialog.setHeaderText("Importing Local Calendars");
        progressDialog.setContentText("The application is now downloading calendars.");
        progressDialog.getDialogPane().setPrefWidth(500);
        progressDialog.getDialogPane().getStylesheets().clear();
        progressDialog.getDialogPane().getStylesheets().add(ATPCalendarApp.class.getResource("dialog.css").toExternalForm());

        executor.submit(task);

        Scene scene = new Scene(calendarView);

        primaryStage.setTitle("iCalendar");
        primaryStage.setScene(scene);
        primaryStage.setWidth(1400);
        primaryStage.setHeight(1000);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
