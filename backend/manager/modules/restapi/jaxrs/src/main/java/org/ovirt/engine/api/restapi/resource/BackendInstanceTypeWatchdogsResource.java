/*
Copyright (c) 2015 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.ovirt.engine.api.restapi.resource;

import java.util.List;
import java.util.Objects;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.InstanceType;
import org.ovirt.engine.api.model.Watchdog;
import org.ovirt.engine.api.model.Watchdogs;
import org.ovirt.engine.api.resource.InstanceTypeWatchdogResource;
import org.ovirt.engine.api.resource.InstanceTypeWatchdogsResource;
import org.ovirt.engine.api.restapi.types.WatchdogMapper;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.core.common.action.WatchdogParameters;
import org.ovirt.engine.core.common.businessentities.VmWatchdog;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.QueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendInstanceTypeWatchdogsResource
        extends AbstractBackendCollectionResource<Watchdog, VmWatchdog>
        implements InstanceTypeWatchdogsResource {

    private Guid instanteTypeId;

    public BackendInstanceTypeWatchdogsResource(Guid instanceTypeId) {
        super(Watchdog.class, VmWatchdog.class);
        this.instanteTypeId = instanceTypeId;
    }

    @Override
    public Watchdogs list() {
        return mapCollection(getBackendCollection(QueryType.GetWatchdog, new IdQueryParameters(instanteTypeId)));
    }

    private Watchdogs mapCollection(List<VmWatchdog> entities) {
        Watchdogs collection = new Watchdogs();
        for (VmWatchdog entity : entities) {
            collection.getWatchdogs().add(addLinks(map(entity)));
        }
        return collection;
    }

    public Response add(Watchdog watchdog) {
        validateParameters(watchdog, "action", "model");
        WatchdogParameters parameters = new WatchdogParameters();
        parameters.setClusterIndependent(true);
        if (watchdog.isSetAction()) {
            parameters.setAction(WatchdogMapper.map(watchdog.getAction()));
        }
        if (watchdog.isSetModel()) {
            parameters.setModel(WatchdogMapper.map(watchdog.getModel()));
        }
        parameters.setId(instanteTypeId);
        parameters.setVm(false);
        return performCreate(ActionType.AddWatchdog, parameters, new WatchdogResolver());
    }

    @Override
    public InstanceTypeWatchdogResource getWatchdogResource(String watchdogId) {
        return inject(new BackendInstanceTypeWatchdogResource(watchdogId, instanteTypeId));
    }

    @Override
    public Watchdog addParents(Watchdog watchdog) {
        InstanceType instanceType = new InstanceType();
        instanceType.setId(instanteTypeId.toString());
        watchdog.setInstanceType(instanceType);
        return watchdog;
    }

    private class WatchdogResolver implements IResolver<Guid, VmWatchdog> {
        @Override
        public VmWatchdog resolve(Guid id) throws BackendFailureException {
            List<VmWatchdog> watchdogs = getBackendCollection(QueryType.GetWatchdog, new IdQueryParameters(instanteTypeId));
            for (VmWatchdog watchdog : watchdogs) {
                if (Objects.equals(watchdog.getId(), id)) {
                    return watchdog;
                }
            }
            return null;
        }
    }
}
