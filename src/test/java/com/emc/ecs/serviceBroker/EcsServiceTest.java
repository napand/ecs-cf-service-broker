package com.emc.ecs.serviceBroker;

import static com.emc.ecs.common.Fixtures.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Autowired;

import com.emc.ecs.managementClient.BucketAction;
import com.emc.ecs.managementClient.Connection;
import com.emc.ecs.managementClient.NamespaceAction;
import com.emc.ecs.managementClient.NamespaceQuotaAction;
import com.emc.ecs.managementClient.NamespaceRetentionAction;
import com.emc.ecs.managementClient.ObjectUserAction;
import com.emc.ecs.managementClient.ObjectUserSecretAction;
import com.emc.ecs.managementClient.ReplicationGroupAction;
import com.emc.ecs.managementClient.model.DataServiceReplicationGroup;
import com.emc.ecs.managementClient.model.NamespaceCreate;
import com.emc.ecs.managementClient.model.NamespaceUpdate;
import com.emc.ecs.managementClient.model.RetentionClassCreate;
import com.emc.ecs.managementClient.model.RetentionClassUpdate;
import com.emc.ecs.managementClient.model.UserSecretKey;
import com.emc.ecs.serviceBroker.config.BrokerConfig;
import com.emc.ecs.serviceBroker.config.CatalogConfig;
import com.emc.ecs.serviceBroker.model.NamespaceQuotaParam;

@RunWith(PowerMockRunner.class)
public class EcsServiceTest {

    @Mock
    private Connection connection;

    @Mock
    private BrokerConfig broker;

    @Mock
    private CatalogConfig catalog;

    @Autowired
    @InjectMocks
    EcsService ecs;

    /**
     * When initializing the ecs-service, and object-endpoint, repo-user &
     * repo-bucket are set, the service will use these static settings. It the
     * repo facilities exist, the ecs-service will continue.
     * 
     * @throws EcsManagementClientException
     * @throws EcsManagementResourceNotFoundException
     */
    @PrepareForTest({ ReplicationGroupAction.class, BucketAction.class,
	    ObjectUserAction.class, ObjectUserSecretAction.class })
    @Test
    public void initializeStaticConfigTest()
	    throws EcsManagementClientException,
	    EcsManagementResourceNotFoundException {
	PowerMockito.mockStatic(ReplicationGroupAction.class);

	when(broker.getObjectEndpoint()).thenReturn(OBJ_ENDPOINT);
	when(broker.getPrefix()).thenReturn(PREFIX);
	when(broker.getReplicationGroup()).thenReturn(RG_NAME);
	when(broker.getNamespace()).thenReturn(NAMESPACE);
	when(broker.getRepositoryUser()).thenReturn("user");
	when(broker.getRepositoryBucket()).thenReturn("repository");

	DataServiceReplicationGroup rg = new DataServiceReplicationGroup();
	rg.setName(RG_NAME);
	rg.setId(RG_ID);
	PowerMockito.mockStatic(BucketAction.class);
	when(BucketAction.exists(connection, REPO_BUCKET, NAMESPACE))
		.thenReturn(true);

	PowerMockito.mockStatic(ReplicationGroupAction.class);
	when(ReplicationGroupAction.list(connection))
		.thenReturn(Arrays.asList(rg));

	PowerMockito.mockStatic(ObjectUserAction.class);
	when(ObjectUserAction.exists(connection, REPO_USER, NAMESPACE))
		.thenReturn(true);

	UserSecretKey secretKey = new UserSecretKey();
	secretKey.setSecretKey(TEST);
	PowerMockito.mockStatic(ObjectUserSecretAction.class);
	when(ObjectUserSecretAction.list(connection, REPO_USER))
		.thenReturn(Arrays.asList(secretKey));

	ecs.initialize();

	assertEquals(OBJ_ENDPOINT, ecs.getObjectEndpoint());
	assertEquals(PREFIX + "test", ecs.prefix(TEST));
	verify(broker, times(1)).setRepositoryEndpoint(OBJ_ENDPOINT);
	verify(broker, times(1)).setRepositorySecret(TEST);
    }

    /**
     * When initializing the ecs-service, if the object-endpoint is not set
     * statically, but base-url is, the service will look up the endpoint from the
     * base-url.
     */
    @Test
    public void initializeBaseUrlLookup() {
    }
    
    /**
     * When initializing the ecs-service, if neither the object-endpoint is not
     * set statically nor the base-url, the service will lookup an endpoint
     * named default in the base-url list. If one is found, it will set this as
     * the repo endpoint.
     */
    @Test
    public void initializeBaseUrlDefaultLookup() {
	
    }
    
