/**
 * @author sliva
 */
package compiler.phases.seman;

import compiler.common.report.*;
import compiler.data.abstree.*;
import compiler.data.abstree.visitor.*;
import compiler.data.type.SemPtrType;

/**
 * Determines which value expression can denote an address.
 *
 * @author sliva
 */
public class AddrResolver extends AbsFullVisitor<Boolean, Object> {

    @Override
    public Boolean visit(AbsArrExpr expr, Object visArg) {
        if (expr.array.accept(this, visArg)) {
            SemAn.isAddr.put(expr, true);
            return true;
        }
        SemAn.isAddr.put(expr, false);
        return false;
    }

    @Override
    public Boolean visit(AbsAtomExpr expr, Object visArg) {
        SemAn.isAddr.put(expr, false);
        return false;
    }

    @Override
    public Boolean visit(AbsBinExpr expr, Object visArg) {
        expr.fstExpr.accept(this, visArg);
        expr.sndExpr.accept(this, visArg);
        SemAn.isAddr.put(expr, false);
        return false;
    }

    @Override
    public Boolean visit(AbsBlockExpr expr, Object visArg) {
        expr.stmts.accept(this, visArg);
        expr.expr.accept(this, visArg);
        expr.decls.accept(this, visArg);
        SemAn.isAddr.put(expr, false);
        return false;
    }

    @Override
    public Boolean visit(AbsCastExpr expr, Object visArg) {
        expr.expr.accept(this, visArg);
        SemAn.isAddr.put(expr, false);
        return false;
    }

    @Override
    public Boolean visit(AbsDelExpr expr, Object visArg) {
        expr.expr.accept(this, visArg);
        SemAn.isAddr.put(expr, false);
        return false;
    }

    @Override
    public Boolean visit(AbsFunName expr, Object visArg) {
        expr.args.accept(this, visArg);
        SemAn.isAddr.put(expr, false);
        return false;
    }

    @Override
    public Boolean visit(AbsNewExpr expr, Object visArg) {
        SemAn.isAddr.put(expr, false);
        return false;
    }

    @Override
    public Boolean visit(AbsRecExpr expr, Object visArg) {
        if (expr.record.accept(this, visArg)) {
            SemAn.isAddr.put(expr, true);
            return true;
        }
        SemAn.isAddr.put(expr, false);
        return false;
    }

    @Override
    public Boolean visit(AbsUnExpr expr, Object visArg) {
        if (expr.oper == AbsUnExpr.Oper.DATA
                && SemAn.ofType.get(expr.subExpr).actualType() instanceof SemPtrType) {
            SemAn.isAddr.put(expr, true);
            return true;
        }
        SemAn.isAddr.put(expr, false);
        return false;
    }

    @Override
    public Boolean visit(AbsVarName expr, Object visArg){
        SemAn.isAddr.put(expr, true);
        return true;
    }
}
