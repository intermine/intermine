package org.intermine.swing.jdk15;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Java 1.5 version of Java 6's javax.swing.SwingWorker.
 *
 * @deprecated Assuming Java 6 is acceptable, this is no longer required.
 */
@Deprecated
public abstract class SwingWorker<T, V> implements Runnable, Future<T> {

    private StateValue state;

    private volatile boolean cancelled;

    private Log logger = LogFactory.getLog(getClass());

    private Thread myThread;

    /**
     * all propertyChangeSupport goes through this.
     */
    private final PropertyChangeSupport propertyChangeSupport = new SwingWorkerPropertyChangeSupport(this);

    /**
     * Values for the {@code state} bound property.
     * @since 1.6
     */
    public enum StateValue {
        /**
         * Initial {@code SwingWorker} state.
         */
        PENDING,
        /**
         * {@code SwingWorker} is {@code STARTED}
         * before invoking {@code doInBackground}.
         */
        STARTED,

        /**
         * {@code SwingWorker} is {@code DONE}
         * after {@code doInBackground} method
         * is finished.
         */
        DONE
    };

    /**
     * Constructs this {@code SwingWorker}.
     */
    public SwingWorker() {
        state = StateValue.PENDING;
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * <p>
     * Note that this method is executed only once.
     *
     * <p>
     * Note: this method is executed in a background thread.
     *
     *
     * @return the computed result
     * @throws Exception if unable to compute a result
     *
     */
    protected abstract T doInBackground() throws Exception ;

    /**
     * Sets this {@code Future} to the result of computation unless
     * it has been cancelled.
     */
    public final void run() {
        setState(StateValue.STARTED);
        try {
            doInBackground();
        } catch (InterruptedException e) {
            logger.warn("SwingWorker interrupted.");
        } catch (Exception e) {
            logger.error("SwingWorker failed: ", e);
        } finally {
            setState(StateValue.DONE);
            doneEDT();
        }
    }

    protected final void publish(V... chunks) {
    }

    protected void process(List<V> chunks) {
    }

    /**
     * Executed on the <i>Event Dispatch Thread</i> after the {@code doInBackground}
     * method is finished. The default
     * implementation does nothing. Subclasses may override this method to
     * perform completion actions on the <i>Event Dispatch Thread</i>. Note
     * that you can query status inside the implementation of this method to
     * determine the result of this task or whether this task has been cancelled.
     *
     * @see #doInBackground
     * @see #isCancelled()
     * @see #get
     */
    protected void done() {
    }

    /**
     * Sets the {@code progress} bound property.
     * The value should be from 0 to 100.
     *
     * <p>
     * Because {@code PropertyChangeListener}s are notified asynchronously on
     * the <i>Event Dispatch Thread</i> multiple invocations to the
     * {@code setProgress} method might occur before any
     * {@code PropertyChangeListeners} are invoked. For performance purposes
     * all these invocations are coalesced into one invocation with the last
     * invocation argument only.
     *
     * <p>
     * For example, the following invokations:
     *
     * <pre>
     * setProgress(1);
     * setProgress(2);
     * setProgress(3);
     * </pre>
     *
     * might result in a single {@code PropertyChangeListener} notification with
     * the value {@code 3}.
     *
     * @param progress the progress value to set
     * @throws IllegalArgumentException is value not from 0 to 100
     */
    protected final void setProgress(int progress) {
    }

    /**
     * Schedules this {@code SwingWorker} for execution on a <i>worker</i>
     * thread. There are a number of <i>worker</i> threads available. In the
     * event all <i>worker</i> threads are busy handling other
     * {@code SwingWorkers} this {@code SwingWorker} is placed in a waiting
     * queue.
     *
     * <p>
     * Note:
     * {@code SwingWorker} is only designed to be executed once.  Executing a
     * {@code SwingWorker} more than once will not result in invoking the
     * {@code doInBackground} method twice.
     */
    public final void execute() {
        if (state == StateValue.PENDING) {
            myThread = new Thread(this, "SwingWorker");
            myThread.start();
        }
    }

    // Future methods START
    /**
     * {@inheritDoc}
     */
    public final boolean cancel(boolean mayInterruptIfRunning) {
        if (state == StateValue.STARTED) {
            if (mayInterruptIfRunning) {
                myThread.interrupt();
            }
            cancelled = true;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isCancelled() {
        return cancelled;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isDone() {
        return state == StateValue.DONE;
    }

    public T get() throws InterruptedException, ExecutionException {
        return null;
    }

    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    // PropertyChangeSupports methods START
    /**
     * Adds a {@code PropertyChangeListener} to the listener list. The listener
     * is registered for all properties. The same listener object may be added
     * more than once, and will be called as many times as it is added. If
     * {@code listener} is {@code null}, no exception is thrown and no action is taken.
     *
     * <p>
     * Note: This is merely a convenience wrapper. All work is delegated to
     * {@code PropertyChangeSupport} from {@link #getPropertyChangeSupport}.
     *
     * @param listener the {@code PropertyChangeListener} to be added
     */
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        getPropertyChangeSupport().addPropertyChangeListener(listener);
    }

    /**
     * Removes a {@code PropertyChangeListener} from the listener list. This
     * removes a {@code PropertyChangeListener} that was registered for all
     * properties. If {@code listener} was added more than once to the same
     * event source, it will be notified one less time after being removed. If
     * {@code listener} is {@code null}, or was never added, no exception is
     * thrown and no action is taken.
     *
     * <p>
     * Note: This is merely a convenience wrapper. All work is delegated to
     * {@code PropertyChangeSupport} from {@link #getPropertyChangeSupport}.
     *
     * @param listener the {@code PropertyChangeListener} to be removed
     */
    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        getPropertyChangeSupport().removePropertyChangeListener(listener);
    }

    /**
     * Reports a bound property update to any registered listeners. No event is
     * fired if {@code old} and {@code new} are equal and non-null.
     *
     * <p>
     * This {@code SwingWorker} will be the source for
     * any generated events.
     *
     * <p>
     * When called off the <i>Event Dispatch Thread</i>
     * {@code PropertyChangeListeners} are notified asynchronously on
     * the <i>Event Dispatch Thread</i>.
     * <p>
     * Note: This is merely a convenience wrapper. All work is delegated to
     * {@code PropertyChangeSupport} from {@link #getPropertyChangeSupport}.
     *
     *
     * @param propertyName the programmatic name of the property that was
     *        changed
     * @param oldValue the old value of the property
     * @param newValue the new value of the property
     */
    public final void firePropertyChange(String propertyName, Object oldValue,
            Object newValue) {
        getPropertyChangeSupport().firePropertyChange(propertyName,
            oldValue, newValue);
    }

    /**
     * Returns the {@code PropertyChangeSupport} for this {@code SwingWorker}.
     * This method is used when flexible access to bound properties support is
     * needed.
     * <p>
     * This {@code SwingWorker} will be the source for
     * any generated events.
     *
     * <p>
     * Note: The returned {@code PropertyChangeSupport} notifies any
     * {@code PropertyChangeListener}s asynchronously on the <i>Event Dispatch
     * Thread</i> in the event that {@code firePropertyChange} or
     * {@code fireIndexedPropertyChange} are called off the <i>Event Dispatch
     * Thread</i>.
     *
     * @return {@code PropertyChangeSupport} for this {@code SwingWorker}
     */
    public final PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    // PropertyChangeSupports methods END

    /**
     * Returns the {@code SwingWorker} state bound property.
     *
     * @return the current state
     */
    public final StateValue getState() {
        return state;
    }

    /**
     * Sets this {@code SwingWorker} state bound property.
     * @param the state state to set
     */
    private void setState(StateValue state) {
        StateValue old = this.state;
        this.state = state;
        firePropertyChange("state", old, state);
    }

    /**
     * Invokes {@code done} on the EDT.
     */
    private void doneEDT() {
        Runnable doDone =
            new Runnable() {
                public void run() {
                    done();
                }
            };
        if (SwingUtilities.isEventDispatchThread()) {
            doDone.run();
        } else {
            SwingUtilities.invokeLater(doDone);
        }
    }


    private class SwingWorkerPropertyChangeSupport extends PropertyChangeSupport {

        private static final long serialVersionUID = 6872014615393309923L;

        SwingWorkerPropertyChangeSupport(Object source) {
            super(source);
        }

        @Override
        public void firePropertyChange(final PropertyChangeEvent evt) {
            if (SwingUtilities.isEventDispatchThread()) {
                super.firePropertyChange(evt);
            } else {
                Runnable fire =
                    new Runnable() {
                        public void run() {
                            SwingWorkerPropertyChangeSupport.this.firePropertyChange(evt);
                        }
                    };
                SwingUtilities.invokeLater(fire);
            }
        }
    }
}
