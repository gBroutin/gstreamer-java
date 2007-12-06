/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package org.gstreamer;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.gstreamer.event.ElementEvent;
import org.gstreamer.event.ElementListener;
import org.gstreamer.event.HandoffEvent;
import org.gstreamer.event.HandoffListener;
import org.gstreamer.lowlevel.GstAPI;
import org.gstreamer.lowlevel.GstAPI.GstCallback;
import static org.gstreamer.lowlevel.GObjectAPI.gobj;
import static org.gstreamer.lowlevel.GstAPI.gst;


/**
 *
 */
public class Element extends GstObject {
    private static Logger logger = Logger.getLogger(Element.class.getName());
    
    /** Creates a new instance of GstElement */
    protected Element(Pointer ptr) {
        super(ptr);
    }
    protected Element(String factoryName, String elementName) {
        this(ElementFactory.makeRawElement(factoryName, elementName));
    }
    protected Element(Pointer ptr, boolean needRef) {
        super(ptr, needRef);
    }
    protected Element(Pointer ptr, boolean needRef, boolean ownsHandle) {
        super(ptr, needRef, ownsHandle);
    }
    
    public boolean link(Element e) {
        return gst.gst_element_link(this, e);
    }
    public void link(Element... elems) {
        Element prev = this;
        for (Element e : elems) {
            prev.link(e);
            prev = e;
        }
    }
    public void unlink(Element e) {
        gst.gst_element_unlink(this, e);
    }
    public void play() {
        setState(State.PLAYING);
    }
    public void pause() {
        setState(State.PAUSED);
    }
    public void stop() {
        setState(State.NULL);
    }
    public StateChangeReturn setState(State state) {
        return gst.gst_element_set_state(this, state);
    }
    public void setCaps(Caps caps) {
        gobj.g_object_set(this, "caps", caps);
    }
    public Pad getPad(String padname) {
        return gst.gst_element_get_pad(this, padname);
    }
    public boolean addPad(Pad pad) {
        return gst.gst_element_add_pad(this, pad);
    }
    public boolean removePad(Pad pad) {
        return gst.gst_element_remove_pad(this, pad);
    }
    public boolean isPlaying() {
        return getState() == State.PLAYING;
    }
    public State getState() {
        return getState(-1);
    }
    public State getState(long timeout) {
        IntByReference state = new IntByReference();
        IntByReference pending = new IntByReference();
        
        gst.gst_element_get_state(this, state, pending, timeout);
        return State.valueOf(state.getValue());
    }
    public void getState(long timeout, State[] states) {
        IntByReference state = new IntByReference();
        IntByReference pending = new IntByReference();
        
        gst.gst_element_get_state(this, state, pending, timeout);
        states[0] = State.valueOf(state.getValue());
        states[1] = State.valueOf(pending.getValue());
    }
    public void setPosition(Time t) {
        setPosition(t.longValue(), Format.TIME);
    }
    public void setPosition(long pos, Format format) {
        gst.gst_element_seek(this, 1.0, format,
                SeekFlags.FLUSH.intValue() | SeekFlags.KEY_UNIT.intValue(), 
                SeekType.SET, pos, SeekType.NONE, -1);
    }
    public long getPosition(Format format) {
        IntByReference fmt = new IntByReference(format.intValue());
        LongByReference pos = new LongByReference();
        gst.gst_element_query_position(this, fmt, pos);
        return pos.getValue();
    }
    public Time getPosition() {
        return new Time(getPosition(Format.TIME));
    }
    public Time getDuration() {
        IntByReference fmt = new IntByReference(Format.TIME.intValue());
        LongByReference duration= new LongByReference();
        gst.gst_element_query_duration(this, fmt, duration);
        return new Time(duration.getValue());
    }
    public ElementFactory getFactory() {
        return gst.gst_element_get_factory(this);
    }
    public Bus getBus() {
        return gst.gst_element_get_bus(this);
    }
    public boolean sendEvent(Event ev) {
        ev.ref(); // send_event takes ownership, so need a ref here to keep using it
        return gst.gst_element_send_event(this, ev);
    }
    public void addElementListener(ElementListener listener) {
        listenerMap.put(listener, new ElementListenerProxy(listener));
    }
    public void removeElementListener(ElementListener listener) {
        ElementListenerProxy proxy = listenerMap.get(listener);
        if (proxy != null) {
            proxy.disconnect();
            listenerMap.remove(listener);
        }
    }
    public static interface PADADDED {
        public void padAdded(Element element, Pad pad);
    }
    public static interface PADREMOVED {
        public void padRemoved(Element element, Pad pad);
    }
    public static interface NOMOREPADS {
        public void noMorePads(Element element);
    }
    public static interface HANDOFF {
        public void handoff(Element element, Buffer buffer, Pad pad);
    }
    public static interface NEWDECODEDPAD {
        public void newDecodedPad(Element element, Pad pad, boolean last);
    }
    public void connect(final PADADDED listener) {
        connect("pad-added", PADADDED.class, listener, new GstCallback() {
            @SuppressWarnings("unused")
            public void callback(Element elem, Pad pad, Pointer user_data) {
                listener.padAdded(elem, pad);
            }
        });
    }
    public void disconnect(PADADDED listener) {
        disconnect(PADADDED.class, listener);
    }
    
    public void connect(final PADREMOVED listener) {
        connect("pad-removed", PADREMOVED.class, listener,new GstCallback() {
            @SuppressWarnings("unused")
            public void callback(Element elem, Pad pad, Pointer user_data) {
                listener.padRemoved(elem, pad);
            }
        });
    }
    public void disconnect(PADREMOVED listener) {
        disconnect(PADREMOVED.class, listener);
    }
    
