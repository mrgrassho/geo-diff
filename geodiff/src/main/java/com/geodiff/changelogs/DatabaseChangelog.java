package com.geodiff.changelogs;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import org.jongo.Jongo;
import org.jongo.MongoCollection;

@ChangeLog
public class DatabaseChangelog {

    @ChangeSet(order = "001", id = "100000", author = "mrg")
    public void seedFilterOptions(Jongo jongo){

        MongoCollection filterOpt = jongo.getCollection("filterOption");
        filterOpt.insert("{name: 'RAW', longName: 'Raw Image'}");
        filterOpt.insert("{name: 'DROUGHT', longName: 'Drought filtered Image'}");
        filterOpt.insert("{name: 'DEFORESTATION', longName: 'Deforestation filtered Image'}");
        filterOpt.insert("{name: 'URBANIZATION', longName: 'Urbanization filtered Image'}");
        filterOpt.insert("{name: 'FLOODING', longName: 'Flooding filtered Image'}");
    }


}