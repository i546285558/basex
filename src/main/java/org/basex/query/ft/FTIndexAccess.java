package org.basex.query.ft;

import static org.basex.query.QueryText.*;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.func.*;
import org.basex.query.iter.*;
import org.basex.query.util.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * FTContains expression with index access.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public final class FTIndexAccess extends Simple {
  /** Full-text expression. */
  private final FTExpr ftexpr;
  /** If the index results are ordered. */
  private final boolean iterable;
  /** Database name. */
  private final String db;

  /**
   * Constructor.
   * @param ii input info
   * @param ex contains, select and optional ignore expression
   * @param nm database name
   * @param iter iterable flag
   */
  public FTIndexAccess(final InputInfo ii, final FTExpr ex, final String nm,
      final boolean iter) {
    super(ii);
    ftexpr = ex;
    iterable = iter;
    db = nm;
  }

  @Override
  public NodeIter iter(final QueryContext ctx) throws QueryException {
    final FTIter ir = ftexpr.iter(ctx);

    return new NodeIter() {
      @Override
      public ANode next() throws QueryException {
        final FTNode it = ir.next();
        if(it != null) {
          // cache entry for visualizations or ft:mark/ft:extract
          if(ctx.ftpos != null) ctx.ftpos.add(it.data, it.pre, it.all);
          // assign scoring, if not done yet
          it.score();
          // remove matches reference to save memory
          it.all = null;
        }
        return it;
      }
    };
  }

  @Override
  public boolean has(final Flag flag) {
    return ftexpr.has(flag);
  }

  @Override
  public boolean removable(final Var v) {
    return ftexpr.removable(v);
  }

  @Override
  public VarUsage count(final Var v) {
    return ftexpr.count(v);
  }

  @Override
  public Expr inline(final QueryContext ctx, final VarScope scp,
      final Var v, final Expr e) throws QueryException {
    return ftexpr.inline(ctx, scp, v, e) == null ? null : optimize(ctx, scp);
  }

  @Override
  public Expr copy(final QueryContext ctx, final VarScope scp, final IntObjMap<Var> vs) {
    return new FTIndexAccess(info, ftexpr.copy(ctx, scp, vs), db, iterable);
  }

  @Override
  public boolean accept(final ASTVisitor visitor) {
    return visitor.lock(db) && ftexpr.accept(visitor);
  }

  @Override
  public void plan(final FElem plan) {
    addPlan(plan, planElem(DATA, db), ftexpr);
  }

  @Override
  public boolean iterable() {
    return iterable;
  }

  @Override
  public String toString() {
    Expr e = ftexpr;
    if(ftexpr instanceof FTWords) {
      final FTWords f = (FTWords) ftexpr;
      if(f.mode == FTMode.ANY && f.occ == null) e = f.query;
    }
    return Function._DB_FULLTEXT.get(info, Str.get(db), e).toString();
  }

  @Override
  public int exprSize() {
    return ftexpr.exprSize() + 1;
  }
}
