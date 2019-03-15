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

package com.atplatform.calendarfx.model;

import com.calendarfx.model.Interval;
import com.calendarfx.model.LoadEvent;
import net.fortuna.ical4j.filter.Filter;
import net.fortuna.ical4j.filter.PeriodRule;
import net.fortuna.ical4j.filter.Rule;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Uid;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class ATPCalendar extends com.calendarfx.model.Calendar {

    private Set<Integer> alreadyLoadedYears = new HashSet<>();

    private Calendar calendar;

    private final Set<Uid> loadedEventIds = new HashSet<>();

    public ATPCalendar(String name, Calendar calendar) {
        super(name);

        requireNonNull(calendar);

        this.calendar = calendar;

        load(Year.now().getValue());
    }

    public void load(LoadEvent event) {
        load(event.getStartTime().getYear());
    }

    /*
     * Use synchronization to ensure consistency of already loaded dates.
     */
    private synchronized void load(int year) {
        if (alreadyLoadedYears.contains(year)) {
            return;
        }

        alreadyLoadedYears.add(year);

        ZonedDateTime st = ZonedDateTime.of(LocalDate.now().with(TemporalAdjusters.firstDayOfYear()), LocalTime.MIN, ZoneId.systemDefault());
        ZonedDateTime et = ZonedDateTime.of(LocalDate.now().with(TemporalAdjusters.lastDayOfYear()), LocalTime.MAX, ZoneId.systemDefault());

        try {
            startBatchUpdates();

            /*
             * Convert start and end times from new java.time API to old API and then to ical API :-)
             */
            Period period = new Period(
                    new DateTime(Date.from(st.toInstant())),
                    new DateTime(Date.from(et.toInstant())));

            Rule[] rules = new Rule[]{new PeriodRule(period)};
            Filter filter = new Filter(rules, Filter.MATCH_ANY);

            @SuppressWarnings("unchecked")
            Collection<VEvent> events = filter.filter(calendar.getComponents(Component.VEVENT));

            for (VEvent evt : events) {

                if (loadedEventIds.contains(evt.getUid())) {
                    continue;
                }

                loadedEventIds.add(evt.getUid());

                ATPCalendarEntry entry = new ATPCalendarEntry(evt);

                ZonedDateTime entryStart = ZonedDateTime.ofInstant(evt.getStartDate().getDate().toInstant(), ZoneId.systemDefault());

                ZonedDateTime entryEnd = ZonedDateTime.ofInstant(evt
                                .getEndDate().getDate().toInstant(),
                        ZoneId.systemDefault());

                if (entryEnd.toLocalDate().isAfter(entryStart.toLocalDate())) {
                    entryEnd = entryEnd.minusDays(1);
                    entry.setFullDay(true);
                }

                entry.setInterval(new Interval(entryStart, entryEnd));

                final Property prop = evt.getProperty("RRULE");
                if (prop instanceof RRule) {
                    RRule rrule = (RRule) prop;
                    entry.setRecurrenceRule("RRULE:" + rrule.getValue());
                }

                addEntry(entry);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            stopBatchUpdates();
        }
    }
}
