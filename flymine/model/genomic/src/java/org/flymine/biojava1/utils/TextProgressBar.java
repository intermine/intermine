package org.flymine.biojava1.utils;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * This is a text progess bar. Once started, it prints "#" to indicate progress.
 * 
 * @author Markus Brosch
 */
public class TextProgressBar implements Runnable {

  /**
   * the Thread printing the "#"
   */
  private Thread _thread;
  
  /**
   * Start the progessbar
   */
  public void start() {
    (_thread = new Thread(this)).start();
  }

  /**
   * Stop the progressbar
   */
  public void stop() {
    System.out.println("DONE!");
    _thread = null;
  }

  /**
   * @see Runnable#run()
   */
  public void run() {
    final int delay = 5000;
    try {
      int i = 0;
      while (_thread == Thread.currentThread()) {
        System.out.println((++i) + " - processing ...");
        Thread.sleep(delay);
      }
    } catch (InterruptedException e) { }
  }

}