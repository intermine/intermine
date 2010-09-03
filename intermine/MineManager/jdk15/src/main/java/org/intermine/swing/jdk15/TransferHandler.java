package org.intermine.swing.jdk15;

import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;

import javax.swing.JComponent;

/**
 * Java 1.5 version of Java 6's javax.swing.TransferHandler.
 *
 * @deprecated Assuming Java 6 is acceptable, this is no longer required.
 */
@Deprecated
public class TransferHandler extends javax.swing.TransferHandler {

    private static final long serialVersionUID = 4699786331123108324L;

    /**
     * An <code>int</code> representing a &quot;link&quot; transfer action.
     * This value is used to specify that data should be linked in a drag
     * and drop operation.
     *
     * @see java.awt.dnd.DnDConstants#ACTION_LINK
     * @since 1.6
     */
    public static final int LINK = DnDConstants.ACTION_LINK;

    public TransferHandler() {
    }

    public TransferHandler(String property) {
        super(property);
    }

    /**
     * This method is called repeatedly during a drag and drop operation
     * to allow the developer to configure properties of, and to return
     * the acceptability of transfers; with a return value of {@code true}
     * indicating that the transfer represented by the given
     * {@code TransferSupport} (which contains all of the details of the
     * transfer) is acceptable at the current time, and a value of {@code false}
     * rejecting the transfer.
     * <p>
     * For those components that automatically display a drop location during
     * drag and drop, accepting the transfer, by default, tells them to show
     * the drop location. This can be changed by calling
     * {@code setShowDropLocation} on the {@code TransferSupport}.
     * <p>
     * By default, when the transfer is accepted, the chosen drop action is that
     * picked by the user via their drag gesture. The developer can override
     * this and choose a different action, from the supported source
     * actions, by calling {@code setDropAction} on the {@code TransferSupport}.
     * <p>
     * On every call to {@code canImport}, the {@code TransferSupport} contains
     * fresh state. As such, any properties set on it must be set on every
     * call. Upon a drop, {@code canImport} is called one final time before
     * calling into {@code importData}. Any state set on the
     * {@code TransferSupport} during that last call will be available in
     * {@code importData}.
     * <p>
     * This method is not called internally in response to paste operations.
     * As such, it is recommended that implementations of {@code importData}
     * explicitly call this method for such cases and that this method
     * be prepared to return the suitability of paste operations as well.
     * <p>
     * Note: The <code>TransferSupport</code> object passed to this method
     * is only valid for the duration of the method call. It is undefined
     * what values it may contain after this method returns.
     *
     * @param support the object containing the details of
     *        the transfer, not <code>null</code>.
     * @return <code>true</code> if the import can happen,
     *         <code>false</code> otherwise
     * @throws NullPointerException if <code>support</code> is {@code null}
     * @see #importData(TransferHandler.TransferSupport)
     * @see javax.swing.TransferHandler.TransferSupport#setShowDropLocation
     * @see javax.swing.TransferHandler.TransferSupport#setDropAction
     * @since 1.6
     */
    public boolean canImport(TransferSupport support) {
        return support.getComponent() instanceof JComponent
            ? super.canImport((JComponent)support.getComponent(), support.getDataFlavors())
            : false;
    }

    /**
     * Causes a transfer to occur from a clipboard or a drag and
     * drop operation. The <code>Transferable</code> to be
     * imported and the component to transfer to are contained
     * within the <code>TransferSupport</code>.
     * <p>
     * While the drag and drop implementation calls {@code canImport}
     * to determine the suitability of a transfer before calling this
     * method, the implementation of paste does not. As such, it cannot
     * be assumed that the transfer is acceptable upon a call to
     * this method for paste. It is recommended that {@code canImport} be
     * explicitly called to cover this case.
     * <p>
     * Note: The <code>TransferSupport</code> object passed to this method
     * is only valid for the duration of the method call. It is undefined
     * what values it may contain after this method returns.
     *
     * @param support the object containing the details of
     *        the transfer, not <code>null</code>.
     * @return true if the data was inserted into the component,
     *         false otherwise
     * @throws NullPointerException if <code>support</code> is {@code null}
     * @see #canImport(TransferHandler.TransferSupport)
     * @since 1.6
     */
    public boolean importData(TransferSupport support) {
        return support.getComponent() instanceof JComponent
            ? super.importData((JComponent)support.getComponent(), support.getTransferable())
            : false;
    }

    /**
     * An interface to tag things with a {@code getTransferHandler} method.
     * @deprecated Assuming Java 6 is acceptable, this is no longer required.
     */
    interface HasGetTransferHandler {

        /** Returns the {@code TransferHandler}.
         *
         * @return The {@code TransferHandler} or {@code null}
         */
        public TransferHandler getTransferHandler();
    }

