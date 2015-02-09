package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Cluster;
import org.ovirt.engine.api.model.Clusters;
import org.ovirt.engine.api.model.Network;
import org.ovirt.engine.api.resource.ClusterResource;
import org.ovirt.engine.api.resource.ClustersResource;
import org.ovirt.engine.api.restapi.utils.GuidUtils;
import org.ovirt.engine.core.common.action.AddClusterOperationParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdsGroupParametersBase;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.common.queries.IdAndNameQueryParameters;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

import static org.ovirt.engine.api.restapi.resource.BackendDataCenterResource.getStoragePool;

public class BackendClustersResource extends AbstractBackendCollectionResource<Cluster, VDSGroup>
        implements ClustersResource {

    static final String[] SUB_COLLECTIONS = { "networks", "permissions", "glustervolumes", "glusterhooks",
            "affinitygroups", "cpuprofiles" };
    static final String[] VIRT_ONLY_MODE_COLLECTIONS_TO_HIDE = {"glustervolumes", "glusterhooks" };
    public BackendClustersResource() {
        super(Cluster.class, VDSGroup.class, SUB_COLLECTIONS);
    }

    @Override
    public Clusters list() {
        ApplicationMode appMode = getCurrent().get(ApplicationMode.class);

        if (appMode == ApplicationMode.VirtOnly) {
            return listVirtOnly();
        }
        else {
            return listAll();
        }
    }

    private Clusters listVirtOnly() {
        if (isFiltered()) {
            return mapVirtOnlyCollection(getBackendCollection(VdcQueryType.GetAllVdsGroups,
                    new VdcQueryParametersBase()));
        }
        else {
            return mapVirtOnlyCollection(getBackendCollection(SearchType.Cluster));
        }
    }

    private Clusters listAll() {
        if (isFiltered()) {
            return mapCollection(getBackendCollection(VdcQueryType.GetAllVdsGroups,
                    new VdcQueryParametersBase()));
        }
        else {
            return mapCollection(getBackendCollection(SearchType.Cluster));
        }
    }

    @Override
    @SingleEntityResource
    public ClusterResource getClusterSubResource(String id) {
        return inject(new BackendClusterResource(id));
    }

    @Override
    public Response add(Cluster cluster) {
        validateParameters(cluster, getMandatoryParameters());
        validateEnums(Cluster.class, cluster);
        StoragePool dataCenter = getDataCenter(cluster);
        return performCreate(VdcActionType.AddVdsGroup,
                createAddCommandParams(cluster, dataCenter),
                new QueryIdResolver<Guid>(VdcQueryType.GetVdsGroupById, IdQueryParameters.class));
    }

    protected String[] getMandatoryParameters() {
        return new String[] { "name", "dataCenter.name|id" };
    }

    protected StoragePool getDataCenter(Cluster cluster) {
        return getStoragePool(cluster.getDataCenter(), this);
    }

    private Guid getManagementNetworkId(Cluster cluster, Guid dataCenterId) {
        Guid managementNetworkId = null;
        if (cluster.isSetManagementNetwork()) {
            validateParameters(cluster.getManagementNetwork(), "id|name");
            final Network rawManagementNetwork = cluster.getManagementNetwork();
            if (rawManagementNetwork.isSetId()) {
                managementNetworkId = GuidUtils.asGuid(rawManagementNetwork.getId());
            } else {
                final org.ovirt.engine.core.common.businessentities.network.Network managementNetwork =
                        getEntity(org.ovirt.engine.core.common.businessentities.network.Network.class,
                                VdcQueryType.GetNetworkByNameAndDataCenter,
                                new IdAndNameQueryParameters(dataCenterId, rawManagementNetwork.getName()),
                                String.format("Network: %s", rawManagementNetwork.getName()));

                managementNetworkId = managementNetwork.getId();
            }
        }
        return managementNetworkId;
    }

    protected AddClusterOperationParameters createAddCommandParams(Cluster cluster, StoragePool dataCenter) {
        VDSGroup clusterEntity = map(cluster, map(dataCenter));

        if (!(cluster.isSetErrorHandling() && cluster.getErrorHandling().isSetOnError())) {
            clusterEntity.setMigrateOnError(null);
        }

        final Guid managementNetworkId = getManagementNetworkId(cluster, dataCenter.getId());

        return new AddClusterOperationParameters(clusterEntity, managementNetworkId);
    }

    @Override
    public Response performRemove(String id) {
        return performAction(VdcActionType.RemoveVdsGroup, new VdsGroupParametersBase(asGuid(id)));
    }

    protected Clusters mapCollection(List<VDSGroup> entities) {
        Clusters collection = new Clusters();
        for (org.ovirt.engine.core.common.businessentities.VDSGroup entity : entities) {
            collection.getClusters().add(addLinks(map(entity)));
        }
        return collection;
    }

    private Clusters mapVirtOnlyCollection(List<VDSGroup> entities) {
        Clusters collection = new Clusters();
        for (org.ovirt.engine.core.common.businessentities.VDSGroup entity : entities) {
            collection.getClusters().add(addLinks(map(entity), VIRT_ONLY_MODE_COLLECTIONS_TO_HIDE));
        }
        return collection;
    }

    /**
     * Map the storage pool (i.e. datacenter entity) to a VDSGroup instance
     * with the same compatibility version
     */
    protected VDSGroup map(StoragePool pool) {
        return getMapper(StoragePool.class, VDSGroup.class).map(pool, null);
    }

    @Override
    protected Cluster doPopulate(Cluster model, VDSGroup entity) {
        return model;
    }
}
