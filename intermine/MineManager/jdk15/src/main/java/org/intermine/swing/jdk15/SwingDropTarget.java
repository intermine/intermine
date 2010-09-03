package org.intermine.swing.jdk15;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.Serializable;
import java.util.TooManyListenersException;

import javax.swing.JComponent;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.UIResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Java 1.5 version of Java 6's javax.swing.TransferHandler.SwingDropTarget.
 *
 * @deprecated Assuming Java 6 is acceptable, this is no longer required.
 */
@Deprecated
public class SwingDropTarget extends DropTarget implements UIResource {

    private static final long serialVersionUID = 1614623962887074272L;

    public SwingDropTarget(JComponent c) {
        super();
        setComponent(c);
        try {
            super.addDropTargetListener(new DropHandler());
        } catch (TooManyListenersException tmle) {
        }
    }

    public void addDropTargetListener(DropTargetListener dtl) throws TooManyListenersException {
        // Since the super class only supports one DropTargetListener,
        // and we add one from the constructor, we always add to the
        // extended list.
        if (listenerList == null) {
            listenerList = new EventListenerList();
        }
        listenerList.add(DropTargetListener.class, dtl);
        }

    public void removeDropTargetListener(DropTargetListener dtl) {
        if (listenerList != null) {
            listenerList.remove(DropTargetListener.class, dtl);
        }
        }

        // --- DropTargetListener methods (multicast) --------------------------

    public void dragEnter(DropTargetDragEvent e) {
            super.dragEnter(e);
            if (listenerList != null) {
                Object[] listeners = listenerList.getListenerList();
                for (int i = listeners.length-2; i>=0; i-=2) {
                    if (listeners[i]==DropTargetListener.class) {
                        ((DropTargetListener)listeners[i+1]).dragEnter(e);
                    }
                }
            }
        }

    public void dragOver(DropTargetDragEvent e) {
            super.dragOver(e);
            if (listenerList != null) {
                Object[] listeners = listenerList.getListenerList();
                for (int i = listeners.length-2; i>=0; i-=2) {
                    if (listeners[i]==DropTargetListener.class) {
                        ((DropTargetListener)listeners[i+1]).dragOver(e);
                    }
                }
            }
        }

    public void dragExit(DropTargetEvent e) {
            super.dragExit(e);
            if (listenerList != null) {
                Object[] listeners = listenerList.getListenerList();
                for (int i = listeners.length-2; i>=0; i-=2) {
                    if (listeners[i]==DropTargetListener.class) {
                        ((DropTargetListener)listeners[i+1]).dragExit(e);
                    }
                }
            }
        }

    public void drop(DropTargetDropEvent e) {
            super.drop(e);
            if (listenerList != null) {
                Object[] listeners = listenerList.getListenerList();
                for (int i = listeners.length-2; i>=0; i-=2) {
                    if (listeners[i]==DropTargetListener.class) {
                        ((DropTargetListener)listeners[i+1]).drop(e);
                    }
                }
            }
        }

    public void dropActionChanged(DropTargetDragEvent e) {
            super.dropActionChanged(e);
            if (listenerList != null) {
                Object[] listeners = listenerList.getListenerList();
                for (int i = listeners.length-2; i>=0; i-=2) {
                    if (listeners[i]==DropTargetListener.class) {
                        ((DropTargetListener)listeners[i+1]).dropActionChanged(e);
                    }
                }
            }
        }

    private EventListenerList listenerList;
}

/**
 * Java 1.5 version of Java 6's javax.swing.TransferHandler.DropHandler.
 *
 * @deprecated Assuming Java 6 is acceptable, this is no longer required.
 */
@Deprecated
class DropHandler implements DropTargetListener, Serializable {

    private static final long serialVersionUID = 1480883206905797703L;

    Log logger = LogFactory.getLog(getClass());

    private boolean canImport;

    private boolean actionSupported(int action) {
        return (action & (TransferHandler.COPY_OR_MOVE | TransferHandler.LINK)) != TransferHandler.NONE;
    }

        // --- DropTargetListener methods -----------------------------------

    public void dragEnter(DropTargetDragEvent e) {
            @SuppressWarnings("unused")
            DataFlavor[] flavors = e.getCurrentDataFlavors();

            JComponent c = (JComponent)e.getDropTargetContext().getComponent();
            TransferHandler importer = (TransferHandler)c.getTransferHandler();

            TransferHandler.TransferSupport support =
                new TransferHandler.TransferSupport(c, e);

        if (importer != null && importer.canImport(support)) {
            canImport = true;
        } else {
            canImport = false;
        }

        int dropAction = e.getDropAction();

        if (canImport && actionSupported(dropAction)) {
                e.acceptDrag(dropAction);
            } else {
                e.rejectDrag();
            }
        }

    public void dragOver(DropTargetDragEvent e) {
        int dropAction = e.getDropAction();

        if (canImport && actionSupported(dropAction)) {
            e.acceptDrag(dropAction);
        } else {
            e.rejectDrag();
        }
        }

    public void dragExit(DropTargetEvent e) {
        }

    public void drop(DropTargetDropEvent e) {
        int dropAction = e.getDropAction();

        JComponent c = (JComponent)e.getDropTargetContext().getComponent();
        TransferHandler importer = (TransferHandler)c.getTransferHandler();

            if (canImport && importer != null && actionSupported(dropAction)) {
                e.acceptDrop(dropAction);

            try {
                @SuppressWarnings("unused")
                Transferable t = e.getTransferable();
                TransferHandler.TransferSupport support =
                    new TransferHandler.TransferSupport(c, e);

                e.dropComplete(importer.importData(support));
            } catch (RuntimeException re) {
                e.dropComplete(false);
            }
            } else {
                e.rejectDrop();
            }
        }

    public void dropActionChanged(DropTargetDragEvent e) {
        int dropAction = e.getDropAction();

        if (canImport && actionSupported(dropAction)) {
            e.acceptDrag(dropAction);
        } else {
            e.rejectDrag();
        }
        }
}
