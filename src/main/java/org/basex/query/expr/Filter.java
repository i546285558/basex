package org.basex.query.expr;

import java.io.IOException;
import org.basex.data.Serializer;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.func.Fun;
import org.basex.query.func.FunDef;
import org.basex.query.item.Empty;
import org.basex.query.item.Item;
import org.basex.query.item.SeqType;
import org.basex.query.item.Type;
import org.basex.query.item.Value;
import org.basex.query.iter.Iter;
import org.basex.query.iter.ItemIter;
import org.basex.query.path.AxisPath;
import org.basex.query.util.Var;
import org.basex.util.Array;
import org.basex.util.InputInfo;

/**
 * Filter expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public class Filter extends Preds {
  /** Expression. */
  Expr root;
  /** Counter flag. */
  private boolean offset;

  /**
   * Constructor.
   * @param ii input info
   * @param r expression
   * @param p predicates
   */
  public Filter(final InputInfo ii, final Expr r, final Expr... p) {
    super(ii, p);
    root = r;
  }

  @Override
  public final Expr comp(final QueryContext ctx) throws QueryException {
    root = checkUp(root, ctx).comp(ctx);

    // convert filters to axis paths
    if(root instanceof AxisPath && !super.uses(Use.POS))
      return ((AxisPath) root).copy().addPreds(pred).comp(ctx);

    // return empty root
    if(root.empty()) return optPre(Empty.SEQ, ctx);

    final Type ct = ctx.value != null ? ctx.value.type : null;
    if(ct != null) ctx.value.type = root.type().type;
    final Expr e = super.comp(ctx);
    if(e != this) return e;
    if(ct != null) ctx.value.type = ct;

    // no predicates.. return root
    if(pred.length == 0) return root;
    final Expr p = pred[0];

    // evaluate return type
    final SeqType t = root.type();
    type = SeqType.get(t.type, t.zeroOrOne() ? SeqType.Occ.ZO : SeqType.Occ.ZM);

    // position predicate
    final Pos pos = p instanceof Pos ? (Pos) p : null;
    // last flag
    final boolean last = p instanceof Fun && ((Fun) p).def == FunDef.LAST;
    // use iterative evaluation
    if(pred.length == 1 && (last || pos != null || !uses(Use.POS)))
      return new IterFilter(this, pos, last);

    // faster runtime evaluation of variable counters (array[$pos] ...)
    offset = pred.length == 1 && p.type().num() && !p.uses(Use.CTX);

    return this;
  }

  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    if(offset) {
      // evaluate offset and create position expression
      final Item it = pred[0].ebv(ctx, input);
      final long l = it.itr(input);
      final Expr e = Pos.get(l, l, input);
      // don't accept fractional numbers
      return l != it.dbl(input) || !(e instanceof Pos) ? Iter.EMPTY :
        new IterFilter(this, (Pos) e, false).iter(ctx);
    }

    final Iter iter = ctx.iter(root);
    final Value cv = ctx.value;
    final long cs = ctx.size;
    final long cp = ctx.pos;

    // cache results to support last() function
    final ItemIter ir = new ItemIter();
    Item i;
    while((i = iter.next()) != null) ir.add(i);

    // evaluate predicates
    for(final Expr p : pred) {
      final long is = ir.size();
      ctx.size = is;
      ctx.pos = 1;
      int c = 0;
      for(int s = 0; s < is; ++s) {
        ctx.value = ir.item[s];
        if(p.test(ctx, input) != null) ir.item[c++] = ir.item[s];
        ctx.pos++;
      }
      ir.size(c);
    }
    ctx.value = cv;
    ctx.size = cs;
    ctx.pos = cp;
    return ir;
  }

  /**
   * Adds a predicate to the filter.
   * @param p predicate to be added
   * @return self reference
   */
  public final Filter addPred(final Expr p) {
    pred = Array.add(pred, p);
    return this;
  }

  @Override
  public final boolean uses(final Use u) {
    return root.uses(u) || super.uses(u);
  }

  @Override
  public final boolean removable(final Var v) {
    return root.removable(v) && super.removable(v);
  }

  @Override
  public final Expr remove(final Var v) {
    root = root.remove(v);
    return super.remove(v);
  }

  @Override
  public final void plan(final Serializer ser) throws IOException {
    ser.openElement(this);
    root.plan(ser);
    super.plan(ser);
    ser.closeElement();
  }

  @Override
  public final String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(root);
    //sb.append(root.size() != 1 ? root : "(" + root + ")");
    return sb.append(super.toString()).toString();
  }
}
