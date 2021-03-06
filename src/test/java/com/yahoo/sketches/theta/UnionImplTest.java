/*
 * Copyright 2015-16, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.theta;

import static com.yahoo.sketches.Util.DEFAULT_UPDATE_SEED;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.yahoo.memory.Memory;
import com.yahoo.memory.WritableMemory;
import com.yahoo.sketches.SketchesArgumentException;

public class UnionImplTest {

  @Test
  public void checkUpdateWithSketch() {
    int k = 16;
    WritableMemory mem = WritableMemory.wrap(new byte[(k*8) + 24]);
    UpdateSketch sketch = Sketches.updateSketchBuilder().setNominalEntries(k).build();
    for (int i=0; i<k; i++)
     {
      sketch.update(i); //exact
    }
    CompactSketch sketchInDirectOrd = sketch.compact(true, mem);
    CompactSketch sketchInDirectUnord = sketch.compact(false, mem);
    CompactSketch sketchInHeap = sketch.compact(true, null);

    Union union = Sketches.setOperationBuilder().setNominalEntries(k).buildUnion();
    union.update(sketchInDirectOrd);
    union.update(sketchInHeap);
    union.update(sketchInDirectUnord);
    assertEquals(union.getResult().getEstimate(), k, 0.0);
  }

  @Test
  public void checkUpdateWithMem() {
    int k = 16;
    WritableMemory skMem = WritableMemory.wrap(new byte[(2*k*8) + 24]);
    WritableMemory dirOrdCskMem = WritableMemory.wrap(new byte[(k*8) + 24]);
    WritableMemory dirUnordCskMem = WritableMemory.wrap(new byte[(k*8) + 24]);
    UpdateSketch udSketch = UpdateSketch.builder().setNominalEntries(k).build(skMem);
    for (int i = 0; i < k; i++) { udSketch.update(i); } //exact
    udSketch.compact(true, dirOrdCskMem);
    udSketch.compact(false, dirUnordCskMem);

    Union union = Sketches.setOperationBuilder().setNominalEntries(k).buildUnion();
    union.update(skMem);
    union.update(dirOrdCskMem);
    union.update(dirUnordCskMem);
    assertEquals(union.getResult().getEstimate(), k, 0.0);
  }

  @Test
  public void checkFastWrap() {
    int k = 16;
    long seed = DEFAULT_UPDATE_SEED;
    int unionSize = Sketches.getMaxUnionBytes(k);
    WritableMemory srcMem = WritableMemory.wrap(new byte[unionSize]);
    Union union = Sketches.setOperationBuilder().setNominalEntries(k).buildUnion(srcMem);
    for (int i = 0; i < k; i++) { union.update(i); } //exact
    assertEquals(union.getResult().getEstimate(), k, 0.0);
    Union union2 = UnionImpl.fastWrap(srcMem, seed);
    assertEquals(union2.getResult().getEstimate(), k, 0.0);
    Memory srcMemR = srcMem;
    Union union3 = UnionImpl.fastWrap(srcMemR, seed);
    assertEquals(union3.getResult().getEstimate(), k, 0.0);
  }

  @Test(expectedExceptions = SketchesArgumentException.class)
  public void checkCorruptFamilyException() {
    int k = 16;
    WritableMemory mem = WritableMemory.wrap(new byte[(k*8) + 24]);
    UpdateSketch sketch = Sketches.updateSketchBuilder().setNominalEntries(k).build();
    for (int i=0; i<k; i++) {
      sketch.update(i);
    }
    sketch.compact(true, mem);

    mem.putByte(PreambleUtil.FAMILY_BYTE, (byte)0); //corrupt family

    Union union = Sketches.setOperationBuilder().setNominalEntries(k).buildUnion();
    union.update(mem);
  }

  @Test(expectedExceptions = SketchesArgumentException.class)
  public void checkVer1FamilyException() {
    int k = 16;
    WritableMemory v3mem = WritableMemory.wrap(new byte[(k*8) + 24]);
    UpdateSketch sketch = Sketches.updateSketchBuilder().setNominalEntries(k).build();
    for (int i=0; i<k; i++) {
      sketch.update(i);
    }
    sketch.compact(true, v3mem);
    WritableMemory v1mem = ForwardCompatibilityTest.convertSerV3toSerV1(v3mem);

    v1mem.putByte(PreambleUtil.FAMILY_BYTE, (byte)2); //corrupt family

    Union union = Sketches.setOperationBuilder().setNominalEntries(k).buildUnion();
    union.update(v1mem);
  }

  @Test(expectedExceptions = SketchesArgumentException.class)
  public void checkVer2FamilyException() {
    int k = 16;
    WritableMemory v3mem = WritableMemory.wrap(new byte[(k*8) + 24]);
    UpdateSketch sketch = Sketches.updateSketchBuilder().setNominalEntries(k).build();
    for (int i=0; i<k; i++) {
      sketch.update(i);
    }
    sketch.compact(true, v3mem);
    WritableMemory v2mem = ForwardCompatibilityTest.convertSerV3toSerV2(v3mem);

    v2mem.putByte(PreambleUtil.FAMILY_BYTE, (byte)2); //corrupt family

    Union union = Sketches.setOperationBuilder().setNominalEntries(k).buildUnion();
    union.update(v2mem);
  }

  @Test
  public void checkMoveAndResize() {
    int k = 1 << 12;
    int u = 2 * k;
    int bytes = Sketches.getMaxUpdateSketchBytes(k);
    WritableMemory wmem = WritableMemory.allocate(bytes/2);
    UpdateSketch sketch = Sketches.updateSketchBuilder().setNominalEntries(k).build(wmem);
    assertTrue(sketch.isSameResource(wmem));

    WritableMemory wmem2 = WritableMemory.allocate(bytes/2);
    Union union = SetOperation.builder().buildUnion(wmem2);
    assertTrue(union.isSameResource(wmem2));

    for (int i = 0; i < u; i++) { union.update(i); }
    assertFalse(union.isSameResource(wmem));

    Union union2 = SetOperation.builder().buildUnion(); //on-heap union
    assertFalse(union2.isSameResource(wmem2));  //obviously not
  }

  @Test
  public void printlnTest() {
    println("PRINTING: "+this.getClass().getName());
  }

  /**
   * @param s value to print
   */
  static void println(String s) {
    //System.out.println(s); //disable here
  }

}
