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

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.dependency.CoNLLDependencyExtractor;
import edu.ucla.sspace.dependency.DefaultDependencyPermutationFunction;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyPermutationFunction;
import edu.ucla.sspace.dependency.FlatPathWeight;
import edu.ucla.sspace.dependency.UniversalPathAcceptor;

import edu.ucla.sspace.index.PermutationFunction;
import edu.ucla.sspace.index.RandomIndexVectorGenerator;

import edu.ucla.sspace.util.GeneratorMap;

import edu.ucla.sspace.text.DependencyFileDocumentIterator;
import edu.ucla.sspace.text.Document;

import edu.ucla.sspace.util.ReflectionUtil;

import edu.ucla.sspace.vector.TernaryVector;

import edu.ucla.sspace.wordsi.ContextExtractor;
import edu.ucla.sspace.wordsi.DependencyContextExtractor;
import edu.ucla.sspace.wordsi.DependencyContextGenerator;
import edu.ucla.sspace.wordsi.RandomIndexingDependencyContextGenerator;

import edu.ucla.sspace.wordsi.psd.PseudoWordDependencyContextExtractor;
import edu.ucla.sspace.wordsi.semeval.SemEvalDependencyContextExtractor;

import java.io.IOException;

import java.util.Collection;
import java.util.Iterator;


/**
 * A dependency based executable class for running {@link Wordsi}.  {@link
 * GenericWordsiMain} provides the core command line arguments and
 * functionality.  This class provides the following additional arguments:
 *
 * <ul>
 *   <li><u>Optional</u>
 *     <ul>
 *       </li> {@code -p}, {@code --pathAcceptor=CLASSNAME} Specifies the {@link
 *       DependencyPathAcceptor} to use when validating paths as features.
 *       (Default: {@link UniversalPathAcceptor})
 *       
 *       </li> {@code -W}, {@code --weightingFunction=CLASSNAME} Specifies the
 *       class that will weight dependency paths.
 *       
 *       </li> {@code -b}, {@code --basisMapping=CLASSNAME} Specifies the class
 *       that deterine what aspect of a {@link DependencyPath} will as a feature
 *       in the word space. (Default: {@link WordBasedBasisMapping})
 *     </ul>
 *   </li>
 * </ul>
 *
 * @author Keith Stevens
 */
public class DVRIWordsiMain extends GenericWordsiMain {

  /**
   * The {@link DependencyPathBasisMapping} used to generate feature indices for
   * dependency paths.
   */
  private GeneratorMap<TernaryVector> indexMap;

  private DependencyPermutationFunction<TernaryVector> permFunc;

  /**
   * {@inheritDoc}
   */
  protected void addExtraOptions(ArgOptions options) {
    super.addExtraOptions(options);

    options.addOption('p', "pathAcceptor",
                      "Specifies the DependencyPathAcceptor to use when " +
                      "validating paths as features.  (Default: Universal)",
                      true, "CLASSNAME", "Optional");
    options.addOption('P', "permutationFunction",
                      "Specifies the DependencyPermutationFunction for " +
                      "TernaryVectors that will permute index vectors before " +
                      "adding them to context vectors. (Default: None)",
                      true, "CLASSNAME", "Optional");
    options.addOption('l', "vectorLength",
                      "Specifies the length of each index vector. " +
                      "(Default: 5000)",
                      true, "CLASSNAME", "Optional");
  }

  /**
   * {@inheritDoc}
   */
  protected void postProcessing() {
    if (argOptions.hasOption('S')) {
      saveObject(openSaveFile(), indexMap);
      saveObject(openSaveFile(), permFunc);
    }
  }

  /**
   * {@inheritDoc}
   */
  protected ContextExtractor getExtractor() {
    int pathLength = argOptions.getIntOption('W', 5);
    int vectorLength = argOptions.getIntOption('L', 5000);

    if (argOptions.hasOption('L')) {
      indexMap = loadObject(openLoadFile());
      permFunc = loadObject(openLoadFile());
    } else {
      indexMap = new GeneratorMap<TernaryVector>(
              new RandomIndexVectorGenerator(vectorLength));
      if (argOptions.hasOption('P')) {
          PermutationFunction<TernaryVector> basePermFunc = 
              ReflectionUtil.getObjectInstance(argOptions.getStringOption('P'));
        permFunc = new DefaultDependencyPermutationFunction<TernaryVector>(
                basePermFunc);
      }
    }

    // Create the acceptor.
    DependencyPathAcceptor acceptor;
    if (argOptions.hasOption('p'))
      acceptor = ReflectionUtil.getObjectInstance(
          argOptions.getStringOption('p'));
    else 
      acceptor = new UniversalPathAcceptor();

    // Set to read only if in evaluation mode.
    if (argOptions.hasOption('e'))
      indexMap.setReadOnly();

    DependencyContextGenerator generator = 
        new RandomIndexingDependencyContextGenerator(
                permFunc, acceptor, indexMap, vectorLength, pathLength);

    // If the evaluation type is for semEval, use a
    // SemEvalDependencyContextExtractor.
    if (argOptions.hasOption('E'))
      return new SemEvalDependencyContextExtractor(
          new CoNLLDependencyExtractor(), generator);

    // If the evaluation type is for pseudoWord, use a
    // PseudoWordDependencyContextExtractor.
    if (argOptions.hasOption('P'))
      return new PseudoWordDependencyContextExtractor(
          new CoNLLDependencyExtractor(), generator, getPseudoWordMap());

    // Otherwise return the normal extractor.
    return new DependencyContextExtractor(
            new CoNLLDependencyExtractor(), generator);
  }

  /**
   * {@inheritDoc}
   */
  protected SSpaceFormat getSpaceFormat() {
    return SSpaceFormat.SPARSE_BINARY;
  }

  /**
   * Throws {@link UnsupportedOperationException}.
   */
  protected void addFileIterators(Collection<Iterator<Document>> docIters,
                                  String[] fileNames) throws IOException {
    throw new UnsupportedOperationException(
        "A file based document iterator does not exist");
  }

  /**
   * Adds {@link DependencyFileDocumentIterator}s for each file name provided.
   */
  protected void addDocIterators(Collection<Iterator<Document>> docIters,
                                 String[] fileNames) throws IOException {
    // All the documents are listed in one file, with one document per line
    for (String s : fileNames)
      docIters.add(new DependencyFileDocumentIterator(s));
  }
}
