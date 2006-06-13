package org.flymine.biojava1.bio;

import org.flymine.model.genomic.BioEntity;

/*
 * Copyright (C) 2002-2004 FlyMine This code may be freely distributed and modified under the terms
 * of the GNU Lesser General Public Licence. This should be distributed with the code. See the
 * LICENSE file for more information or http://www.gnu.org/copyleft/lesser.html.
 */

/**
 * This interface should be implemented by Sequences and Features which reflect a FlyMine BioEntity
 * 
 * @author Markus Brosch
 */
public interface IFlyMine {

  /**
   * Each Feature and each Sequence in BioJava corresponds to one specific FlyMine BioEntity. The ID
   * of this BioEntity is used internally and change from FlyMine release to release. Here it is
   * used to refer to the related BioEntity object.
   * 
   * @return the relating FlyMine BioEntity ID
   */
  public Integer getBioEntityID();

  /**
   * Each Feature and each Sequence in BioJava corresponds to one specific FlyMine BioEntity.
   * 
   * @return the relating FlyMine BioEntity
   */
  public BioEntity getBioEntity();
  
}