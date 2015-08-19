/*
 *
 *  * Copyright 2014 Orient Technologies.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  
 */

package com.orientechnologies.lucene.test.geo;

import com.orientechnologies.lucene.test.BaseSpatialLuceneTest;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Enrico Risa on 07/08/15.
 */

@Test(groups = "embedded")
public class LuceneSpatialLineStringTest extends BaseSpatialLuceneTest {
  @Override
  protected String getDatabaseName() {
    return "spatialPolygonTest";
  }

  @BeforeClass
  public void init() {
    initDB();

    databaseDocumentTx.set(ODatabase.ATTRIBUTES.CUSTOM, "strictSql=false");
    OSchema schema = databaseDocumentTx.getMetadata().getSchema();
    OClass v = schema.getClass("V");
    OClass oClass = schema.createClass("Place");
    oClass.setSuperClass(v);
    oClass.createProperty("location", OType.EMBEDDED, schema.getClass("LineString"));
    oClass.createProperty("name", OType.STRING);

    databaseDocumentTx.command(new OCommandSQL("CREATE INDEX Place.location ON Place(location) SPATIAL ENGINE LUCENE")).execute();

  }

  @Test
  public void testLineStringWithoutIndex() {

    databaseDocumentTx.command(new OCommandSQL("drop index Place.location")).execute();
    queryLineString();
  }

  public ODocument createLineString(List<List<Double>> coordinates) {
    ODocument location = new ODocument("LineString");
    location.field("coordinates", coordinates);
    return location;
  }

  @Test
  public void testIndexingLineString() throws IOException {

    ODocument linestring1 = new ODocument("Place");
    linestring1.field("name", "LineString1");
    linestring1.field("location", createLineString(new ArrayList<List<Double>>() {
      {
        add(Arrays.asList(0d, 0d));
        add(Arrays.asList(3d, 3d));
      }
    }));

    ODocument linestring2 = new ODocument("Place");
    linestring2.field("name", "LineString2");
    linestring2.field("location", createLineString(new ArrayList<List<Double>>() {
      {
        add(Arrays.asList(0d, 1d));
        add(Arrays.asList(0d, 5d));
      }
    }));
    databaseDocumentTx.save(linestring1);
    databaseDocumentTx.save(linestring2);

    OIndex<?> index = databaseDocumentTx.getMetadata().getIndexManager().getIndex("Place.location");

    Assert.assertEquals(index.getSize(), 2);
    queryLineString();

  }

  protected void queryLineString() {
    // Should contain Berlin
    String query = "select * from Place where location && { 'shape' : { 'type' : 'LineString' , 'coordinates' : [[1,2],[4,6]]} } ";
    List<ODocument> docs = databaseDocumentTx.query(new OSQLSynchQuery<ODocument>(query));

    Assert.assertEquals(docs.size(), 1);

  }

  @AfterClass
  public void deInit() {
    deInitDB();
  }
}
