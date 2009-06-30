/* 
 * Copyright (c) 2007, 2008 Wayne Meissner
 * 
 * This file is part of gstreamer-java.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gstreamer.lowlevel;

import org.gstreamer.Clock;
import org.gstreamer.ClockID;
import org.gstreamer.ClockReturn;
import org.gstreamer.ClockTime;
import org.gstreamer.lowlevel.annotations.CallerOwnsReturn;

import com.sun.jna.Pointer;


/**
 * GstClock functions
 */
public interface GstClockAPI extends com.sun.jna.Library {
    static GstClockAPI INSTANCE = GstNative.load(GstClockAPI.class);
    
    GType gst_clock_get_type();
    ClockTime gst_clock_set_resolution(Clock clock, ClockTime resolution);
    ClockTime gst_clock_get_resolution(Clock clock);
    ClockTime gst_clock_get_time(Clock clock);
    void gst_clock_set_calibration(Clock clock, ClockTime internal, ClockTime external, ClockTime rate_num, ClockTime rate_denom);
    void gst_clock_get_calibration(Clock clock, long[] internal, long[] external,
            long[] rate_num, long[] rate_denom);
    /* master/slave clocks */
    boolean gst_clock_set_master(Clock clock, Clock master);
    @CallerOwnsReturn Clock gst_clock_get_master(Clock clock);
    boolean gst_clock_add_observation(Clock clock, ClockTime slave, ClockTime Master, double[] r_squared);
    
    /* getting and adjusting internal time */
    ClockTime gst_clock_get_internal_time(Clock clock);
    ClockTime gst_clock_adjust_unlocked(Clock clock, ClockTime internal);
    ClockTime gst_clock_unadjust_unlocked(Clock clock, ClockTime external);


    /* creating IDs that can be used to get notifications */
    @CallerOwnsReturn ClockID gst_clock_new_single_shot_id(Clock clock, ClockTime time);
    @CallerOwnsReturn ClockID gst_clock_new_periodic_id(Clock clock, ClockTime start_time, ClockTime interval);

    
    /* reference counting */
    void gst_clock_id_ref(ClockID id);
    void gst_clock_id_unref(ClockID id);
    void gst_clock_id_unref(Pointer id);

    /* operations on IDs */
    int gst_clock_id_compare_func(ClockID id1, ClockID id2);

    ClockTime gst_clock_id_get_time(ClockID id);
    ClockReturn gst_clock_id_wait(ClockID id, /* GstClockTimeDiff * */ long[] jitter);
    public static interface GstClockCallback {
        /**
         * @param clock The clock that triggered the callback
         * @param time The time it was triggered
         * @param id The {@link ClockID} that expired
         * @param user_data user data passed in the gst_clock_id_wait_async() function
         * @return currently unused.
         */
        boolean callback(Clock clock, ClockTime time, ClockID id, Pointer user_data);
    }

    ClockReturn gst_clock_id_wait_async(ClockID id, GstClockCallback func, Pointer user_data);
    void gst_clock_id_unschedule(ClockID id);

}
