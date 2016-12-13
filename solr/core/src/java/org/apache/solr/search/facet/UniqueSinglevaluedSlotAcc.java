/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.search.facet;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.LongValues;
import org.apache.solr.search.SolrIndexSearcher;

class UniqueSinglevaluedSlotAcc extends UniqueSlotAcc {
  final SortedDocValues topLevel;
  final SortedDocValues[] subDvs;
  final MultiDocValues.OrdinalMap ordMap;
  LongValues toGlobal;
  SortedDocValues subDv;

  public UniqueSinglevaluedSlotAcc(FacetContext fcontext, String field, int numSlots, HLLAgg.HLLFactory factory) throws IOException {
    super(fcontext, field, numSlots, factory);
    SolrIndexSearcher searcher = fcontext.qcontext.searcher();
    topLevel = FieldUtil.getSortedDocValues(fcontext.qcontext, searcher.getSchema().getField(field), null);
    nTerms = topLevel.getValueCount();
    if (topLevel instanceof MultiDocValues.MultiSortedDocValues) {
      ordMap = ((MultiDocValues.MultiSortedDocValues)topLevel).mapping;
      subDvs = ((MultiDocValues.MultiSortedDocValues)topLevel).values;
    } else {
      ordMap = null;
      subDvs = null;
    }
  }

  @Override
  protected BytesRef lookupOrd(int ord) {
    return topLevel.lookupOrd(ord);
  }

  @Override
  public void setNextReader(LeafReaderContext readerContext) throws IOException {
    super.setNextReader(readerContext);
    if (subDvs != null) {
      subDv = subDvs[readerContext.ord];
      toGlobal = ordMap.getGlobalOrds(readerContext.ord);
    } else {
      assert readerContext.ord==0 || topLevel.getValueCount() == 0;
      subDv = topLevel;
    }
  }

  @Override
  public void collect(int doc, int slotNum) {
    int segOrd = subDv.getOrd(doc);
    if (segOrd < 0) return;  // -1 means missing
    int ord = toGlobal==null ? segOrd : (int)toGlobal.get(segOrd);

    FixedBitSet bits = arr[slotNum];
    if (bits == null) {
      bits = new FixedBitSet(nTerms);
      arr[slotNum] = bits;
    }
    bits.set(ord);
  }
}