    /**
     * When initializing the ecs-service, if neither the object-endpoint is not
     * set statically nor the base-url, the service will lookup an endpoint
     * named default in the base-url list. If none is found, it will throw an
     * exception.
     */
    @Test
    public void initializeBaseUrlDefaultLookupFails() {
	
    }

    /**
     * When initializing the ecs-service, if the repo-bucket and user do not exist,
     * the service will create these.
     */
    @Test
    public void initializeCreateRepoBucketAndUser() {
	
    }

    /**
     * When creating a new namespace the settings in the plan will carry
     * through to the created service.  Any settings not implemented in
     * the service, the plan or the parameters will be kept as null.
     * 
     * @throws Exception
     */
    @PrepareForTest({ NamespaceAction.class, NamespaceQuotaAction.class })
    @Test
    public void createNamespaceDefaultTest() throws Exception {
	PowerMockito.mockStatic(NamespaceAction.class);
	PowerMockito.doNothing().when(NamespaceAction.class, "create",
		same(connection), any(NamespaceCreate.class));
	PowerMockito.mockStatic(NamespaceQuotaAction.class);
	PowerMockito.doNothing().when(NamespaceQuotaAction.class, "create",
		same(connection), anyString(), any(NamespaceQuotaParam.class));

	when(broker.getPrefix()).thenReturn(PREFIX);
	when(catalog.findServiceDefinition(SERVICE_ID))
		.thenReturn(namespaceServiceFixture());

	Map<String, Object> params = new HashMap<>();
	ecs.createNamespace(NAMESPACE, SERVICE_ID, PLAN_ID1, params);

	Mockito.verify(catalog, times(1)).findServiceDefinition(SERVICE_ID);

	PowerMockito.verifyStatic();

	ArgumentCaptor<NamespaceCreate> createCaptor =
		    ArgumentCaptor.forClass(NamespaceCreate.class);
	NamespaceAction.create(same(connection), createCaptor.capture());
	NamespaceCreate create = createCaptor.getValue();
	assertEquals(PREFIX + NAMESPACE, create.getNamespace());
	assertNull(create.getIsEncryptionEnabled());
	assertNull(create.getIsComplianceEnabled());
	assertNull(create.getIsStaleAllowed());
	assertEquals(Integer.valueOf(5), create.getDefaultBucketBlockSize());

	PowerMockito.verifyStatic();

	ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
	ArgumentCaptor<NamespaceQuotaParam> quotaParamCaptor = ArgumentCaptor
		.forClass(NamespaceQuotaParam.class);
	NamespaceQuotaAction.create(same(connection), idCaptor.capture(),
		quotaParamCaptor.capture());
	assertEquals(PREFIX + NAMESPACE, idCaptor.getValue());
	assertEquals(5, quotaParamCaptor.getValue().getBlockSize());
	assertEquals(4, quotaParamCaptor.getValue().getNotificationSize());
    }

    /**
     * When changing plans the params of the new plan should be set, on the
     * existing namespace. The resulting update action will have the settings of
     * the new plan.
     * 
     * @throws Exception
     */
    @PrepareForTest({ NamespaceAction.class })
    @Test
    public void changeNamespacePlanTest() throws Exception {
	PowerMockito.mockStatic(NamespaceAction.class);
	PowerMockito.doNothing().when(NamespaceAction.class, "update",
		same(connection), anyString(),
		any(NamespaceUpdate.class));

	ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
	ArgumentCaptor<NamespaceUpdate> updateCaptor = ArgumentCaptor
		.forClass(NamespaceUpdate.class);

	when(broker.getPrefix()).thenReturn(PREFIX);
	when(catalog.findServiceDefinition(SERVICE_ID))
		.thenReturn(namespaceServiceFixture());

	Map<String, Object> params = new HashMap<>();
	ecs.changeNamespacePlan(NAMESPACE, SERVICE_ID, PLAN_ID2, params);

	Mockito.verify(catalog, times(1)).findServiceDefinition(SERVICE_ID);
	PowerMockito.verifyStatic();
	NamespaceAction.update(same(connection), idCaptor.capture(),
		updateCaptor.capture());
	assertEquals(PREFIX + NAMESPACE, idCaptor.getValue());
	NamespaceUpdate update = updateCaptor.getValue();
	assertEquals(EXTERNAL_ADMIN, update.getExternalGroupAdmins());
	assertTrue(update.getIsEncryptionEnabled());
	assertTrue(update.getIsComplianceEnabled());
	assertTrue(update.getIsStaleAllowed());
    }

