/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hll;

import static com.yahoo.sketches.hll.TgtHllType.HLL_4;
import static com.yahoo.sketches.hll.TgtHllType.HLL_6;
import static com.yahoo.sketches.hll.TgtHllType.HLL_8;
import static java.lang.Math.min;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.SketchesArgumentException;

/**
 * @author Lee Rhodes
 */
@SuppressWarnings("unused")
public class UnionTest {
  static final String LS = System.getProperty("line.separator");

  static final int[] nArr = new int[] {1, 3, 10, 30, 100, 300, 1000, 3000, 10000, 30000};

  @Test
  public void checkUnions() {
    //HLL_4=0,  HLL_6=1, HLL_8=2
    //              n1,...        lgK,...          tgtHll,    Mode         Ooo           Est
    String hdrFmt = "%6s%6s%6s" + "%7s%5s%5s%5s" + "%6s%6s" + "%6s%6s%6s" +"%2s%1s%1s"+ "%12s%12s";
    String hdr = String.format(hdrFmt,
        "n1", "n2", "tot",
        "lgMaxK", "lgK1", "lgK2", "lgKR",
        "tgt1", "tgt2",
        "Mode1", "Mode2", "ModeR",
        "1", "2", "R",
        "Est", "Err%");

    int t1 = 2;
    int t2 = 2;
    int rt = 2; //result type
    println("TgtR: " + TgtHllType.values()[rt].toString());
    println(hdr);

    int lgK1 = 7;
    int lgK2 = 7;
    int lgMaxK = 7;
    int n1 = 7;
    int n2 = 7;
    basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
    n1 = 8;
    n2 = 7;
    basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
    n1 = 7;
    n2 = 8;
    basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
    n1 = 8;
    n2 = 8;
    basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
    n1 = 7;
    n2 = 14;
    basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
    println("++");

    int i = 0;
    for (i = 7; i <= 13; i++)
    {
      lgK1 = i;
      lgK2 = i;
      lgMaxK = i;
      {
        n1 = ((1 << (i - 3)) * 3)/4;
        n2 = n1;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
        n1 += 2;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
        n1 -= 2;
        n2 += 2;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
        n1 += 2;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
      }
      println("-");
      lgK1 = i;
      lgK2 = i+1;
      lgMaxK = i;
      {
        n1 = ((1 << (i - 3)) * 3)/4;
        n2 = n1;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
        n1 += 2;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
        n1 -= 2;
        n2 += 2;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
        n1 += 2;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
      }
      println("-");
      lgK1 = i + 1;
      lgK2 = i;
      lgMaxK = i;
      {
        n1 = ((1 << (i - 3)) * 3)/4;
        n2 = n1;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
        n1 += 2;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
        n1 -= 2;
        n2 += 2;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
        n1 += 2;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
      }
      println("-");
      lgK1 = i + 1;
      lgK2 = i + 1;
      lgMaxK = i;
      {
        n1 = ((1 << (i - 3)) * 3)/4;
        n2 = n1;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
        n1 += 2;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
        n1 -= 2;
        n2 += 2;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
        n1 += 2;
        basicUnion(n1, n2, lgK1, lgK2, lgMaxK, t1, t2, rt);
      }
      println("++");
    }
  }

  @Test
  public void check() { //n1=8, n2=7, lgK1=lgK2=lgMaxK=7, all HLL_8
    basicUnion(8, 7,   7, 7, 7,  2, 2,  2);
  }