    /**
     * Represents a location where dropped data should be inserted.
     * This is a base class that only encapsulates a point.
     * Components supporting drop may provide subclasses of this
     * containing more information.
     * <p>
     * Developers typically shouldn't create instances of, or extend, this
     * class. Instead, these are something provided by the DnD
     * implementation by <code>TransferSupport</code> instances and by
     * components with a <code>getDropLocation()</code> method.
     *
     * @see javax.swing.TransferHandler.TransferSupport#getDropLocation
     * @since 1.6
     * 
     * @deprecated Assuming Java 6 is acceptable, this is no longer required.
     */
    public static class DropLocation {
        private final Point dropPoint;

        /**
         * Constructs a drop location for the given point.
         *
         * @param dropPoint the drop point, representing the mouse's
         *        current location within the component.
         * @throws IllegalArgumentException if the point
         *         is <code>null</code>
         */
        protected DropLocation(Point dropPoint) {
            if (dropPoint == null) {
                throw new IllegalArgumentException("Point cannot be null");
            }

            this.dropPoint = new Point(dropPoint);
        }

        /**
         * Returns the drop point, representing the mouse's
         * current location within the component.
         *
         * @return the drop point.
         */
        public final Point getDropPoint() {
            return new Point(dropPoint);
        }

        /**
         * Returns a string representation of this drop location.
         * This method is intended to be used for debugging purposes,
         * and the content and format of the returned string may vary
         * between implementations.
         *
         * @return a string representation of this drop location
         */
        @Override
        public String toString() {
            return getClass().getName() + "[dropPoint=" + dropPoint + "]";
        }
    };

    /**
     * This class encapsulates all relevant details of a clipboard
     * or drag and drop transfer, and also allows for customizing
     * aspects of the drag and drop experience.
     * <p>
     * The main purpose of this class is to provide the information
     * needed by a developer to determine the suitability of a
     * transfer or to import the data contained within. But it also
     * doubles as a controller for customizing properties during drag
     * and drop, such as whether or not to show the drop location,
     * and which drop action to use.
     * <p>
     * Developers typically need not create instances of this
     * class. Instead, they are something provided by the DnD
     * implementation to certain methods in <code>TransferHandler</code>.
     *
     * @see #canImport(TransferHandler.TransferSupport)
     * @see #importData(TransferHandler.TransferSupport)
     * @since 1.6
     * 
     * @deprecated Assuming Java 6 is acceptable, this is no longer required.
     */
    @SuppressWarnings("unused")
    public final static class TransferSupport {
        private boolean isDrop;
        private Component component;

        private boolean showDropLocationIsSet;
        private boolean showDropLocation;

        private int dropAction = -1;

        /**
         * The source is a {@code DropTargetDragEvent} or
         * {@code DropTargetDropEvent} for drops,
         * and a {@code Transferable} otherwise
         */
        private Object source;

        private DropLocation dropLocation;

        /**
         * Create a <code>TransferSupport</code> with <code>isDrop()</code>
         * <code>true</code> for the given component, event, and index.
         *
         * @param component the target component
         * @param event a <code>DropTargetEvent</code>
         */
        TransferSupport(Component component,
                        DropTargetEvent event) {

            isDrop = true;
            setDNDVariables(component, event);
        }

        /**
         * Create a <code>TransferSupport</code> with <code>isDrop()</code>
         * <code>false</code> for the given component and
         * <code>Transferable</code>.
         *
         * @param component the target component
         * @param transferable the transferable
         * @throws NullPointerException if either parameter
         *         is <code>null</code>
         */
        private TransferSupport(Component component, Transferable transferable) {
            if (component == null) {
                throw new NullPointerException("component is null");
            }

            if (transferable == null) {
                throw new NullPointerException("transferable is null");
            }

            isDrop = false;
            this.component = component;
            this.source = transferable;
        }

        /**
         * Allows for a single instance to be reused during DnD.
         *
         * @param component the target component
         * @param event a <code>DropTargetEvent</code>
         */
        private void setDNDVariables(Component component,
                                     DropTargetEvent event) {

            assert isDrop;

            this.component = component;
            this.source = event;
            dropLocation = null;
            dropAction = -1;
            showDropLocationIsSet = false;

            if (source == null) {
                return;
            }

            assert source instanceof DropTargetDragEvent ||
                   source instanceof DropTargetDropEvent;

            Point p = source instanceof DropTargetDragEvent
                          ? ((DropTargetDragEvent)source).getLocation()
                          : ((DropTargetDropEvent)source).getLocation();

            /*
            if (component instanceof JTextComponent) {
                try {
                    AccessibleMethod method
                        = new AccessibleMethod(JTextComponent.class,
                                               "dropLocationForPoint",
                                               Point.class);

                    dropLocation =
                        (DropLocation)method.invokeNoChecked(component, p);
                } catch (NoSuchMethodException e) {
                    throw new AssertionError(
                        "Couldn't locate method JTextComponent.dropLocationForPoint");
                }
            } else if (component instanceof JComponent) {
                dropLocation = ((JComponent)component).dropLocationForPoint(p);
            }
            */
            dropLocation = new DropLocation(p);

            /*
             * The drop location may be null at this point if the component
             * doesn't return custom drop locations. In this case, a point-only
             * drop location will be created lazily when requested.
             */
        }