    /**
     * When creating plan with user specified parameters, the params should be
     * set, except when overridden by a plan or service setting. Therefore,
     * default-bucket-quota will not be "10", it will be "5" since that's the
     * setting in the plan. Other settings will carry through.
     * 
     * @throws Exception
     */
    @PrepareForTest({ NamespaceAction.class, NamespaceQuotaAction.class })
    @Test
    public void createNamespaceWithParamsTest() throws Exception {
	Map<String, Object> params = new HashMap<>();
	params.put("domain-group-admins", EXTERNAL_ADMIN);
	params.put("encrypted", true);
	params.put("compliance-enabled", true);
	params.put("access-during-outage", true);
	params.put("default-bucket-quota", 10);

	PowerMockito.mockStatic(NamespaceAction.class);
	PowerMockito.doNothing().when(NamespaceAction.class, "create",
		same(connection), any(NamespaceUpdate.class));
	ArgumentCaptor<NamespaceCreate> createCaptor = ArgumentCaptor
		.forClass(NamespaceCreate.class);
	
	PowerMockito.mockStatic(NamespaceQuotaAction.class);
	PowerMockito.doNothing().when(NamespaceQuotaAction.class, "create",
		same(connection), anyString(),
		any(NamespaceQuotaParam.class));

	when(broker.getPrefix()).thenReturn(PREFIX);
	when(catalog.findServiceDefinition(SERVICE_ID))
		.thenReturn(namespaceServiceFixture());

	ecs.createNamespace(NAMESPACE, SERVICE_ID, PLAN_ID1, params);

	Mockito.verify(catalog, times(1)).findServiceDefinition(SERVICE_ID);
	PowerMockito.verifyStatic();
	NamespaceAction.create(same(connection), createCaptor.capture());
	NamespaceCreate create = createCaptor.getValue();
	assertEquals(PREFIX + NAMESPACE, create.getNamespace());
	assertEquals(EXTERNAL_ADMIN, create.getExternalGroupAdmins());
	assertTrue(create.getIsEncryptionEnabled());
	assertTrue(create.getIsComplianceEnabled());
	assertTrue(create.getIsStaleAllowed());
	assertEquals(Integer.valueOf(5), create.getDefaultBucketBlockSize());
	
	PowerMockito.verifyStatic();
	ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
	ArgumentCaptor<NamespaceQuotaParam> quotaParamCaptor = ArgumentCaptor
		.forClass(NamespaceQuotaParam.class);
	NamespaceQuotaAction.create(same(connection), idCaptor.capture(),
		quotaParamCaptor.capture());
	assertEquals(PREFIX + NAMESPACE, idCaptor.getValue());
	assertEquals(5, quotaParamCaptor.getValue().getBlockSize());
	assertEquals(4, quotaParamCaptor.getValue().getNotificationSize());
    }

    /**
     * When changing a namespace plan that does not include a default quota, and
     * a quota is specified in the parameters with the value of -1, then the
     * quota should be removed.
     */
    
    /**
     * When changing a namespace plan that includes a default quota, and a quota
     * is specified in the parameters, then the quota change should be ignored.
     */
    
    /**
     * When changing namespace plan with user specified parameters, the params
     * should be set, except when overridden by a plan or service setting.
     * Therefore, default-bucket-quota will not be "10", it will be "5" since
     * that's the setting in the plan. Other settings will carry through.
     * 
     * @throws Exception
     */
    @PrepareForTest({ NamespaceAction.class })
    @Test
    public void changeNamespacePlanWithParamsTest() throws Exception {
	Map<String, Object> params = new HashMap<>();
	params.put("domain-group-admins", EXTERNAL_ADMIN);
	params.put("encrypted", true);
	params.put("compliance-enabled", true);
	params.put("access-during-outage", true);
	params.put("default-bucket-quota", 10);

	PowerMockito.mockStatic(NamespaceAction.class);
	PowerMockito.doNothing().when(NamespaceAction.class, "update",
		same(connection), anyString(),
		any(NamespaceUpdate.class));
	ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
	ArgumentCaptor<NamespaceUpdate> updateCaptor = ArgumentCaptor
		.forClass(NamespaceUpdate.class);

	when(broker.getPrefix()).thenReturn(PREFIX);
	when(catalog.findServiceDefinition(SERVICE_ID))
		.thenReturn(namespaceServiceFixture());

	ecs.changeNamespacePlan(NAMESPACE, SERVICE_ID, PLAN_ID1, params);

	Mockito.verify(catalog, times(1)).findServiceDefinition(SERVICE_ID);
	PowerMockito.verifyStatic();
	NamespaceAction.update(same(connection), idCaptor.capture(),
		updateCaptor.capture());
	NamespaceUpdate update = updateCaptor.getValue();
	assertEquals(PREFIX + NAMESPACE, idCaptor.getValue());
	assertEquals(EXTERNAL_ADMIN, update.getExternalGroupAdmins());
	assertTrue(update.getIsEncryptionEnabled());
	assertTrue(update.getIsComplianceEnabled());
	assertTrue(update.getIsStaleAllowed());
	assertEquals(Integer.valueOf(5), update.getDefaultBucketBlockSize());
    }

