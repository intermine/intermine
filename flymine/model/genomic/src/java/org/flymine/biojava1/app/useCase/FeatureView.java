package org.flymine.biojava1.app.useCase;

import org.biojava.bio.gui.sequence.*;
import org.biojava.bio.seq.FeatureFilter;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.symbol.RangeLocation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

/**
 * Example how you can make a Sequence/Feature viewer. Adapted from some BioJava examples ...
 * <p>
 * This FeatureView example is used in SimpleUseCase.java
 * 
 * @author Markus Brosch
 */
public class FeatureView extends JFrame {

  public static final int WINDOW_WIDTH = 1000;
  public static final int WINDOW_HEIGHT = 600;

  private Sequence _seq;
  private JPanel _jPanel = new JPanel();

  private MultiLineRenderer _tracks = new MultiLineRenderer();
  private FeatureRenderer _featureRenderer = new BasicFeatureRenderer();
  private SequencePanel _seqPanel = new SequencePanel();
  private FeatureBlockSequenceRenderer _fbr = new FeatureBlockSequenceRenderer();

  public FeatureView(final Sequence pSeq, final int pStart, final int pEnd) {
    try {
      _seq = pSeq;
      init(pStart, pEnd);
      this.pack();
      this.show();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * initialize GUI components
   */
  private void init(final int pStart, final int pEnd) throws Exception {
    this.setTitle("FeatureView");
    this.getContentPane().add(_jPanel, BorderLayout.CENTER);
    _jPanel.add(_seqPanel, null);
    int seqMin = pStart;
    int seqMax = pEnd;

    //Register the FeatureRenderer with the FeatureBlockSequenceRenderer
    _fbr.setFeatureRenderer(_featureRenderer);

    //Render repeat features
    FeatureRenderer fr_repeats = new ZiggyFeatureRenderer();
    SequenceRenderer fbsr_repeats = new FeatureBlockSequenceRenderer(fr_repeats);
    SequenceRenderer repeats = new FilteringRenderer(fbsr_repeats, new FeatureFilter.ByType(
        "RepeatRegion"), false);
    _tracks.addRenderer(repeats);

    // Render Gene features
    FeatureRenderer fr_mrna = new BasicFeatureRenderer(); //ZiggyFeatureRenderer();
    SequenceRenderer fbsr_mrna = new FeatureBlockSequenceRenderer(fr_mrna);
    SequenceRenderer mrna = new FilteringRenderer(fbsr_mrna, new FeatureFilter.ByType("Gene"),
        false);
    _tracks.addRenderer(mrna);

    // Render Gene features
    FeatureRenderer fr_t = new ZiggyFeatureRenderer();
    SequenceRenderer fbsr_t = new FeatureBlockSequenceRenderer(fr_t);
    SequenceRenderer t = new FilteringRenderer(fbsr_t, new FeatureFilter.ByType("Exon"), false);
    _tracks.addRenderer(t);

    // Add a ruler for showing position in the sequence
    _tracks.addRenderer(new RulerRenderer());

    _tracks.addRenderer(new SymbolSequenceRenderer());

    // Set up sequence panel
    _seqPanel.setScale(1.0 * WINDOW_WIDTH / (seqMax - seqMin + 1));
    //set the MultiLineRenderer as the SequencePanels renderer
    _seqPanel.setRenderer(_tracks);
    //set the Sequence to Render
    _seqPanel.setSequence(_seq);
    //display the whole Sequence
    _seqPanel.setRange(new RangeLocation(seqMin, seqMax));
  }

  /**
   * Overridden so program terminates when window closes
   */
  protected void processWindowEvent(final WindowEvent pWE) {
    if (pWE.getID() == WindowEvent.WINDOW_CLOSING) {
      System.exit(0);
    } else {
      super.processWindowEvent(pWE);
    }
  }
}