        /**
         * Returns whether or not this <code>TransferSupport</code>
         * represents a drop operation.
         *
         * @return <code>true</code> if this is a drop operation,
         *         <code>false</code> otherwise.
         */
        public boolean isDrop() {
            return isDrop;
        }

        /**
         * Returns the target component of this transfer.
         *
         * @return the target component
         */
        public Component getComponent() {
            return component;
        }

        /**
         * Checks that this is a drop and throws an
         * {@code IllegalStateException} if it isn't.
         *
         * @throws IllegalStateException if {@code isDrop} is false.
         */
        private void assureIsDrop() {
            if (!isDrop) {
                throw new IllegalStateException("Not a drop");
            }
        }

        /**
         * Returns the current (non-{@code null}) drop location for the component,
         * when this {@code TransferSupport} represents a drop.
         * <p>
         * Note: For components with built-in drop support, this location
         * will be a subclass of {@code DropLocation} of the same type
         * returned by that component's {@code getDropLocation} method.
         * <p>
         * This method is only for use with drag and drop transfers.
         * Calling it when {@code isDrop()} is {@code false} results
         * in an {@code IllegalStateException}.
         *
         * @return the drop location
         * @throws IllegalStateException if this is not a drop
         * @see #isDrop
         */
        public DropLocation getDropLocation() {
            assureIsDrop();

            if (dropLocation == null) {
                /*
                 * component didn't give us a custom drop location,
                 * so lazily create a point-only location
                 */
                Point p = source instanceof DropTargetDragEvent
                              ? ((DropTargetDragEvent)source).getLocation()
                              : ((DropTargetDropEvent)source).getLocation();

                dropLocation = new DropLocation(p);
            }

            return dropLocation;
        }

        /**
         * Sets whether or not the drop location should be visually indicated
         * for the transfer - which must represent a drop. This is applicable to
         * those components that automatically
         * show the drop location when appropriate during a drag and drop
         * operation). By default, the drop location is shown only when the
         * {@code TransferHandler} has said it can accept the import represented
         * by this {@code TransferSupport}. With this method you can force the
         * drop location to always be shown, or always not be shown.
         * <p>
         * This method is only for use with drag and drop transfers.
         * Calling it when {@code isDrop()} is {@code false} results
         * in an {@code IllegalStateException}.
         *
         * @param showDropLocation whether or not to indicate the drop location
         * @throws IllegalStateException if this is not a drop
         * @see #isDrop
         */
        public void setShowDropLocation(boolean showDropLocation) {
            assureIsDrop();

            this.showDropLocation = showDropLocation;
            this.showDropLocationIsSet = true;
        }

        /**
         * Sets the drop action for the transfer - which must represent a drop
         * - to the given action,
         * instead of the default user drop action. The action must be
         * supported by the source's drop actions, and must be one
         * of {@code COPY}, {@code MOVE} or {@code LINK}.
         * <p>
         * This method is only for use with drag and drop transfers.
         * Calling it when {@code isDrop()} is {@code false} results
         * in an {@code IllegalStateException}.
         *
         * @param dropAction the drop action
         * @throws IllegalStateException if this is not a drop
         * @throws IllegalArgumentException if an invalid action is specified
         * @see #getDropAction
         * @see #getUserDropAction
         * @see #getSourceDropActions
         * @see #isDrop
         */
        public void setDropAction(int dropAction) {
            assureIsDrop();

            int action = dropAction & getSourceDropActions();

            if (!(action == COPY || action == MOVE || action == LINK)) {
                throw new IllegalArgumentException("unsupported drop action: " + dropAction);
            }

            this.dropAction = dropAction;
        }