    /**
     * When creating a namespace with retention-class defined in the plan and
     * the retention-class does not already exist, the retention class should be
     * created.
     * 
     * @throws Exception
     */
    @PrepareForTest({ NamespaceAction.class, NamespaceRetentionAction.class} )
    @Test
    public void createNamespaceWithRetention() throws Exception {
	Map<String, Object> params = new HashMap<>();

	PowerMockito.mockStatic(NamespaceAction.class);
	PowerMockito.doNothing().when(NamespaceAction.class, "update",
		same(connection), anyString(),
		any(NamespaceCreate.class));
	PowerMockito.mockStatic(NamespaceRetentionAction.class);
	PowerMockito.doNothing().when(NamespaceRetentionAction.class, "create",
		same(connection), anyString(),
		any(RetentionClassCreate.class));

	when(broker.getPrefix()).thenReturn(PREFIX);
	when(catalog.findServiceDefinition(SERVICE_ID))
		.thenReturn(namespaceServiceFixture());

	ecs.createNamespace(NAMESPACE, SERVICE_ID, PLAN_ID3, params);

	Mockito.verify(catalog, times(1)).findServiceDefinition(SERVICE_ID);
	PowerMockito.verifyStatic();
	ArgumentCaptor<NamespaceCreate> createCaptor = ArgumentCaptor
		.forClass(NamespaceCreate.class);
	NamespaceAction.create(same(connection), createCaptor.capture());
	NamespaceCreate create = createCaptor.getValue();
	assertEquals(PREFIX + NAMESPACE, create.getNamespace());
	assertTrue(create.getIsEncryptionEnabled());
	assertTrue(create.getIsStaleAllowed());
	assertTrue(create.getIsComplianceEnabled());

	PowerMockito.verifyStatic();
	ArgumentCaptor<RetentionClassCreate> retentionCreateCaptor = ArgumentCaptor
		.forClass(RetentionClassCreate.class);
	ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
	NamespaceRetentionAction.create(same(connection), idCaptor .capture(),
		retentionCreateCaptor.capture());
	RetentionClassCreate retention = retentionCreateCaptor.getValue();
	assertEquals(PREFIX + NAMESPACE, idCaptor.getValue());
	assertEquals("one-year", retention.getName());
	assertEquals(31536000, retention.getPeriod());
    }

    /**
     * When changing a namespace plan and a different retention-class is
     * specified in the parameters, then the retention class should be added.
     * 
     * @throws Exception
     */
    @PrepareForTest({ NamespaceAction.class, NamespaceRetentionAction.class })
    @Test
    public void changeNamespacePlanNewRentention() throws Exception {
	Map<String, Object> retention = new HashMap<>();
	retention.put("thirty-days", 2592000);
	Map<String, Object> params = new HashMap<>();
	params.put("retention", retention);

	PowerMockito.mockStatic(NamespaceAction.class);
	PowerMockito.doNothing().when(NamespaceAction.class, "update",
		same(connection), anyString(), any(NamespaceUpdate.class));

	PowerMockito.mockStatic(NamespaceRetentionAction.class);
	PowerMockito.when(NamespaceRetentionAction.class, "exists",
		same(connection), anyString(),
		any(RetentionClassUpdate.class)).thenReturn(false);
	PowerMockito.doNothing().when(NamespaceRetentionAction.class, "create",
		same(connection), anyString(),
		any(RetentionClassCreate.class));
	ArgumentCaptor<String> nsCaptor = ArgumentCaptor.forClass(String.class);
	ArgumentCaptor<RetentionClassCreate> createCaptor = ArgumentCaptor
		.forClass(RetentionClassCreate.class);

	when(broker.getPrefix()).thenReturn(PREFIX);
	when(catalog.findServiceDefinition(SERVICE_ID))
		.thenReturn(namespaceServiceFixture());

	ecs.changeNamespacePlan(NAMESPACE, SERVICE_ID, PLAN_ID2, params);

	Mockito.verify(catalog, times(1)).findServiceDefinition(SERVICE_ID);
	PowerMockito.verifyStatic();
	NamespaceRetentionAction.create(same(connection), nsCaptor.capture(),
		createCaptor.capture());
	assertEquals(PREFIX + NAMESPACE, nsCaptor.getValue());
	assertEquals("thirty-days", createCaptor.getValue().getName());
	assertEquals(2592000, createCaptor.getValue().getPeriod());
    }
    