    public void connect(final NOMOREPADS listener) {
        connect("no-more-pads", NOMOREPADS.class, listener, new GstCallback() {
            @SuppressWarnings("unused")
            public void callback(Element elem, Pointer user_data) {
                listener.noMorePads(elem);
            }
        });
    }
    public void disconnect(NOMOREPADS listener) {
        disconnect(NOMOREPADS.class, listener);
    }
    public void connect(final NEWDECODEDPAD listener) {
        connect("new-decoded-pad", NEWDECODEDPAD.class, listener, new GstCallback() {
            @SuppressWarnings("unused")
            public void callback(Pointer elem, Pointer pad, boolean last) {
                listener.newDecodedPad(Element.this, Pad.objectFor(pad, true), last);
            }
        });
    }
    public void disconnect(NEWDECODEDPAD listener) {
        disconnect(NEWDECODEDPAD.class, listener);
    }
    public void connect(final HANDOFF listener) {
        connect("handoff", HANDOFF.class, listener, new GstAPI.HandoffCallback() {
            public void callback(Element src, Buffer buffer, Pad pad, Pointer user_data) {
                buffer.struct.read();
                listener.handoff(src, buffer, pad);
            }            
        });
    }
    public void disconnect(HANDOFF listener) {
        disconnect(HANDOFF.class, listener);
    }
    
    public void addHandoffListener(final HandoffListener listener) {
        HANDOFF handoff = new HANDOFF() {
            public void handoff(Element elem, Buffer buffer, Pad pad) {
                listener.handoff(new HandoffEvent(elem, buffer, pad));
            }
        };
        handoffMap.put(listener, handoff);
        connect(handoff);
    }
    public void removeHandoffListener(HandoffListener listener) {
        disconnect(handoffMap.get(listener));
    }
    
    class ElementListenerProxy {
        public ElementListenerProxy(final ElementListener listener) {
            Element.this.connect(added = new PADADDED() {
                public void padAdded(Element elem, Pad pad) {
                    listener.padAdded(new ElementEvent(elem, pad));
                }
            });
            Element.this.connect(removed = new PADREMOVED() {
                public void padRemoved(Element elem, Pad pad) {
                    listener.padRemoved(new ElementEvent(elem, pad));
                }
            });
            Element.this.connect(nomorepads = new NOMOREPADS() {
                public void noMorePads(Element elem) {
                    listener.noMorePads(new ElementEvent(elem, null));
                }
            });
        }
        public void disconnect() {
            Element.this.disconnect(nomorepads);
            Element.this.disconnect(removed);
            Element.this.disconnect(added);
        }
        private PADADDED added;
        private PADREMOVED removed;
        private NOMOREPADS nomorepads;
    }
    
    /**
     * Link together a list of elements.
     * 
     * @param elements The list of elements to link together
     * 
     * @return true if all elements successfully linked.
     */
    public static boolean linkMany(Element... elements) {
        return gst.gst_element_link_many(elements);
    }
    
    /**
     * Unlink a list of elements.
     * 
     * @param elements The list of elements to link together
     * 
     */
    public static void unlinkMany(Element... elements) {
        gst.gst_element_unlink_many(elements);
    }
    
    /**
     * Link together source and destination pads of two elements.
     * 
     * @param src The {@link Element} containing the source {@link Pad}.
     * @param srcPadName The name of the source {@link Pad}.  Can be null for any pad.
     * @param dest The {@link Element} containing the destination {@link Pad}.
     * @param destPadName The name of the destination {@link Pad}.  Can be null for any pad.
     * 
     * @return true if the pads were successfully linked.
     */
    public static boolean linkPads(Element src, String srcPadName, Element dest, String destPadName) {
        return gst.gst_element_link_pads(src, srcPadName, dest, destPadName);
    }
    
    /**
     * Link together source and destination pads of two elements.
     * 
     * @param src The {@link Element} containing the source {@link Pad}.
     * @param srcPadName The name of the source {@link Pad}.  Can be null for any pad.
     * @param dest The {@link Element} containing the destination {@link Pad}.
     * @param destPadName The name of the destination {@link Pad}.  Can be null for any pad.
     * @param caps The {@link Caps} to use to filter the link.
     * 
     * @return true if the pads were successfully linked.
     */
    public static boolean linkPadsFiltered(Element src, String srcPadName, 
            Element dest, String destPadName, Caps caps) {
        return gst.gst_element_link_pads_filtered(src, srcPadName, dest, destPadName, caps);
    }
    
    /**
     * Unlink source and destination pads of two elements.
     * 
     * @param src The {@link Element} containing the source {@link Pad}.
     * @param srcPadName The name of the source {@link Pad}.
     * @param dest The {@link Element} containing the destination {@link Pad}.
     * @param destPadName The name of the destination {@link Pad}.
     * 
     */
    public static void unlinkPads(Element src, String srcPadName, Element dest, String destPadName) {
        gst.gst_element_unlink_pads(src, srcPadName, dest, destPadName);
    }
    
    static Element objectFor(Pointer ptr, boolean needRef) {
        return GstObject.objectFor(ptr, Element.class, needRef);
    }
    
    private Map<HandoffListener, HANDOFF> handoffMap =
            new ConcurrentHashMap<HandoffListener, HANDOFF>();
    private Map<ElementListener, ElementListenerProxy> listenerMap =
            new ConcurrentHashMap<ElementListener, ElementListenerProxy>();
}