        /**
         * Returns the action chosen for the drop, when this
         * {@code TransferSupport} represents a drop.
         * <p>
         * Unless explicitly chosen by way of {@code setDropAction},
         * this returns the user drop action provided by
         * {@code getUserDropAction}.
         * <p>
         * You may wish to query this in {@code TransferHandler}'s
         * {@code importData} method to customize processing based
         * on the action.
         * <p>
         * This method is only for use with drag and drop transfers.
         * Calling it when {@code isDrop()} is {@code false} results
         * in an {@code IllegalStateException}.
         *
         * @return the action chosen for the drop
         * @throws IllegalStateException if this is not a drop
         * @see #setDropAction
         * @see #getUserDropAction
         * @see #isDrop
         */
        public int getDropAction() {
            return dropAction == -1 ? getUserDropAction() : dropAction;
        }

        /**
         * Returns the user drop action for the drop, when this
         * {@code TransferSupport} represents a drop.
         * <p>
         * The user drop action is chosen for a drop as described in the
         * documentation for {@link java.awt.dnd.DropTargetDragEvent} and
         * {@link java.awt.dnd.DropTargetDropEvent}. A different action
         * may be chosen as the drop action by way of the {@code setDropAction}
         * method.
         * <p>
         * You may wish to query this in {@code TransferHandler}'s
         * {@code canImport} method when determining the suitability of a
         * drop or when deciding on a drop action to explicitly choose.
         * <p>
         * This method is only for use with drag and drop transfers.
         * Calling it when {@code isDrop()} is {@code false} results
         * in an {@code IllegalStateException}.
         *
         * @return the user drop action
         * @throws IllegalStateException if this is not a drop
         * @see #setDropAction
         * @see #getDropAction
         * @see #isDrop
         */
        public int getUserDropAction() {
            assureIsDrop();

            return (source instanceof DropTargetDragEvent)
                ? ((DropTargetDragEvent)source).getDropAction()
                : ((DropTargetDropEvent)source).getDropAction();
        }

        /**
         * Returns the drag source's supported drop actions, when this
         * {@code TransferSupport} represents a drop.
         * <p>
         * The source actions represent the set of actions supported by the
         * source of this transfer, and are represented as some bitwise-OR
         * combination of {@code COPY}, {@code MOVE} and {@code LINK}.
         * You may wish to query this in {@code TransferHandler}'s
         * {@code canImport} method when determining the suitability of a drop
         * or when deciding on a drop action to explicitly choose. To determine
         * if a particular action is supported by the source, bitwise-AND
         * the action with the source drop actions, and then compare the result
         * against the original action. For example:
         * <pre>
         * boolean copySupported = (COPY & getSourceDropActions()) == COPY;
         * </pre>
         * <p>
         * This method is only for use with drag and drop transfers.
         * Calling it when {@code isDrop()} is {@code false} results
         * in an {@code IllegalStateException}.
         *
         * @return the drag source's supported drop actions
         * @throws IllegalStateException if this is not a drop
         * @see #isDrop
         */
        public int getSourceDropActions() {
            assureIsDrop();

            return (source instanceof DropTargetDragEvent)
                ? ((DropTargetDragEvent)source).getSourceActions()
                : ((DropTargetDropEvent)source).getSourceActions();
        }

        /**
         * Returns the data flavors for this transfer.
         *
         * @return the data flavors for this transfer
         */
        public DataFlavor[] getDataFlavors() {
            if (isDrop) {
                if (source instanceof DropTargetDragEvent) {
                    return ((DropTargetDragEvent)source).getCurrentDataFlavors();
                } else {
                    return ((DropTargetDropEvent)source).getCurrentDataFlavors();
                }
            }

            return ((Transferable)source).getTransferDataFlavors();
        }

        /**
         * Returns whether or not the given data flavor is supported.
         *
         * @param df the <code>DataFlavor</code> to test
         * @return whether or not the given flavor is supported.
         */
        public boolean isDataFlavorSupported(DataFlavor df) {
            if (isDrop) {
                if (source instanceof DropTargetDragEvent) {
                    return ((DropTargetDragEvent)source).isDataFlavorSupported(df);
                } else {
                    return ((DropTargetDropEvent)source).isDataFlavorSupported(df);
                }
            }

            return ((Transferable)source).isDataFlavorSupported(df);
        }

        /**
         * Returns the <code>Transferable</code> associated with this transfer.
         * <p>
         * Note: Unless it is necessary to fetch the <code>Transferable</code>
         * directly, use one of the other methods on this class to inquire about
         * the transfer. This may perform better than fetching the
         * <code>Transferable</code> and asking it directly.
         *
         * @return the <code>Transferable</code> associated with this transfer
         */
        public Transferable getTransferable() {
            if (isDrop) {
                if (source instanceof DropTargetDragEvent) {
                    return ((DropTargetDragEvent)source).getTransferable();
                } else {
                    return ((DropTargetDropEvent)source).getTransferable();
                }
            }

            return (Transferable)source;
        }
    }
}