    /**
     * When changing a namespace plan and a retention-class is specified in the
     * parameters with the value -1, then the retention class should be
     * removed.
     * 
     * @throws Exception
     */
    @PrepareForTest({ NamespaceAction.class, NamespaceRetentionAction.class })
    @Test
    public void changeNamespacePlanRemoveRentention() throws Exception {
	Map<String, Object> retention = new HashMap<>();
	retention.put("thirty-days", -1);
	Map<String, Object> params = new HashMap<>();
	params.put("retention", retention);

	PowerMockito.mockStatic(NamespaceAction.class);
	PowerMockito.doNothing().when(NamespaceAction.class, "update",
		same(connection), anyString(), any(NamespaceUpdate.class));

	PowerMockito.mockStatic(NamespaceRetentionAction.class);
	PowerMockito.when(NamespaceRetentionAction.class, "exists",
		same(connection), anyString(),
		any(RetentionClassUpdate.class)).thenReturn(true);
	PowerMockito.doNothing().when(NamespaceRetentionAction.class, "delete",
		same(connection), anyString(), anyString());
	ArgumentCaptor<String> nsCaptor = ArgumentCaptor.forClass(String.class);
	ArgumentCaptor<String> rcCaptor = ArgumentCaptor.forClass(String.class);

	when(broker.getPrefix()).thenReturn(PREFIX);
	when(catalog.findServiceDefinition(SERVICE_ID))
		.thenReturn(namespaceServiceFixture());

	ecs.changeNamespacePlan(NAMESPACE, SERVICE_ID, PLAN_ID2, params);

	Mockito.verify(catalog, times(1)).findServiceDefinition(SERVICE_ID);
	PowerMockito.verifyStatic();
	NamespaceRetentionAction.delete(same(connection), nsCaptor.capture(),
		rcCaptor.capture());
	assertEquals(PREFIX + NAMESPACE, nsCaptor.getValue());
	assertEquals("thirty-days", rcCaptor.getValue());
    }
    
    /**
     * When changing a namespace plan and a retention-class is specified in the
     * parameters with a different value than the existing value, then the
     * retention-class should be changed.
     * 
     * @throws Exception
     */
    @PrepareForTest({ NamespaceAction.class, NamespaceRetentionAction.class })
    @Test
    public void changeNamespacePlanChangeRentention() throws Exception {
	Map<String, Object> retention = new HashMap<>();
	retention.put("thirty-days", 2592000);
	Map<String, Object> params = new HashMap<>();
	params.put("retention", retention);

	PowerMockito.mockStatic(NamespaceAction.class);
	PowerMockito.doNothing().when(NamespaceAction.class, "update",
		same(connection), anyString(), any(NamespaceUpdate.class));

	PowerMockito.mockStatic(NamespaceRetentionAction.class);
	PowerMockito.when(NamespaceRetentionAction.class, "exists",
		same(connection), anyString(),
		any(RetentionClassUpdate.class)).thenReturn(true);
	PowerMockito.doNothing().when(NamespaceRetentionAction.class, "update",
		same(connection), anyString(), anyString(),
		any(RetentionClassUpdate.class));
	ArgumentCaptor<String> nsCaptor = ArgumentCaptor.forClass(String.class);
	ArgumentCaptor<String> rcCaptor = ArgumentCaptor.forClass(String.class);
	ArgumentCaptor<RetentionClassUpdate> updateCaptor = ArgumentCaptor
		.forClass(RetentionClassUpdate.class);

	when(broker.getPrefix()).thenReturn(PREFIX);
	when(catalog.findServiceDefinition(SERVICE_ID))
		.thenReturn(namespaceServiceFixture());

	ecs.changeNamespacePlan(NAMESPACE, SERVICE_ID, PLAN_ID2, params);

	Mockito.verify(catalog, times(1)).findServiceDefinition(SERVICE_ID);
	PowerMockito.verifyStatic();
	NamespaceRetentionAction.update(same(connection), nsCaptor.capture(),
		rcCaptor.capture(), updateCaptor.capture());
	assertEquals(PREFIX + NAMESPACE, nsCaptor.getValue());
	assertEquals("thirty-days", rcCaptor.getValue());
	assertEquals(2592000, updateCaptor.getValue().getPeriod());
    }
}