  private static void basicUnion(int n1, int n2, int lgK1, int lgK2,
      int lgMaxK, int t1, int t2, int rt)  {
    long v = 0;
    int tot = n1 + n2;
    TgtHllType type1 = TgtHllType.values()[t1];
    String t1str = type1.toString();
    TgtHllType type2 = TgtHllType.values()[t2];
    String t2str = type2.toString();
    TgtHllType resultType = TgtHllType.values()[rt];
    String rtStr = resultType.toString();

    HllSketch h1 = new HllSketch(lgK1, type1);
    HllSketch h2 = new HllSketch(lgK2, type2);
    int lgControlK = min(min(lgK1, lgK2), lgMaxK); //min of all 3
    HllSketch control = new HllSketch(lgControlK, resultType);
    String fmt = "%6d%6d%6d," + "%7d%5d%5d%5d," + "%6s%6s," + "%6s%6s%6s,"
        +"%2s%2s%2s," + "%12f%12f%%";

    for (long i = 0; i < n1; i++) {
      h1.update(v + i);
      control.update(v + i);
    }
    v += n1;
    for (long i = 0; i < n2; i++) {
      h2.update(v + i);
      control.update(v + i);
    }
    v += n2;

    String h1SketchStr = ("H1 SKETCH: \n" + h1.toString());
    String h2SketchStr = ("H2 SKETCH: \n" + h2.toString());

    Union union = new Union(lgMaxK);
    union.update(h1);

    String uH1SketchStr = ("Union after H1: \n" + union.getResult(resultType).toString());
    //println(uH1SketchStr);

    union.update(h2);
    HllSketch result = union.getResult(resultType);
    int lgKR = result.getLgConfigK();

    String uSketchStr =("Union after H2: \n" + result.toString());

    double uEst = result.getEstimate();
    double uUb = result.getUpperBound(2);
    double uLb = result.getLowerBound(2);
    double rerr = ((uEst/tot) - 1.0) * 100;
    String mode1 = h1.getCurrentMode().toString();
    String mode2 = h2.getCurrentMode().toString();
    String modeR = result.getCurrentMode().toString();

    //Control
    String cSketchStr = ("CONTROL SKETCH: \n" + control.toString());
    double controlEst = control.getEstimate();
    double controlUb = control.getUpperBound(2);
    double controlLb = control.getLowerBound(2);
    String h1ooo = h1.isOutOfOrderFlag() ? "T" : "F";
    String h2ooo = h2.isOutOfOrderFlag() ? "T" : "F";
    String resultooo = result.isOutOfOrderFlag() ?  "T" : "F";
    String row = String.format(fmt,
        n1, n2, tot,
        lgMaxK, lgK1, lgK2, lgKR,
        t1str, t2str,
        mode1, mode2, modeR,
        h1ooo, h2ooo, resultooo,
        uEst, rerr);
    println(row);
    println(h1SketchStr);
    println(h2SketchStr);
    println(uH1SketchStr);
    println(uSketchStr);
    println(cSketchStr);

    assertTrue((controlUb - controlEst) <= (uUb - uEst));
    assertTrue((controlEst - controlLb) <= (uEst - uLb));
  }

  @Test
  public void checkToFromUnion1() {
    for (int i = 0; i < 10; i++) {
      int n = nArr[i];
      for (int lgK = 4; lgK <= 13; lgK++) {
        toFrom1(lgK, HLL_4, n);
        toFrom1(lgK, HLL_6, n);
        toFrom1(lgK, HLL_8, n);
      }
      println("=======");
    }
  }

  private static void toFrom1(int lgK, TgtHllType tgtHllType, int n) {
    Union srcU = new Union(lgK);
    HllSketch srcSk = new HllSketch(lgK, tgtHllType);
    for (int i = 0; i < n; i++) {
      srcSk.update(i);
    }
    println("n: " + n + ", lgK: " + lgK + ", type: " + tgtHllType);
    //printSketch(src, "SRC");
    srcU.update(srcSk);

    byte[] byteArr = srcU.toCompactByteArray();
    Memory mem = Memory.wrap(byteArr);
    Union dstU = Union.heapify(mem);

    assertEquals(dstU.getEstimate(), srcU.getEstimate(), 0.0);
  }

  @Test
  public void checkToFromUnion2() {
    for (int i = 0; i < 10; i++) {
      int n = nArr[i];
      for (int lgK = 4; lgK <= 13; lgK++) {
        toFrom2(lgK, HLL_4, n);
        toFrom2(lgK, HLL_6, n);
        toFrom2(lgK, HLL_8, n);
      }
      println("=======");
    }
  }

