package org.basex.util.ft;

import static java.lang.StrictMath.*;

/**
 * Default scoring model, assembling all score calculations.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public final class Scoring {
  /** Logarithmic base for calculating the score value. */
  private static final double LOG = Math.E - 1;
  /** Scoring step. */
  private static final double SCORESTEP = 0.8;

  /** Private constructor. */
  private Scoring() { }

  /**
   * Calculates a score value, based on the token length
   * and complete text length.
   * @param tl token length
   * @param l complete length
   * @return result
   */
  public static double word(final int tl, final double l) {
    return min(1, log(1 + LOG * tl / l));
  }

  /**
   * Combines two scoring values.
   * @param o old value
   * @param n new value
   * @return result
   */
  public static double and(final double o, final double n) {
    return 1 - (1 - o) * (1 - n);
  }

  /**
   * Combines two scoring values.
   * @param o old value
   * @param n new value
   * @return result
   */
  public static double or(final double o, final double n) {
    return and(o, n);
  }

  /**
   * Inverses the scoring value for FTNot.
   * @param d scoring value
   * @return inverse scoring value
   */
  public static double not(final double d) {
    return 1 - d;
  }

  /**
   * Returns a score for the let clause.
   * @param s summed up scoring values
   * @param c number of values
   * @return new score value
   */
  public static double let(final double s, final int c) {
    return s / c;
  }

  /**
   * Calculates the score for a text node.
   * Used if no index score is available.
   * @param npv number of pos values
   * @param is total number of index entries
   * @param tokl token length
   * @param tl text length
   * @return score value
   */
  public static double textNode(final int npv, final int is, final int tokl,
      final int tl) {
    return max((double) npv / is, log(tokl * npv + 1) / log(tl + 1));
  }

  /**
   * Returns a score for a single step.
   * @param sc current score value
   * @return new score value
   */
  public static double step(final double sc) {
    return sc * SCORESTEP;
  }
}
