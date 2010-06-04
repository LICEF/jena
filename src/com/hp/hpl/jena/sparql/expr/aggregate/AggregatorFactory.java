/*
 * (c) Copyright 2010 Talis Systems Ltd.
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.expr.aggregate;

import com.hp.hpl.jena.sparql.expr.Expr ;
import com.hp.hpl.jena.sparql.expr.ExprList ;
import com.hp.hpl.jena.sparql.util.ALog ;

public class AggregatorFactory
{
    public static AggregateFactory createCount(boolean distinct)
    { 
        return distinct ? AggCountDistinct.get() : AggCount.get() ;
    }

    public static AggregateFactory createCountExpr(boolean distinct, Expr expr)
    { 
        return distinct ? new AggCountVarDistinct(expr) : new AggCountVar(expr) ;
    }
    
    public static AggregateFactory createSum(boolean distinct, Expr expr)
    { 
        return distinct ? err("sum distinct") : new AggSum(expr) ;
    }
    
    public static AggregateFactory createMin(boolean distinct, Expr expr)
    { 
        return distinct ? err("min distinct") : new AggMin(expr) ;
    }
    
    public static AggregateFactory createMax(boolean distinct, Expr expr)
    { 
        return distinct ? err("max distinct") : new AggMax(expr) ;
    }
    
    public static AggregateFactory createAvg(boolean distinct, Expr expr)
    { 
        return distinct ? err("avg distinct") : new AggAvg(expr) ;
    }
        
    public static AggregateFactory createSample(boolean distinct, Expr expr)    { return null ; }
    
    public static AggregateFactory createGroupConcat(boolean distinct, ExprList exprList, String separator)
    { return null ; }
    
    private static AggregateFactory err(String label)
    {
        ALog.fatal(AggregatorFactory.class, "Not implemented: "+label) ;
        return null ;
    }
}

/*
 * (c) Copyright 2010 Talis Systems Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */