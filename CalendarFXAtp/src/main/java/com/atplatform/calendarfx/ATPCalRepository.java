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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;

import com.atplatform.calendarfx.model.ATPCalendar;
import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;

/**
 * Created by gdiaz on 7/01/2017.
 */
public final class ATPCalRepository {

    private static final String CALENDARS_DIR = "/.store/atpics/";


    public static CalendarSource defaultCalendars = new CalendarSource("Default");


    public static DoubleProperty workDoneProperty = new SimpleDoubleProperty();

    public static DoubleProperty totalWorkProperty = new SimpleDoubleProperty();

    public static StringProperty messageProperty = new SimpleStringProperty();

    public static void loadSources() throws IOException, ParserException {
		File folder = new File(System.getProperty("user.home") + CALENDARS_DIR);
		File[] files = folder.listFiles();
		Arrays.sort(files, new Comparator<File>() {

			@Override
			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		totalWorkProperty.set(files.length);
		
		for(int i = 0; i < files.length; ++i) {
			File file = files[i];
			String name = file.getName();
			
			workDoneProperty.set(i+1);
			messageProperty.set("Calendar: " + name);
			
			CalendarBuilder builder = new CalendarBuilder();
            InputStream inputStream = new FileInputStream(file);

            net.fortuna.ical4j.model.Calendar calendar = builder.build(inputStream);

            final ATPCalendar cal = new ATPCalendar(name, calendar);
            cal.setStyle(Calendar.Style.STYLE1);
            
			Platform.runLater(() -> {
				defaultCalendars.getCalendars().add(cal);
			});
		}
    }

	public static CalendarSource getDefaultCalendarSource() {
		return defaultCalendars;
	}

}
