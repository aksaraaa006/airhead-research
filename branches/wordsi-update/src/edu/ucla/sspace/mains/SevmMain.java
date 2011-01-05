/*
 * Copyright 2010 Keith Stevens 
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.mains;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;

import edu.ucla.sspace.common.SelectionalPreferenceSpace;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.dependency.DependencyExtractorManager;
import edu.ucla.sspace.dependency.FlatPathWeight;
import edu.ucla.sspace.dependency.UniversalRelationAcceptor;

import edu.ucla.sspace.sevm.SyntacticallyEnrichedVectorModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;


/**
 * @author Keith Stevens
 */
public class SevmMain extends DependencyGenericMain {

  private SelectionalPreferenceSpace sps;

  private BasisMapping<String, String> termBasis;
  private BasisMapping<String, String> relationBasis;

  protected void handleExtraOptions() {
    termBasis = new StringBasisMapping();
    relationBasis = new StringBasisMapping();
  }

  protected SemanticSpace getSpace() {
    setupDependencyExtractor();
    return  new SyntacticallyEnrichedVectorModel(
        termBasis, relationBasis,
        DependencyExtractorManager.getDefaultExtractor(),
        new UniversalRelationAcceptor(), new FlatPathWeight(), 1);
  }

  public static void main(String[] args) throws Exception {
    SevmMain main = new SevmMain();
    main.run(args);
  }

  protected void saveSSpace(SemanticSpace sspace, File outputFile)
    throws IOException{
    long startTime = System.currentTimeMillis();
    ObjectOutputStream ous = new ObjectOutputStream(new FileOutputStream(
          outputFile));
    ous.writeObject(sps);
    ous.close();
    long endTime = System.currentTimeMillis();
    verbose("printed space in %.3f seconds", ((endTime - startTime) / 1000d));
  }
}