  private static void toFrom2(int lgK, TgtHllType tgtHllType, int n) {
    Union srcU = new Union(lgK);
    HllSketch srcSk = new HllSketch(lgK, tgtHllType);
    for (int i = 0; i < n; i++) {
      srcSk.update(i);
    }
    println("n: " + n + ", lgK: " + lgK + ", type: " + tgtHllType);
    //printSketch(src, "SRC");
    srcU.update(srcSk);

    byte[] byteArr = srcU.toCompactByteArray();
    Union dstU = Union.heapify(byteArr);

    assertEquals(dstU.getEstimate(), srcU.getEstimate(), 0.0);
  }

  @Test
  public void checkCompositeEst() {
    Union u = new Union(12);
    assertEquals(u.getCompositeEstimate(), 0, .03);
    for (int i = 1; i <= 15; i++) { u.update(i); }
    assertEquals(u.getCompositeEstimate(), 15, 15 *.03);
    for (int i = 15; i <= 1000; i++) { u.update(i); }
    assertEquals(u.getCompositeEstimate(), 1000, 1000 * .03);
  }

  @Test
  public void checkMisc() {
    try {
      Union u = new Union(HllUtil.MIN_LOG_K - 1);
      fail();
    } catch (SketchesArgumentException e) {
      //expected
    }
    try {
      Union u = new Union(HllUtil.MAX_LOG_K + 1);
      fail();
    } catch (SketchesArgumentException e) {
      //expected
    }
    Union u = new Union(7);
    HllSketch sk = u.getResult();
    assertTrue(sk.isEmpty());
  }

  @Test
  public void checkHeapify() {
    Union u = new Union(16);
    for (int i = 0; i < (1 << 20); i++) {
      u.update(i);
    }
    double est1 = u.getEstimate();
    byte[] byteArray = u.toUpdatableByteArray();
    Union u2 = Union.heapify(byteArray);
    assertEquals(u2.getEstimate(), est1, 0.0);
  }

  @Test //for lgK <= 12
  public void checkUbLb() {
    int lgK = 4;
    int n = 1 << 20;
    boolean oooFlag = false;
    println("LgK="+lgK+", UB3, " + ((getBound(lgK, true, oooFlag, 3, n) / n) - 1));
    println("LgK="+lgK+", UB2, " + ((getBound(lgK, true, oooFlag, 2, n) / n) - 1));
    println("LgK="+lgK+", UB1, " + ((getBound(lgK, true, oooFlag, 1, n) / n) - 1));
    println("LgK="+lgK+", LB1, " + ((getBound(lgK, false, oooFlag, 1, n) / n) - 1));
    println("LgK="+lgK+", LB2, " + ((getBound(lgK, false, oooFlag, 2, n) / n) - 1));
    println("LgK="+lgK+", LB3, " + ((getBound(lgK, false, oooFlag, 3, n) / n) - 1));
  }

  @Test
  public void checkEmptyCouponMisc() {
    int lgK = 8;
    Union union = new Union(lgK);
    for (int i = 0; i < 20; i++) { union.update(i); } //SET mode
    union.couponUpdate(0);
    assertEquals(union.getEstimate(), 20.0, 0.001);
    assertEquals(union.getTgtHllType(), TgtHllType.HLL_8);
    assertFalse(union.isMemory());
    assertFalse(union.isOffHeap());
    int bytes = union.getUpdatableSerializationBytes();
    assertTrue(bytes  <= Union.getMaxSerializationBytes(lgK));

  }

  private static double getBound(int lgK, boolean ub, boolean oooFlag, int numStdDev, double est) {
    double re = RelativeErrorTables.getRelErr(ub, oooFlag, lgK, numStdDev);
    return (ub) ? est / (1.0 + re) : est / (1.0 +re);
  }

  @Test
  public void printlnTest() {
    println("PRINTING: "+this.getClass().getName());
  }

  /**
   * @param s value to print
   */
  static void println(String s) {
    print(s + LS);
  }

  /**
   * @param s value to print
   */
  static void print(String s) {
    //System.out.print(s); //disable here
  }

}
