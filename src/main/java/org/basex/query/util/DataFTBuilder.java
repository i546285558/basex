package org.basex.query.util;

import static org.basex.util.Token.*;
import org.basex.data.FTPos;
import org.basex.data.FTPosData;
import org.basex.query.item.DBNode;
import org.basex.query.item.Nod;
import org.basex.util.TokenBuilder;
import org.basex.util.TokenList;
import org.basex.util.ft.FTLexer;
import org.basex.util.ft.FTSpan;

/**
 * Class for constructing decorated full-text nodes.
 *
 * @author BaseX Team 2005-11, ISC License
 * @author Christian Gruen
 */
final class DataFTBuilder {
  /** Dots. */
  private static final byte[] DOTS = token("...");
  /** Full-text position data. */
  private final FTPosData ftpos;
  /** Length of full-text extract. */
  private final int ftlen;

  /**
   * Constructor.
   * @param pos full-text position data
   * @param len length of extract
   */
  DataFTBuilder(final FTPosData pos, final int len) {
    ftpos = pos;
    ftlen = len;
  }

  /**
   * Builds full-text information.
   * @param nd node to be added
   * @return number of added nodes
   */
  TokenList build(final Nod nd) {
    // check full-text mode
    if(!(nd instanceof DBNode)) return null;

    // check if full-text data exists for the current node
    final DBNode node = (DBNode) nd;
    final FTPos ftp = ftpos.get(node.data, node.pre);
    if(ftp == null) return null;

    boolean marked = false;
    final TokenList tl = new TokenList();
    final TokenBuilder tb = new TokenBuilder();
    final FTLexer lex = new FTLexer().sc().init(nd.atom());
    int len = -ftlen;
    while(lex.hasNext()) {
      final FTSpan span = lex.next();
      // check if current text is still to be marked or already marked
      if(ftp.contains(span.pos) || marked) {
        if(tb.size() != 0) {
          // write current text node
          tl.add(tb.finish());
          len += tb.size();
          tb.reset();
          // skip construction
          if(len >= 0 && tl.size() > 1 && !marked) break;
        }
        if(!marked) tl.add((byte[]) null);
        marked ^= true;
      }
      // add span
      tb.add(span.text);
    }
    // write last text node
    if(tb.size() != 0) {
      tl.add(tb.finish());
      len += tb.size();
    }

    // chop first and last text
    if(len > 0) {
      final int ts = tl.size();
      // get first text (empty if it is a full-text match)
      final byte[] first = tl.get(0) != null ? tl.get(0) : EMPTY;
      // get last text (empty if it is a full-text match)
      final byte[] last = tl.get(ts - 2) != null ? tl.get(ts - 1) : EMPTY;

      if(first != EMPTY) {
        // remove leading characters of first text
        final double l = first.length + last.length;
        final int ll = Math.min(first.length, (int) (first.length / l * len));
        tl.set(concat(DOTS, subtoken(first, ll)), 0);
        len -= ll;
      }
      if(last != EMPTY && len > 0) {
        // remove trailing characters of last text
        final int ll = Math.min(last.length, len);
        tl.set(concat(subtoken(last, 0, last.length - ll), DOTS), ts - 1);
        len -= ll;
      }
      // still too much text: shorten inner texts
      for(int t = ts - 2; t > 0 && len > 0; t--) {
        final byte[] txt = tl.get(t);
        // skip elements, marked texts and too short text snippets
        if(txt == null || tl.get(t - 1) == null) continue;
        final int ll = Math.min(txt.length, len);
        tl.set(concat(subtoken(txt, 0, (txt.length - ll) / 2), DOTS,
            subtoken(txt, (txt.length + ll) / 2)), t);
        len -= ll;
      }
    }
    return tl;
  }
}